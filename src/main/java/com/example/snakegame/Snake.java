package com.example.snakegame;

import java.util.List;

public class Snake {
    private int snakeId;
    private String name;
    private List<Coord> coordList;
    private Coord headCoord;
    private boolean turnDone;
    private int length;
    MoveDirection moveDirection = MoveDirection.FORWARD;

    public Coord getHeadCoord() {
        return headCoord;
    }

    public void setHeadCoord(Coord headCoord) {
        this.headCoord = headCoord;
    }

    public void setMoveDirection(MoveDirection moveDirection) {
        this.moveDirection = moveDirection;
    }

    public Snake(int snakeId, String name) {
        this.snakeId = snakeId;
        this.name = name;
    }

    public void setTurnDone(boolean turnDone) {
        this.turnDone = turnDone;
    }

    public boolean isTurnDone() {
        return turnDone;
    }

    public MoveDirection getMoveDirection() {
        return moveDirection;
    }

    public List<Coord> getCoordList() {
        return coordList;
    }
}
