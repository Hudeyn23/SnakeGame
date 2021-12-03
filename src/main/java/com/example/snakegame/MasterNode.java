package com.example.snakegame;

import javafx.scene.Node;
import me.ippolitov.fit.snakes.SnakesProto;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class MasterNode extends AbstractNode {

    public MasterNode() throws UnknownHostException {
    }

    @Override
    public void run() {
        ScheduledExecutorService service = Executors.newScheduledThreadPool(3);
        service.scheduleAtFixedRate(() -> {
            SnakesProto.GameMessage.AnnouncementMsg announcementMsg = SnakesProto.GameMessage.AnnouncementMsg.newBuilder().setConfig(config).setPlayers(SnakesProto.GamePlayers.newBuilder().addAllPlayers(getProtoPlayer())).build();
            SnakesProto.GameMessage gameMessage = SnakesProto.GameMessage.newBuilder().setAnnouncement(announcementMsg).setMsgSeq(seq.getAndIncrement()).build();
            DatagramPacket packet = new DatagramPacket(gameMessage.toByteArray(), gameMessage.toByteArray().length, multicastAddress, 9192);
            try {
                socket.send(packet);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }, 0, 1, TimeUnit.SECONDS);
        service.scheduleAtFixedRate(() -> {
            for (NodeInfo node : nodes.values()) {
                List<SnakesProto.GameState.Snake> snakes = toProtoSnake(model.getSnakes());
                List<SnakesProto.GameState.Coord> foodCoords = toProtoCoords(model.getFoodCells().stream().map(Cell::getCoord).collect(Collectors.toList()));
                List<SnakesProto.GamePlayer> players = getProtoPlayer();
                SnakesProto.GamePlayers gamePlayers = SnakesProto.GamePlayers.newBuilder().addAllPlayers(players).build();
                SnakesProto.GameState gameState = SnakesProto.GameState.newBuilder().setStateOrder(stateOrder).addAllSnakes(snakes).addAllFoods(foodCoords).setPlayers(gamePlayers).setConfig(config).build();
                SnakesProto.GameMessage.StateMsg stateMsg = SnakesProto.GameMessage.StateMsg.newBuilder().setState(gameState).build();
                SnakesProto.GameMessage gameMessage = SnakesProto.GameMessage.newBuilder().setState(stateMsg).setMsgSeq(seq.getAndIncrement()).build();
                DatagramPacket packet = new DatagramPacket(gameMessage.toByteArray(), gameMessage.toByteArray().length);
                AcknowledgeWait acknowledgeWait = new AcknowledgeWait(System.currentTimeMillis(), gameMessage);
                node.getAcknowledgeWaits().add(acknowledgeWait);
                try {
                    socket.send(packet);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }, config.getStateDelayMs(), config.getStateDelayMs(), TimeUnit.MILLISECONDS);
        service.scheduleAtFixedRate(() -> {
            for(NodeInfo node : nodes.values()){
                if (node.getAcknowledgeWaits().peek())
            }
        },)
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

    private List<SnakesProto.GamePlayer> getProtoPlayer() {
        List<SnakesProto.GamePlayer> gamePlayers = new ArrayList<>();
        for (NodeInfo node : nodes.values()) {
            SnakesProto.GamePlayer player = SnakesProto.GamePlayer.newBuilder()
                    .setName(node.getName())
                    .setId(node.getId())
                    .setIpAddress(node.getAddress().toString())
                    .setPort(node.getPort())
                    .setRole(getProtoRole(node.getNodeRole()))
                    .setType(SnakesProto.PlayerType.HUMAN)
                    .setScore(model.getSnake(node.getId()).getScore())
                    .build();
        }
        return gamePlayers;
    }

    private SnakesProto.NodeRole getProtoRole(NodeRole role) {
        return switch (role) {
            case DEPUTY -> SnakesProto.NodeRole.DEPUTY;
            case MASTER -> SnakesProto.NodeRole.MASTER;
            case NORMAL -> SnakesProto.NodeRole.NORMAL;
            case VIEWER -> SnakesProto.NodeRole.VIEWER;
        };
    }

    @Override
    protected SnakesProto.GameMessage processPing(SnakesProto.GameMessage message, DatagramPacket packet) {
        NodeInfo node;
        if ((node = nodes.get(packet.getSocketAddress())) == null) {
            return null;
        }
        SnakesProto.GameMessage.AckMsg ackMsg = SnakesProto.GameMessage.AckMsg.newBuilder().build();
        SnakesProto.GameMessage gameMessage = SnakesProto.GameMessage.newBuilder().setAck(ackMsg).setMsgSeq(message.getMsgSeq()).setReceiverId(node.getId()).setSenderId(id).build();
        AcknowledgeWait acknowledgeWait = new AcknowledgeWait(System.currentTimeMillis(), gameMessage);
        node.getAcknowledgeWaits().add(acknowledgeWait);
        return gameMessage;
    }

    @Override
    protected SnakesProto.GameMessage processSteer(SnakesProto.GameMessage message, DatagramPacket packet) {
        NodeInfo node;
        if ((node = nodes.get(packet.getSocketAddress())) == null) {
            return null;
        }
        SnakesProto.GameMessage.SteerMsg steerMsg = message.getSteer();
        switch (steerMsg.getDirection()) {
            case UP -> model.setTurn(node.getId(), MoveDirection.UP);
            case DOWN -> model.setTurn(node.getId(), MoveDirection.DOWN);
            case LEFT -> model.setTurn(node.getId(), MoveDirection.LEFT);
            case RIGHT -> model.setTurn(node.getId(), MoveDirection.RIGHT);
        }
        SnakesProto.GameMessage.AckMsg ackMsg = SnakesProto.GameMessage.AckMsg.newBuilder().build();
        return SnakesProto.GameMessage.newBuilder().setAck(ackMsg).setMsgSeq(message.getMsgSeq()).build();
    }

    @Override
    protected SnakesProto.GameMessage processAck(SnakesProto.GameMessage message, DatagramPacket packet) {
        NodeInfo node;
        if ((node = nodes.get(packet.getSocketAddress())) == null) {
            return null;
        }
        node.getAcknowledgeWaits().removeIf(wait -> wait.getGameMessage().getMsgSeq() == message.getMsgSeq());
        return null;
    }

    @Override
    protected SnakesProto.GameMessage processState(SnakesProto.GameMessage message, Node node) {
        return null;
    }

    @Override
    protected SnakesProto.GameMessage processAnnounce(SnakesProto.GameMessage message, DatagramPacket packet) {
        return null;
    }

    @Override
    protected SnakesProto.GameMessage processJoin(SnakesProto.GameMessage message, DatagramPacket packet) {
        SnakesProto.GameMessage.JoinMsg msg = message.getJoin();
        String name = msg.getName();
        int id = idPool.getId();
        if (msg.getOnlyView()) {
            NodeInfo node = nodes.computeIfAbsent(packet.getSocketAddress(), k -> new NodeInfo(name, id, k, packet.getPort(), NodeRole.VIEWER));
            SnakesProto.GameMessage.AckMsg ackMsg = SnakesProto.GameMessage.AckMsg.newBuilder().build();
            return SnakesProto.GameMessage.newBuilder().setAck(ackMsg).setMsgSeq(message.getMsgSeq()).build();
        } else {
            try {
                model.addPlayer(msg.getName(), id);
                NodeInfo node = nodes.computeIfAbsent(packet.getSocketAddress(), k -> new NodeInfo(name, id, k, packet.getPort(), NodeRole.NORMAL));
                SnakesProto.GameMessage.AckMsg ackMsg = SnakesProto.GameMessage.AckMsg.newBuilder().build();
                return SnakesProto.GameMessage.newBuilder().setAck(ackMsg).setMsgSeq(message.getMsgSeq()).build();
            } catch (SnakeException e) {
                e.printStackTrace();
                NodeInfo node = nodes.computeIfAbsent(packet.getSocketAddress(), k -> new NodeInfo(name, id, k, packet.getPort(), NodeRole.VIEWER));
                SnakesProto.GameMessage.ErrorMsg errorMsg = SnakesProto.GameMessage.ErrorMsg.newBuilder().setErrorMessage(e.getMessage()).build();
                SnakesProto.GameMessage gameMessage = SnakesProto.GameMessage.newBuilder().setError(errorMsg).setMsgSeq(seq.getAndIncrement()).build();
                AcknowledgeWait acknowledgeWait = new AcknowledgeWait(System.currentTimeMillis(), gameMessage);
                node.getAcknowledgeWaits().add(acknowledgeWait);
                return gameMessage;
            }
        }
    }

    @Override
    protected SnakesProto.GameMessage processError(SnakesProto.GameMessage message, DatagramPacket packet) {
        return null;
    }

    @Override
    protected SnakesProto.GameMessage processRole(SnakesProto.GameMessage message, DatagramPacket packet) {
        return null;
    }
}
