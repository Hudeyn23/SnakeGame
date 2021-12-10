package com.example.snakegame.net;

import java.net.InetAddress;
import java.net.SocketAddress;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.PriorityBlockingQueue;

public class NodeInfo {
    private String name;
    private int id;
    private InetAddress address;
    private int port;
    private NodeRole nodeRole;
    private long lastSeenOnline;
    private boolean isPinged;
    private int score;
    Queue<AcknowledgeWait> acknowledgeWaits = new PriorityBlockingQueue<>();

    public NodeInfo(String name, int id, InetAddress address, int port, NodeRole nodeRole) {
        this.name = name;
        this.id = id;
        this.address = address;
        this.port = port;
        this.nodeRole = nodeRole;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }


    public String getName() {
        return name;
    }

    public Queue<AcknowledgeWait> getAcknowledgeWaits() {
        return acknowledgeWaits;
    }

    public NodeRole getNodeRole() {
        return nodeRole;
    }

    public InetAddress getAddress() {
        return address;
    }

    public long getLastSeenOnline() {
        return lastSeenOnline;
    }

    public void setLastSeenOnline(long lastSeenOnline) {
        this.lastSeenOnline = lastSeenOnline;
    }

    public boolean isPinged() {
        return isPinged;
    }

    public void setPinged(boolean pinged) {
        isPinged = pinged;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NodeInfo nodeInfo = (NodeInfo) o;
        return Objects.equals(address, nodeInfo.address);
    }


    @Override
    public int hashCode() {
        return Objects.hash(address);
    }

    public int getPort() {
        return port;
    }


    public int getId() {
        return id;
    }


}
