package com.example.snakegame;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class Model {
    Field field;
    private Map<Integer, Snake> snakes;
    private int sizex;
    private int sizey;
    private List<Cell> emptyCells;
    private List<Cell> foodCells;
    private float foodStatic;
    private float foodPerPlayer;
    private int aliveSnakes;

    public Model(int sizex, int sizey, float foodStatic, float foodPerPlayer) {
        this.sizex = sizex;
        this.sizey = sizey;
        this.foodStatic = foodStatic;
        this.foodPerPlayer = foodPerPlayer;
        snakes = new ConcurrentHashMap<>();
        field = new Field(sizex, sizey);
        emptyCells = Arrays.stream(field.getField()).flatMap(Arrays::stream).collect(Collectors.toList());
        foodCells = new ArrayList<>();
        aliveSnakes = 0;
    }

    public Model(int sizex, int sizey, List<Snake> snakesList, List<Coord> food, float foodStatic, float foodPerPlayer) {
        this(sizex, sizey, foodStatic, foodPerPlayer);
        Coord prevCoord = new Coord(0, 0);
        for (Snake snake : snakesList) {
            for (Coord coord : snake.getCoordList()) {
                emptyCells.remove(field.getCell(divideModulo(coord.getX() + prevCoord.getX(), sizex), divideModulo(coord.getY() + prevCoord.getY(), sizey)));
                prevCoord = new Coord(divideModulo(coord.getX() + prevCoord.getX(), sizex), divideModulo(coord.getY() + prevCoord.getY(), sizey));
            }
            if (snake.isAlive()) aliveSnakes++;
            snakes.put(snake.getSnakeId(), snake);
        }
        prevCoord = new Coord(0,0);
        for (Coord coord : food) {
            emptyCells.remove(field.getCell(divideModulo(coord.getX() + prevCoord.getX(), sizex), divideModulo(coord.getY() + prevCoord.getY(), sizey)));
            prevCoord = new Coord(divideModulo(coord.getX() + prevCoord.getX(), sizex), divideModulo(coord.getY() + prevCoord.getY(), sizey));
        }
    }

    private void createFood() {
        if (emptyCells.size() > foodStatic + foodPerPlayer * aliveSnakes) {
            Random rand = new Random();
            int numberOfElements = (int) (foodStatic + foodPerPlayer * aliveSnakes);
            for (int i = 0; i < numberOfElements; i++) {
                int randomIndex = rand.nextInt(emptyCells.size());
                Cell randomElement = emptyCells.get(randomIndex);
                randomElement.setFood();
                emptyCells.remove(randomIndex);
                foodCells.add(randomElement);
            }
        }
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

    public void addPlayer(int id) throws SnakeException {
        Snake snake = null;
        Cell centreCell = null;
        Cell secondCell = null;
        for (Cell cell : emptyCells) {
            boolean isBreak = false;
            Cell begin = field.getCell(divideModulo(cell.getCoord().getX() - 2, sizex), divideModulo((cell.getCoord().getY() - 2), sizey));
            for (int i = 0; i < 5; i++) {
                if (isBreak) {
                    break;
                }
                for (int j = 0; j < 5; j++) {
                    if (field.getCell(divideModulo(begin.getCoord().getX() + i, sizex), divideModulo(begin.getCoord().getY() + j, sizey)).getSnake() != null) {
                        isBreak = true;
                        break;
                    }
                }
            }
            if (isBreak)
                continue;
            if ((secondCell = field.getCell(cell.getCoord().getX(), divideModulo(cell.getCoord().getY() + 1, sizey))).isEmpty()) {
                snake = new Snake(id, MoveDirection.UP);
                secondCell.setSnake(snake);
                snake.getCoordList().add(secondCell.getCoord());
                snake.getCoordList().add(Coord.UP);
            } else if ((secondCell = field.getCell(cell.getCoord().getX(), divideModulo(cell.getCoord().getY() - 1, sizey))).isEmpty()) {
                snake = new Snake(id, MoveDirection.DOWN);
                secondCell.setSnake(snake);
                snake.getCoordList().add(secondCell.getCoord());
                snake.getCoordList().add(Coord.DOWN);
            } else if ((secondCell = field.getCell(divideModulo(cell.getCoord().getX() + 1, sizex), cell.getCoord().getY())).isEmpty()) {
                snake = new Snake(id, MoveDirection.LEFT);
                secondCell.setSnake(snake);
                snake.getCoordList().add(secondCell.getCoord());
                snake.getCoordList().add(Coord.LEFT);
            } else if ((secondCell = field.getCell(divideModulo(cell.getCoord().getX() - 1, sizex), cell.getCoord().getY())).isEmpty()) {
                snake = new Snake(id, MoveDirection.RIGHT);
                secondCell.setSnake(snake);
                snake.getCoordList().add(secondCell.getCoord());
                snake.getCoordList().add(Coord.RIGHT);
            }
            if (snake == null)
                break;
            centreCell = cell;
            centreCell.setSnake(snake);
            snakes.put(id, snake);
            break;
        }
        if (centreCell == null) {
            throw new SnakeException("no 5x5 available field");
        }
    }

    public void addViewer(String name, int id) {
        Snake viewer = new Snake(id, null);
        viewer.setAlive(false);
        snakes.put(id, viewer);
    }

    public void makeTurns() {
        for (Snake snake : snakes.values()) {
            MoveDirection direction = snake.getMoveDirection();
            System.out.println(direction);
            Coord headCoord = getHeadCell(snake).getCoord();
            if (direction == MoveDirection.UP) {
                if (headCoord.getY() == 0) {
                    System.out.println(divideModulo(headCoord.getY() - 1, sizey));
                }
                Cell cell = field.getCell(headCoord.getX(), divideModulo(headCoord.getY() - 1, sizey));
                move(snake, cell, direction);
            }
            if (direction == MoveDirection.DOWN) {
                Cell cell = field.getCell(headCoord.getX(), divideModulo(headCoord.getY() + 1, sizey));
                move(snake, cell, direction);
            }
            if (direction == MoveDirection.LEFT) {
                Cell cell = field.getCell(divideModulo(headCoord.getX() - 1, sizex), headCoord.getY());
                move(snake, cell, direction);
            }
            if (direction == MoveDirection.RIGHT) {
                Cell cell = field.getCell(divideModulo(headCoord.getX() + 1, sizex), headCoord.getY());
                move(snake, cell, direction);
            }
        }
        createFood();
    }


    private void move(Snake snake, Cell cell, MoveDirection direction) {
        if (cell.isEmpty()) {
            cell.setSnake(snake);
            addCellToList(snake, direction);
            Coord tail = snake.getCoordList().remove(0);
            field.getCell(tail).setEmpty();
            Coord preTail = snake.getCoordList().get(0);
            snake.getCoordList().set(0, new Coord((tail.getX() + preTail.getX()) % sizex, divideModulo(tail.getY() + preTail.getY(), sizey)));
            snake.setTurnDone(true);
            emptyCells.remove(cell);
        } else if (cell.isFood()) {
            cell.setSnake(snake);
            addCellToList(snake, direction);
            snake.addScore(1);
            snake.setTurnDone(true);
            foodCells.remove(cell);
        } else if (cell.getSnake() != null) {
            cell.getSnake().setScore(0);
            if (cell.getSnake().isTurnDone()) {
                for (Coord coord : cell.getSnake().getCoordList()) {
                    field.getCell(coord).setEmpty();
                    emptyCells.add(field.getCell(coord));
                }
            }
            for (Coord coord : snake.getCoordList()) {
                field.getCell(coord).setEmpty();
                emptyCells.add(field.getCell(coord));
            }
            snake.setScore(0);
        }
    }

    private Cell getHeadCell(Snake snake) {
        List<Coord> coords = snake.getCoordList();
        Coord tail = coords.get(0);
        Cell head = field.getCell(tail);
        for (int i = 1; i < coords.size(); i++) {
            head = field.getCell(divideModulo(head.getCoord().getX() + coords.get(i).getX(), sizex), divideModulo(head.getCoord().getY() + coords.get(i).getY(), sizey));
        }
        return head;
    }

    private void addCellToList(Snake snake, MoveDirection direction) {
        if (direction == MoveDirection.UP)
            snake.getCoordList().add(Coord.UP);
        if (direction == MoveDirection.DOWN)
            snake.getCoordList().add(Coord.DOWN);
        if (direction == MoveDirection.RIGHT)
            snake.getCoordList().add(Coord.RIGHT);
        if (direction == MoveDirection.LEFT)
            snake.getCoordList().add(Coord.LEFT);
    }

    public static int divideModulo(int dividend, int divider) {
        if (dividend < 0) return dividend % divider + divider;
        return dividend % divider;
    }

}
