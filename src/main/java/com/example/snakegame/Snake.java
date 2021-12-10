package com.example.snakegame;

import java.util.ArrayList;
import java.util.List;

public class Snake {
    private int snakeId;
    private List<Coord> coordList;
    private boolean turnDone;
    private boolean alive;
    private int score = 0;
    MoveDirection moveDirection;

    public void setMoveDirection(MoveDirection moveDirection) {
        this.moveDirection = moveDirection;
    }

    public Snake(int snakeId, MoveDirection moveDirection) {
        this.snakeId = snakeId;
        this.moveDirection = moveDirection;
        coordList = new ArrayList<>();
        alive = true;
    }

    public void setScore(int score) {
        this.score = score;
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
