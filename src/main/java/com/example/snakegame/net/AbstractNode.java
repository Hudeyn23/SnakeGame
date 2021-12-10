package com.example.snakegame.net;

import com.example.snakegame.GameInfo;
import com.example.snakegame.MoveDirection;
import com.example.snakegame.ui.GameController;
import me.ippolitov.fit.snakes.SnakesProto;

import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicLong;

public abstract class AbstractNode extends Thread {
    protected GameController gameController;
    protected String name;
    protected me.ippolitov.fit.snakes.SnakesProto.GameConfig config;
    protected DatagramSocket socket;
    protected Map<SocketAddress, NodeInfo> nodes;
    protected IdPool idPool = new IdPool(10);
    protected AtomicLong seq = new AtomicLong(0);
    protected int stateOrder = 0;
    protected NodeRole role;
    protected int id;
    private final String MULTICAST_ADDRESS = "239.192.0.4";
    protected HashMap<SocketAddress, GameInfo> gamesInfo;
    protected final InetSocketAddress multicastAddress;
    protected MulticastSocket multicastSocket;
    protected ScheduledExecutorService service = Executors.newScheduledThreadPool(4);
    protected boolean isShutdown = false;
    protected int score = 0;

    public AbstractNode() throws IOException {
        nodes = new ConcurrentHashMap<>();
        multicastAddress = new InetSocketAddress(MULTICAST_ADDRESS, 9192);
        multicastSocket = new MulticastSocket(9192);
        socket = new DatagramSocket();
        multicastSocket.joinGroup(multicastAddress.getAddress());
        gamesInfo = new HashMap<>();
    }


    protected void shutdown() {
        service.shutdown();
        isShutdown = true;
    }

    abstract protected void startAcknowledgeService();

    protected void listen() {
        byte[] buffer = new byte[65536];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        while (!isShutdown) {
            try {
                socket.receive(packet);
                byte[] buf2 = Arrays.copyOf(packet.getData(), packet.getLength());
                me.ippolitov.fit.snakes.SnakesProto.GameMessage gameMessage = me.ippolitov.fit.snakes.SnakesProto.GameMessage.parseFrom(buf2);
                DatagramPacket answ = processMessage(gameMessage, packet);
                if (answ != null)
                    socket.send(answ);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private DatagramPacket processMessage(me.ippolitov.fit.snakes.SnakesProto.GameMessage message, DatagramPacket packet) {
        DatagramPacket answ = null;
        switch (message.getTypeCase()) {
            case PING -> answ = processPing(message, packet);
            case STEER -> answ = processSteer(message, packet);
            case ACK -> answ = processAck(message, packet);
            case STATE -> answ = processState(message, packet);
            // case ANNOUNCEMENT -> answ = processAnnounce(message, packet);
            case JOIN -> answ = processJoin(message, packet);
            case ERROR -> answ = processError(message, packet);
            case ROLE_CHANGE -> answ = processRole(message, packet);
        }
        return answ;
    }

    protected MoveDirection toGameDirection(SnakesProto.Direction direction) {
        return switch (direction) {
            case UP -> MoveDirection.UP;
            case DOWN -> MoveDirection.DOWN;
            case LEFT -> MoveDirection.LEFT;
            case RIGHT -> MoveDirection.RIGHT;
        };
    }

    protected SnakesProto.Direction toProtoDirection(MoveDirection direction) {
        return switch (direction) {
            case UP -> SnakesProto.Direction.UP;
            case DOWN -> SnakesProto.Direction.DOWN;
            case LEFT -> SnakesProto.Direction.LEFT;
            case RIGHT -> SnakesProto.Direction.RIGHT;
        };
    }

    protected SnakesProto.NodeRole getProtoRole(NodeRole role) {
        return switch (role) {
            case DEPUTY -> SnakesProto.NodeRole.DEPUTY;
            case MASTER -> SnakesProto.NodeRole.MASTER;
            case NORMAL -> SnakesProto.NodeRole.NORMAL;
            case VIEWER -> SnakesProto.NodeRole.VIEWER;
        };
    }


    protected NodeRole getNodeRole(SnakesProto.NodeRole role) {
        return switch (role) {
            case DEPUTY -> NodeRole.DEPUTY;
            case MASTER -> NodeRole.MASTER;
            case NORMAL -> NodeRole.NORMAL;
            case VIEWER -> NodeRole.VIEWER;
        };
    }

    public abstract void setTurn(MoveDirection direction);

    private DatagramPacket processAck(me.ippolitov.fit.snakes.SnakesProto.GameMessage message, DatagramPacket packet) {
        NodeInfo node;
        if ((node = nodes.get(packet.getSocketAddress())) == null) {
            System.out.println("node null");
            return null;
        }
        node.setLastSeenOnline(System.currentTimeMillis());
        if (node.getAcknowledgeWaits().removeIf(wait -> wait.getGameMessage().getMsgSeq() == message.getMsgSeq())) {
            System.out.println("get ack " + message.getTypeCase());
        }
        return null;
    }

    protected DatagramPacket processPing(me.ippolitov.fit.snakes.SnakesProto.GameMessage message, DatagramPacket packet) {
        NodeInfo node;
        if ((node = nodes.get(packet.getSocketAddress())) == null) {
            return null;
        }
        node.setLastSeenOnline(System.currentTimeMillis());
        me.ippolitov.fit.snakes.SnakesProto.GameMessage.AckMsg ackMsg = me.ippolitov.fit.snakes.SnakesProto.GameMessage.AckMsg.newBuilder().build();
        me.ippolitov.fit.snakes.SnakesProto.GameMessage gameMessage = me.ippolitov.fit.snakes.SnakesProto.GameMessage.newBuilder().setAck(ackMsg).setMsgSeq(message.getMsgSeq()).setReceiverId(node.getId()).setSenderId(id).build();
        return new DatagramPacket(gameMessage.toByteArray(), gameMessage.toByteArray().length, node.getAddress(), node.getPort());
    }

    abstract protected DatagramPacket processSteer(me.ippolitov.fit.snakes.SnakesProto.GameMessage message, DatagramPacket packet);

    abstract protected DatagramPacket processState(me.ippolitov.fit.snakes.SnakesProto.GameMessage message, DatagramPacket packet);


    abstract protected DatagramPacket processJoin(me.ippolitov.fit.snakes.SnakesProto.GameMessage message, DatagramPacket packet);

    abstract protected DatagramPacket processError(me.ippolitov.fit.snakes.SnakesProto.GameMessage message, DatagramPacket packet);

    abstract protected DatagramPacket processRole(me.ippolitov.fit.snakes.SnakesProto.GameMessage message, DatagramPacket packet);
}
