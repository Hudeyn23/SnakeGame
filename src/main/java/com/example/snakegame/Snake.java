package com.example.snakegame;

import java.util.List;

public class Snake {
    private int snakeId;
    private String name;
    private List<Coord> coordList;
    private Coord headCoord;
    private boolean turnDone;
    private int length;
    private boolean alive;
    private int score = 0;
    MoveDirection moveDirection;

    public Coord getHeadCoord() {
        return headCoord;
    }

    public void setHeadCoord(Coord headCoord) {
        this.headCoord = headCoord;
    }

    public void setMoveDirection(MoveDirection moveDirection) {
        this.moveDirection = moveDirection;
    }

    public Snake(int snakeId, String name, MoveDirection moveDirection) {
        this.snakeId = snakeId;
        this.name = name;
        this.moveDirection = moveDirection;
    }

    public int getSnakeId() {
        return snakeId;
    }

    public void addScore(int incr) {
        score += incr;
    }

    public int getScore() {
        return score;
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

    public boolean isAlive() {
        return alive;
    }

    public void setAlive(boolean alive) {
        this.alive = alive;
    }
}
