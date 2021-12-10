package com.example.snakegame.net;

import com.example.snakegame.*;
import com.example.snakegame.ui.GameController;
import me.ippolitov.fit.snakes.SnakesProto;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class MasterNode extends AbstractNode {
    private Model model;
    private NodeInfo deputy;
    private MoveDirection myDirection;

    public MasterNode(GameController gameController, String name, SnakesProto.GameConfig config) throws IOException, SnakeException {
        super();
        this.config = config;
        model = new Model(config.getWidth(), config.getHeight(), config.getFoodStatic(), config.getFoodPerPlayer());
        role = NodeRole.MASTER;
        id = idPool.getId();
        model.addPlayer(id);
        this.gameController = gameController;
        this.name = name;
    }

    public MasterNode(AbstractNode node, SnakesProto.GameState gameState) throws IOException {
        super();
        role = NodeRole.MASTER;
        this.config = node.config;
        id = node.id;
        this.name = node.name;
        stateOrder = gameState.getStateOrder();
        gameController = node.gameController;
        List<Snake> snakes = new ArrayList<>();
        for (SnakesProto.GameState.Snake snake : gameState.getSnakesList()) {
            Snake gameSnake = new Snake(snake.getPlayerId(), toGameDirection(snake.getHeadDirection()));
            if (snake.getState() == SnakesProto.GameState.Snake.SnakeState.ALIVE) {
                gameSnake.setAlive(true);
                for (SnakesProto.GameState.Coord coord : snake.getPointsList()) {
                    gameSnake.getCoordList().add(toGameCoord(coord));
                }
            } else {
                gameSnake.setAlive(false);
            }
            snakes.add(gameSnake);
        }
        List<Coord> foodCoord = gameState.getFoodsList().stream().map(this::toGameCoord).collect(Collectors.toList());
        model = new Model(config.getWidth(), config.getHeight(), snakes, foodCoord, config.getFoodStatic(), config.getFoodPerPlayer());
        for (SnakesProto.GamePlayer player : gameState.getPlayers().getPlayersList()) {
            if (player.getId() == id) continue;
            if (player.getRole() == SnakesProto.NodeRole.MASTER) continue;
            InetSocketAddress socketAddress = new InetSocketAddress(player.getIpAddress(), player.getPort());
            NodeInfo nodeInfo = new NodeInfo(player.getName(), player.getId(), socketAddress.getAddress(), player.getPort(), getNodeRole(player.getRole()));
            nodes.put(socketAddress, nodeInfo);
            idPool.getId(player.getId());
        }
        seq.set(node.seq.longValue());
        gameController.setNode(this);
        sendNewMasterMessage();
    }

    protected void sendNewMasterMessage() throws IOException {
        for (NodeInfo node : nodes.values()) {
            SnakesProto.GameMessage.RoleChangeMsg roleChangeMsg = SnakesProto.GameMessage.RoleChangeMsg.newBuilder().
                    setReceiverRole(SnakesProto.NodeRole.NORMAL).
                    setSenderRole(SnakesProto.NodeRole.MASTER).
                    build();
            System.out.println(node.getAddress());
            System.out.println(node.getPort());
            SnakesProto.GameMessage gameMessage = SnakesProto.GameMessage.newBuilder().setRoleChange(roleChangeMsg).setMsgSeq(seq.getAndIncrement()).setSenderId(id).setReceiverId(id).build();
            DatagramPacket packet = new DatagramPacket(gameMessage.toByteArray(), gameMessage.toByteArray().length, node.getAddress(), node.getPort());
            AcknowledgeWait acknowledgeWait = new AcknowledgeWait(System.currentTimeMillis(), packet, gameMessage);
            node.getAcknowledgeWaits().add(acknowledgeWait);
            socket.send(packet);
        }
    }

    protected void findNewDeputy() {
        for (NodeInfo node : nodes.values()) {
            deputy = node;
            SnakesProto.GameMessage.RoleChangeMsg roleChangeMsg = SnakesProto.GameMessage.RoleChangeMsg.newBuilder().setSenderRole(SnakesProto.NodeRole.MASTER).setReceiverRole(SnakesProto.NodeRole.DEPUTY).build();
            SnakesProto.GameMessage gameMessage = SnakesProto.GameMessage.newBuilder().setRoleChange(roleChangeMsg).setSenderId(id).setReceiverId(node.getId()).setMsgSeq(seq.getAndIncrement()).build();
            DatagramPacket packet = new DatagramPacket(gameMessage.toByteArray(), gameMessage.toByteArray().length, node.getAddress(), node.getPort());
            AcknowledgeWait acknowledgeWait = new AcknowledgeWait(System.currentTimeMillis(), packet, gameMessage);
            node.getAcknowledgeWaits().add(acknowledgeWait);
            try {
                socket.send(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
            break;
        }
    }

    protected void startAcknowledgeService() {
        service.scheduleAtFixedRate(() -> {
            try {

                System.out.println(deputy);
                if (deputy == null) {
                    findNewDeputy();
                }
                for (NodeInfo node : nodes.values()) {
                    if ((System.currentTimeMillis() - node.getLastSeenOnline() > config.getNodeTimeoutMs())) {
                        if (node.getNodeRole() == NodeRole.DEPUTY) {
                            nodes.remove(node.getAddress());
                            findNewDeputy();
                        } else {
                            nodes.remove(node.getAddress());
                            model.getSnake(node.getId()).setAlive(false);
                        }
                    }
                    if (System.currentTimeMillis() - node.getLastSeenOnline() > config.getPingDelayMs()) {
                        SnakesProto.GameMessage.PingMsg pingMsg = SnakesProto.GameMessage.PingMsg.newBuilder().build();
                        SnakesProto.GameMessage gameMessage = SnakesProto.GameMessage.newBuilder().setPing(pingMsg).setMsgSeq(seq.getAndIncrement()).build();
                        DatagramPacket packet = new DatagramPacket(gameMessage.toByteArray(), gameMessage.toByteArray().length, node.getAddress(), node.getPort());
                        AcknowledgeWait acknowledgeWait = new AcknowledgeWait(System.currentTimeMillis(), packet, gameMessage);
                        node.getAcknowledgeWaits().add(acknowledgeWait);
                        try {
                            socket.send(packet);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    AcknowledgeWait acknowledgeWait = node.getAcknowledgeWaits().peek();
                    if (acknowledgeWait != null) {
                        if (System.currentTimeMillis() - acknowledgeWait.getTime() > config.getPingDelayMs()) {
                            try {
                                socket.send(acknowledgeWait.getPacket());
                                acknowledgeWait.setTime(System.currentTimeMillis());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    } else {
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, config.getPingDelayMs(), config.getPingDelayMs(), TimeUnit.MILLISECONDS);
    }

    @Override
    public void setTurn(MoveDirection direction) {
        model.setTurn(id, direction);
    }

    public void run() {
        startAcknowledgeService();
        service.scheduleAtFixedRate(() -> {
            SnakesProto.GameMessage.AnnouncementMsg announcementMsg = SnakesProto.GameMessage.AnnouncementMsg.newBuilder().setConfig(config).setPlayers(SnakesProto.GamePlayers.newBuilder().addAllPlayers(getProtoPlayer())).build();
            SnakesProto.GameMessage gameMessage = SnakesProto.GameMessage.newBuilder().setAnnouncement(announcementMsg).setMsgSeq(seq.getAndIncrement()).build();
            DatagramPacket packet = new DatagramPacket(gameMessage.toByteArray(), gameMessage.toByteArray().length, multicastAddress);
            try {
                socket.send(packet);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }, 0, 1, TimeUnit.SECONDS);
        service.scheduleAtFixedRate(() -> {
            try {
                try {
                    model.makeTurns();
                    for (NodeInfo node : nodes.values()) {
                        node.setScore(model.getSnake(node.getId()).getScore());
                    }
                    score = model.getSnake(id).getScore();
                    List<SnakesProto.GameState.Snake> snakes = toProtoSnake(model.getSnakes());
                    List<SnakesProto.GameState.Coord> foodCoords = toProtoCoords(model.getFoodCells().stream().map(Cell::getCoord).collect(Collectors.toList()));
                    List<SnakesProto.GamePlayer> players = getProtoPlayer();
                    SnakesProto.GamePlayers gamePlayers = SnakesProto.GamePlayers.newBuilder().addAllPlayers(players).build();
                    SnakesProto.GameState gameState = SnakesProto.GameState.newBuilder().setStateOrder(stateOrder++).addAllSnakes(snakes).addAllFoods(foodCoords).setPlayers(gamePlayers).setConfig(config).build();
                    SnakesProto.GameMessage.StateMsg stateMsg = SnakesProto.GameMessage.StateMsg.newBuilder().setState(gameState).build();
                    for (NodeInfo node : nodes.values()) {
                        SnakesProto.GameMessage gameMessage = SnakesProto.GameMessage.newBuilder().setState(stateMsg).setMsgSeq(seq.getAndIncrement()).build();
                        DatagramPacket packet = new DatagramPacket(gameMessage.toByteArray(), gameMessage.toByteArray().length, node.getAddress(), node.getPort());
                        AcknowledgeWait acknowledgeWait = new AcknowledgeWait(System.currentTimeMillis(), packet, gameMessage);
                        node.getAcknowledgeWaits().add(acknowledgeWait);
                        socket.send(packet);
                    }
                    gameController.update(gameState);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, config.getStateDelayMs(), config.getStateDelayMs(), TimeUnit.MILLISECONDS);

        listen();

    }

    private List<SnakesProto.GameState.Snake> toProtoSnake(Collection<Snake> gameSnake) {
        List<SnakesProto.GameState.Snake> protoSnakes = new ArrayList<>();
        for (Snake snake : gameSnake) {
            SnakesProto.GameState.Snake.SnakeState snakeState = snake.isAlive() ? SnakesProto.GameState.Snake.SnakeState.ALIVE : SnakesProto.GameState.Snake.SnakeState.ZOMBIE;
            SnakesProto.Direction direction = switch (snake.getMoveDirection()) {
                case UP -> SnakesProto.Direction.UP;
                case DOWN -> SnakesProto.Direction.DOWN;
                case LEFT -> SnakesProto.Direction.LEFT;
                case RIGHT -> SnakesProto.Direction.RIGHT;
            };
            int playerId = snake.getSnakeId();
            List<SnakesProto.GameState.Coord> protoCoords = toProtoCoords(snake.getCoordList());
            SnakesProto.GameState.Snake protoSnake = SnakesProto.GameState.Snake.newBuilder().setState(snakeState).setPlayerId(playerId).addAllPoints(protoCoords).setHeadDirection(direction).build();
            protoSnakes.add(protoSnake);

        }
        return protoSnakes;
    }

    private List<SnakesProto.GameState.Coord> toProtoCoords(Collection<Coord> coords) {
        List<SnakesProto.GameState.Coord> protoCoords = new ArrayList<>();
        for (Coord coord : coords) {
            protoCoords.add(SnakesProto.GameState.Coord.newBuilder()
                    .setX(coord.getX())
                    .setY(coord.getY())
                    .build());
        }
        return protoCoords;
    }

    private Coord toGameCoord(SnakesProto.GameState.Coord coord) {
        if (coord.getX() == 1 && coord.getY() == 0) return Coord.RIGHT;
        if (coord.getX() == -1 && coord.getY() == 0) return Coord.LEFT;
        if (coord.getX() == 0 && coord.getY() == 1) return Coord.DOWN;
        if (coord.getX() == 0 && coord.getY() == -1) return Coord.UP;
        return new Coord(coord.getX(), coord.getY());
    }

    private List<SnakesProto.GamePlayer> getProtoPlayer() {
        List<SnakesProto.GamePlayer> gamePlayers = new ArrayList<>();
        for (NodeInfo node : nodes.values()) {
            SnakesProto.GamePlayer player = SnakesProto.GamePlayer.newBuilder()
                    .setName(node.getName())
                    .setId(node.getId())
                    .setIpAddress(node.getAddress().getCanonicalHostName())
                    .setPort(node.getPort())
                    .setRole(getProtoRole(node.getNodeRole()))
                    .setType(SnakesProto.PlayerType.HUMAN)
                    .setScore(node.getScore())
                    .build();
            gamePlayers.add(player);

        }
        SnakesProto.GamePlayer player = SnakesProto.GamePlayer.newBuilder()
                .setName(name)
                .setId(id)
                .setIpAddress("")
                .setPort(0)
                .setRole(getProtoRole(role))
                .setType(SnakesProto.PlayerType.HUMAN)
                .setScore(score)
                .build();
        gamePlayers.add(player);
        return gamePlayers;
    }


    @Override
    protected DatagramPacket processSteer(SnakesProto.GameMessage message, DatagramPacket packet) {
        NodeInfo node;
        if ((node = nodes.get(packet.getSocketAddress())) == null || !model.getSnake(node.getId()).isAlive()) {
            return null;
        }
        node.setLastSeenOnline(System.currentTimeMillis());
        SnakesProto.GameMessage.SteerMsg steerMsg = message.getSteer();
        MoveDirection moveDirection = toGameDirection(steerMsg.getDirection());
        model.setTurn(node.getId(), moveDirection);
        SnakesProto.GameMessage.AckMsg ackMsg = SnakesProto.GameMessage.AckMsg.newBuilder().build();
        SnakesProto.GameMessage gameMessage = SnakesProto.GameMessage.newBuilder().setAck(ackMsg).setMsgSeq(message.getMsgSeq()).build();
        return new DatagramPacket(gameMessage.toByteArray(), gameMessage.toByteArray().length, node.getAddress(), node.getPort());
    }


    @Override
    protected DatagramPacket processState(SnakesProto.GameMessage message, DatagramPacket packet) {
        return null;
    }


    @Override
    protected DatagramPacket processJoin(SnakesProto.GameMessage message, DatagramPacket packet) {
        SnakesProto.GameMessage.JoinMsg msg = message.getJoin();
        String name = msg.getName();
        System.out.println("join" + name);
        int id = idPool.getId();
        if (msg.getOnlyView()) {
            NodeInfo node = nodes.computeIfAbsent(packet.getSocketAddress(), k -> new NodeInfo(msg.getName(), id, packet.getAddress(), packet.getPort(), NodeRole.VIEWER));
            SnakesProto.GameMessage.AckMsg ackMsg = SnakesProto.GameMessage.AckMsg.newBuilder().build();
            SnakesProto.GameMessage gameMessage = SnakesProto.GameMessage.newBuilder().setAck(ackMsg).setMsgSeq(message.getMsgSeq()).setReceiverId(id).setSenderId(id).build();
            return new DatagramPacket(gameMessage.toByteArray(), gameMessage.toByteArray().length, node.getAddress(), node.getPort());
        } else {
            try {
                model.addPlayer(id);
                NodeInfo node = nodes.computeIfAbsent(packet.getSocketAddress(), k -> new NodeInfo(msg.getName(), id, packet.getAddress(), packet.getPort(), NodeRole.NORMAL));
                node.setLastSeenOnline(System.currentTimeMillis());
                SnakesProto.GameMessage.AckMsg ackMsg = SnakesProto.GameMessage.AckMsg.newBuilder().build();
                SnakesProto.GameMessage gameMessage = SnakesProto.GameMessage.newBuilder().setAck(ackMsg).setMsgSeq(message.getMsgSeq()).setReceiverId(id).setSenderId(id).build();
                return new DatagramPacket(gameMessage.toByteArray(), gameMessage.toByteArray().length, node.getAddress(), node.getPort());
            } catch (SnakeException e) {
                e.printStackTrace();
                SnakesProto.GameMessage.ErrorMsg errorMsg = SnakesProto.GameMessage.ErrorMsg.newBuilder().setErrorMessage(e.getMessage()).build();
                SnakesProto.GameMessage gameMessage = SnakesProto.GameMessage.newBuilder().setError(errorMsg).setMsgSeq(seq.getAndIncrement()).build();
                DatagramPacket answPacket = new DatagramPacket(gameMessage.toByteArray(), gameMessage.toByteArray().length, packet.getSocketAddress());
                idPool.freeId(id);
                return answPacket;
            }
        }
    }

    @Override
    protected void shutdown() {

    }

    @Override
    protected DatagramPacket processError(SnakesProto.GameMessage message, DatagramPacket packet) {
        return null;
    }

    @Override
    protected DatagramPacket processRole(SnakesProto.GameMessage message, DatagramPacket packet) {
        return null;
    }
}
