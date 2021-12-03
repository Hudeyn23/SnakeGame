package com.example.snakegame;

import me.ippolitov.fit.snakes.SnakesProto;

public class AcknowledgeWait implements Comparable<AcknowledgeWait> {
    private final Long time;
    private final SnakesProto.GameMessage gameMessage;

    public AcknowledgeWait(Long time, SnakesProto.GameMessage gameMessage) {
        this.time = time;
        this.gameMessage = gameMessage;
    }

    public SnakesProto.GameMessage getGameMessage() {
        return gameMessage;
    }

    @Override
    public int compareTo(AcknowledgeWait o) {
        return Long.compare(time, o.getTime());
    }

    public Long getTime() {
        return time;
    }

}
