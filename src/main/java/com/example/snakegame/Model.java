package com.example.snakegame;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Model {
    Field field;
    private int idCounter = 0;
    private Map<Integer, Snake> snakes;
    private int sizex;
    private int sizey;
    private List<Cell> emptyCells;
    private List<Cell> foodCells;
    private final Coord UP = new Coord(0, 1);
    private final Coord DOWN = new Coord(0, -1);
    private final Coord LEFT = new Coord(-1, 0);
    private final Coord RIGHT = new Coord(1, 0);

    public Model(int sizex, int sizey) {
        this.sizex = sizex;
        this.sizey = sizey;
        field = new Field(sizex, sizey);
        snakes = new ConcurrentHashMap<>();
    }

    public void setTurn(int id, MoveDirection moveDirection) {
        Snake snake = snakes.get(id);
        if (snake.getMoveDirection() == MoveDirection.UP || snake.getMoveDirection() == MoveDirection.DOWN) {
            if (moveDirection == MoveDirection.LEFT || moveDirection == MoveDirection.RIGHT) {
                snake.setMoveDirection(moveDirection);
            }
        }
        if (snake.getMoveDirection() == MoveDirection.LEFT || snake.getMoveDirection() == MoveDirection.RIGHT) {
            if (moveDirection == MoveDirection.UP || moveDirection == MoveDirection.DOWN) {
                snake.setMoveDirection(moveDirection);
            }
        }

    }

    public Collection<Cell> getFoodCells() {
        return foodCells;
    }

    public Collection<Snake> getSnakes() {
        return snakes.values();
    }

    public Snake getSnake(int id) {
        return snakes.get(id);
    }

    public void addPlayer(String name, int id) throws SnakeException {
        Snake snake = null;
        Cell centreCell = null;
        Cell secondCell = null;
        for (Cell cell : emptyCells) {
            boolean isBreak = false;
            Cell begin = field.getCell((cell.getCoord().getX() - 2) % sizex, (cell.getCoord().getY() + 2) % sizey);
            for (int i = 0; i < 5; i++) {
                if (isBreak) {
                    break;
                }
                for (int j = 0; j < 5; j++) {
                    if (field.getCell((begin.getCoord().getX() + i) % sizex, (begin.getCoord().getY() - j) % sizey).getSnake() != null) {
                        isBreak = true;
                        break;
                    }
                }
            }
            if (isBreak)
                continue;
            if ((secondCell = field.getCell(cell.getCoord().getX(), (cell.getCoord().getY() + 1) % sizey)).isEmpty()) {
                snake = new Snake(id, name, MoveDirection.DOWN);
                secondCell.setSnake(snake);
                snake.getCoordList().add(secondCell.getCoord());
                snake.getCoordList().add(DOWN);
            }
            if ((secondCell = field.getCell(cell.getCoord().getX(), (cell.getCoord().getY() - 1) % sizey)).isEmpty()) {
                snake = new Snake(id, name, MoveDirection.UP);
                secondCell.setSnake(snake);
                snake.getCoordList().add(secondCell.getCoord());
                snake.getCoordList().add(UP);
            }
            if ((secondCell = field.getCell((cell.getCoord().getX() + 1) % sizex, cell.getCoord().getY())).isEmpty()) {
                snake = new Snake(id, name, MoveDirection.LEFT);
                secondCell.setSnake(snake);
                snake.getCoordList().add(secondCell.getCoord());
                snake.getCoordList().add(LEFT);
            }
            if ((secondCell = field.getCell((cell.getCoord().getX() - 1) % sizex, cell.getCoord().getY())).isEmpty()) {
                snake = new Snake(id, name, MoveDirection.RIGHT);
                secondCell.setSnake(snake);
                snake.getCoordList().add(secondCell.getCoord());
                snake.getCoordList().add(RIGHT);
            }
            if (snake == null)
                break;
            centreCell = cell;
            centreCell.setSnake(snake);
            break;
        }
        if (centreCell == null) {
            throw new SnakeException("no 5x5 available field");
        }
    }

    public void makeTurns() {
        for (Snake snake : snakes.values()) {
            MoveDirection direction = snake.getMoveDirection();
            Coord headCoord = snake.getHeadCoord();
            if (direction == MoveDirection.UP) {
                Cell cell = field.getCell(headCoord.getX(), (headCoord.getY() + 1) % sizey);
                move(snake, cell, direction);
            }
            if (direction == MoveDirection.DOWN) {
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
        if (direction == MoveDirection.UP)
            snake.getCoordList().add(UP);
        if (direction == MoveDirection.DOWN)
            snake.getCoordList().add(DOWN);
        if (direction == MoveDirection.RIGHT)
            snake.getCoordList().add(RIGHT);
        if (direction == MoveDirection.LEFT)
            snake.getCoordList().add(LEFT);
    }

}
