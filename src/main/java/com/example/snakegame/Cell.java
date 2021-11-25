package com.example.snakegame;

public class Cell {
    private boolean isEmpty;
    private boolean isFood;
    private Snake snake;
    private final Coord coord;

    public Cell(int x, int y) {
        isEmpty = true;
        isFood = false;
        snake = null;
        coord = new Coord(x, y);
    }

    public Snake getSnake() {
        return snake;
    }

    public Coord getCoord() {
        return coord;
    }

    public boolean isEmpty() {
        return isEmpty;
    }

    public void setEmpty(boolean empty) {
        isEmpty = empty;
    }

    public void setFood(boolean food) {
        isFood = food;
    }

    public void setSnake(Snake snake) {
        this.snake = snake;
    }

    public boolean isFood() {
        return isFood;
    }
}
