package com.example.snakegame;

import java.net.InetAddress;
import java.net.SocketAddress;
import java.util.Queue;
import java.util.concurrent.PriorityBlockingQueue;

public class NodeInfo {
    private String name;
    private int id;
    private SocketAddress address;
    private int port;
    private NodeRole nodeRole;
    Queue<AcknowledgeWait> acknowledgeWaits = new PriorityBlockingQueue<>();
    public NodeInfo(String name, int id, SocketAddress address, int port, NodeRole nodeRole) {
        this.name = name;
        this.id = id;
        this.address = address;
        this.port = port;
        this.nodeRole = nodeRole;
    }

    public Queue<AcknowledgeWait> getAcknowledgeWaits() {
        return acknowledgeWaits;
    }

    public NodeRole getNodeRole() {
        return nodeRole;
    }

    public SocketAddress getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    public String getName() {
        return name;
    }

    public int getId() {
        return id;
    }
}
