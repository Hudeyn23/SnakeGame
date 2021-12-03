/*
package com.example.snakegame;

import com.example.snakegame.IdPool;
import com.example.snakegame.NodeInfo;
import me.ippolitov.fit.snakes.SnakesProto;

import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class NodeService extends Thread {
    private SnakesProto.GameConfig config;
    private DatagramSocket socket;
    private Map<InetAddress, NodeInfo> nodes = new ConcurrentHashMap<>();
    private IdPool idPool = new IdPool(10);
    private AtomicLong seq = new AtomicLong(0);
    private Model model;
    private int stateOrder = 0;
    private Queue<AcknowledgeWait> waitQueue = new PriorityBlockingQueue<>();

    public NodeService() throws IOException {
        socket = new MulticastSocket(9192);
    }

    public void startServer(SnakesProto.GameConfig config) {
        this.config = config;
        Model model = new Model(config.getWidth(), config.getHeight());
        start();
    }

    @Override
    public void run() {
        try {
            group = InetAddress.getByName("239.192.0.4");
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        ScheduledExecutorService service = Executors.newScheduledThreadPool(2);
        service.scheduleAtFixedRate(() -> {
            SnakesProto.GameMessage.AnnouncementMsg announcementMsg = SnakesProto.GameMessage.AnnouncementMsg.newBuilder().setConfig(config).setPlayers(SnakesProto.GamePlayers.newBuilder().addAllPlayers(players.values())).build();
            SnakesProto.GameMessage gameMessage = SnakesProto.GameMessage.newBuilder().setAnnouncement(announcementMsg).setMsgSeq(seq.getAndIncrement()).build();
            DatagramPacket packet = new DatagramPacket(gameMessage.toByteArray(), gameMessage.toByteArray().length);
            try {
                socket.send(packet);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }, 0, 1, TimeUnit.SECONDS);
        service.scheduleAtFixedRate(() -> {
            List<SnakesProto.GameState.Snake> snakes = toProtoSnake(model.getSnakes());
            List<SnakesProto.GameState.Coord> coords = toProtoCoords(model.getFoodCells().stream().map(Cell::getCoord).collect(Collectors.toList()));
            List<SnakesProto.GamePlayers> players = getProtoPlayer();

            SnakesProto.GameState gameState = SnakesProto.GameState.newBuilder().set
            SnakesProto.GameMessage.StateMsg stateMsg = SnakesProto.GameMessage.StateMsg.newBuilder().setState().build();
        }, config.getStateDelayMs(), config.getStateDelayMs(), TimeUnit.MILLISECONDS);
        listen();
    }

    private void listen() {
        byte[] buffer = new byte[65536];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        while (isInterrupted()) {
            try {
                socket.receive(packet);
                SnakesProto.GameMessage gameMessage = SnakesProto.GameMessage.parseFrom(packet.getData());
                int id = idPool.getId();
                String name = gameMessage.getJoin().getName();
                packet.getAddress();
                NodeRole nodeRole = NodeRole.NORMAL;
                NodeInfo node = nodes.computeIfAbsent(packet.getAddress(), k -> new NodeInfo(name, id, k, packet.getPort(), nodeRole));
                SnakesProto.GameMessage answ = processMessage(gameMessage, node);
                DatagramPacket answPacket = new DatagramPacket(answ.toByteArray(), answ.toByteArray().length, packet.getAddress(), packet.getPort());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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
        List<SnakesProto.GameState.Coord> protoCoord = new ArrayList<>();
        for (Coord coord : coords) {
            protoCoord.add(SnakesProto.GameState.Coord.newBuilder()
                    .setX(coord.getX())
                    .setY(coord.getY())
                    .build());
        }
        return protoCoord;
    }

    private List<SnakesProto.GamePlayers> getProtoPlayer() {
        List<SnakesProto.GamePlayers> gamePlayers = new ArrayList<>();
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

    private SnakesProto.GameMessage processMessage(SnakesProto.GameMessage message, NodeInfo node) {
        SnakesProto.GameMessage answ = null;
        switch (message.getTypeCase()) {
            case PING -> answ = processPing(message, node);
            case STEER -> answ = processSteer(message, node);
            case ACK -> answ = processAck(message, node);
            // case STATE -> answ = processState(message, node);
            case ANNOUNCEMENT -> answ = processAnnounce(message, node);
            case JOIN -> answ = processJoin(message, node);
            case ERROR -> answ = processError(message, node);
            case ROLE_CHANGE -> answ = processRole(message, node);
        }
        return answ;
    }

    private SnakesProto.GameMessage processPing(SnakesProto.GameMessage message, NodeInfo node) {

    }

    private SnakesProto.GameMessage processSteer(SnakesProto.GameMessage message, NodeInfo node) {
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

    private SnakesProto.GameMessage processAck(SnakesProto.GameMessage message, NodeInfo node) {

    }

    // private SnakesProto.GameMessage processState(SnakesProto.GameMessage message, Node node) {

}

    private SnakesProto.GameMessage processAnnounce(SnakesProto.GameMessage message, NodeInfo node) {

    }

    private SnakesProto.GameMessage processJoin(SnakesProto.GameMessage message, NodeInfo node) {
        SnakesProto.GameMessage.JoinMsg msg = message.getJoin();
        if (msg.getOnlyView()) {

        } else {
            try {
                model.addPlayer(msg.getName(), node.getId());
                SnakesProto.GameMessage.AckMsg ackMsg = SnakesProto.GameMessage.AckMsg.newBuilder().build();
                return SnakesProto.GameMessage.newBuilder().setAck(ackMsg).setMsgSeq(message.getMsgSeq()).build();
            } catch (SnakeException e) {
                e.printStackTrace();
                SnakesProto.GameMessage.ErrorMsg errorMsg = SnakesProto.GameMessage.ErrorMsg.newBuilder().setErrorMessage(e.getMessage()).build();
                SnakesProto.GameMessage gameMessage = SnakesProto.GameMessage.newBuilder().setError(errorMsg).setMsgSeq(seq.getAndIncrement()).build();
                AcknowledgeWait acknowledgeWait = new AcknowledgeWait(node, System.currentTimeMillis(), gameMessage);
                waitQueue.add(acknowledgeWait);
                return gameMessage;
            }
        }
    }


    private SnakesProto.GameMessage processError(SnakesProto.GameMessage message, NodeInfo node) {

    }

    private SnakesProto.GameMessage processRole(SnakesProto.GameMessage message, NodeInfo node) {

    }

} */
