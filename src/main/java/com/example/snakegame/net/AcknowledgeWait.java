package com.example.snakegame.net;

import me.ippolitov.fit.snakes.SnakesProto;

import java.net.DatagramPacket;

public class AcknowledgeWait implements Comparable<AcknowledgeWait> {
    private long time;
    private final DatagramPacket packet;
    private final SnakesProto.GameMessage gameMessage;
    public AcknowledgeWait(Long time, DatagramPacket packet, SnakesProto.GameMessage gameMessage) {
        this.time = time;
        this.packet = packet;
        this.gameMessage = gameMessage;
    }

    public DatagramPacket getPacket() {
        return packet;
    }

    public SnakesProto.GameMessage getGameMessage() {
        return gameMessage;
    }

    public void setTime(long time) {
        this.time = time;
    }

    @Override
    public int compareTo(AcknowledgeWait o) {
        return Long.compare(time, o.getTime());
    }

    public long getTime() {
        return time;
    }

}
