package com.example.snakegame;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Model {
    Field field;
    private int idCounter = 0;
    private Map<Integer, Snake> snakes;
    private int sizex;
    private int sizey;
    private List<Cell> emptyCells;
    private int round = 0;

    public Model(int sizex, int sizey) {
        this.sizex = sizex;
        this.sizey = sizey;
        field = new Field(sizex, sizey);
        snakes = new HashMap<>();

    }

    public int setTurn(int id, int round, MoveDirection moveDirection) {
        if (this.round == round) {
            Snake snake = snakes.get(id);
            snake.setMoveDirection(moveDirection);
        }
    }

    public int addPlayer(String name) {
        Snake snake = new Snake(++idCounter, name);
        if (snakes.putIfAbsent(idCounter, snake) != null) {
            return -1;
        }
        return idCounter;
    }

    public void makeTurns() {
        for (Snake snake : snakes.values()) {
            MoveDirection direction = snake.getMoveDirection();
            Coord headCoord = snake.getHeadCoord();
            if (direction == MoveDirection.FORWARD) {
                Cell cell = field.getCell(headCoord.getX(), (headCoord.getY() + 1) % sizey);
                move(snake, cell, direction);
            }
            if (direction == MoveDirection.BACKWARD) {
                Cell cell = field.getCell(headCoord.getX(), (headCoord.getY() - 1) % sizey);
                move(snake, cell, direction);
            }
            if (direction == MoveDirection.LEFT) {
                Cell cell = field.getCell((headCoord.getX() - 1) % sizex, headCoord.getY());
                move(snake, cell, direction);
            }
            if (direction == MoveDirection.RIGHT) {
                Cell cell = field.getCell((headCoord.getX() + 1) % sizex, headCoord.getY());
                move(snake, cell, direction);
            }
        }
    }


    private void move(Snake snake, Cell cell, MoveDirection direction) {
        if (cell.isEmpty()) {
            cell.setSnake(snake);
            addCellToList(snake, direction);
            Coord tail = snake.getCoordList().remove(0);
            Coord preTail = snake.getCoordList().get(0);
            snake.getCoordList().set(0, new Coord(tail.getX() + preTail.getX(), tail.getY() + preTail.getY()));
            snake.setTurnDone(true);
            emptyCells.remove(cell);
        } else if (cell.isFood()) {
            cell.setSnake(snake);
            cell.setFood(false);
            addCellToList(snake, direction);
            snake.setTurnDone(true);
        } else if (cell.getSnake() != null) {
            if (cell.getSnake().isTurnDone()) {
                for (Coord coord : cell.getSnake().getCoordList()) {
                    field.getCell(coord.getX(), coord.getY()).setSnake(null);
                    field.getCell(coord.getX(), coord.getY()).setEmpty(true);
                    emptyCells.add(field.getCell(coord.getX(), coord.getY()));
                }
            }
            for (Coord coord : snake.getCoordList()) {
                field.getCell(coord.getX(), coord.getY()).setSnake(null);
                field.getCell(coord.getX(), coord.getY()).setEmpty(true);
                emptyCells.add(field.getCell(coord.getX(), coord.getY()));
            }
        }
    }

    private void addCellToList(Snake snake, MoveDirection direction) {
        if (direction == MoveDirection.FORWARD)
            snake.getCoordList().add(new Coord(0, 1));
        if (direction == MoveDirection.BACKWARD)
            snake.getCoordList().add(new Coord(0, 0));
        if (direction == MoveDirection.RIGHT)
            snake.getCoordList().add(new Coord(1, 0));
        if (direction == MoveDirection.LEFT)
            snake.getCoordList().add(new Coord(-1, 0));
    }

}
