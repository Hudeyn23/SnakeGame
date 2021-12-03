package com.example.snakegame;

import javafx.scene.Node;

import java.io.IOException;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

public abstract class AbstractNode extends Thread {
    protected me.ippolitov.fit.snakes.SnakesProto.GameConfig config;
    protected DatagramSocket socket;
    protected Map<SocketAddress, NodeInfo> nodes = new ConcurrentHashMap<>();
    // protected IdPool idPool = new IdPool(10);
    protected AtomicLong seq = new AtomicLong(0);
    protected IdPool idPool = new IdPool(10);
    protected Model model;
    protected int stateOrder = 0;
    protected NodeRole role;
    protected int id;
    private final String MULTICAST_ADDRESS = "239.192.0.4";
    protected InetAddress multicastAddress;
    pr
    public AbstractNode() throws UnknownHostException {
        multicastAddress = InetAddress.getByName(MULTICAST_ADDRESS);
    }

    protected void listen() {
        byte[] buffer = new byte[65536];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        while (Thread.currentThread().isInterrupted()) {
            try {
                socket.receive(packet);
                me.ippolitov.fit.snakes.SnakesProto.GameMessage gameMessage = me.ippolitov.fit.snakes.SnakesProto.GameMessage.parseFrom(packet.getData());
                me.ippolitov.fit.snakes.SnakesProto.GameMessage answ = processMessage(gameMessage, packet);
                DatagramPacket answPacket = new DatagramPacket(answ.toByteArray(), answ.toByteArray().length, packet.getAddress(), packet.getPort());
                if (answPacket != null)
                    socket.send(answPacket);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private me.ippolitov.fit.snakes.SnakesProto.GameMessage processMessage(me.ippolitov.fit.snakes.SnakesProto.GameMessage message, DatagramPacket packet) {
        me.ippolitov.fit.snakes.SnakesProto.GameMessage answ = null;
        switch (message.getTypeCase()) {
            case PING -> answ = processPing(message, packet);
            case STEER -> answ = processSteer(message, packet);
            case ACK -> answ = processAck(message, packet);
            // case STATE -> answ = processState(message, packet);
            case ANNOUNCEMENT -> answ = processAnnounce(message, packet);
            case JOIN -> answ = processJoin(message, packet);
            case ERROR -> answ = processError(message, packet);
            case ROLE_CHANGE -> answ = processRole(message, packet);
        }
        return answ;
    }

    abstract protected me.ippolitov.fit.snakes.SnakesProto.GameMessage processPing(me.ippolitov.fit.snakes.SnakesProto.GameMessage message, DatagramPacket packet);

    abstract protected me.ippolitov.fit.snakes.SnakesProto.GameMessage processSteer(me.ippolitov.fit.snakes.SnakesProto.GameMessage message, DatagramPacket packet);

    abstract protected me.ippolitov.fit.snakes.SnakesProto.GameMessage processAck(me.ippolitov.fit.snakes.SnakesProto.GameMessage message, DatagramPacket packet);

    abstract protected me.ippolitov.fit.snakes.SnakesProto.GameMessage processState(me.ippolitov.fit.snakes.SnakesProto.GameMessage message, Node node);

    abstract protected me.ippolitov.fit.snakes.SnakesProto.GameMessage processAnnounce(me.ippolitov.fit.snakes.SnakesProto.GameMessage message, DatagramPacket packet);

    abstract protected me.ippolitov.fit.snakes.SnakesProto.GameMessage processJoin(me.ippolitov.fit.snakes.SnakesProto.GameMessage message, DatagramPacket packet);

    abstract protected me.ippolitov.fit.snakes.SnakesProto.GameMessage processError(me.ippolitov.fit.snakes.SnakesProto.GameMessage message, DatagramPacket packet);

    abstract protected me.ippolitov.fit.snakes.SnakesProto.GameMessage processRole(me.ippolitov.fit.snakes.SnakesProto.GameMessage message, DatagramPacket packet);
}
