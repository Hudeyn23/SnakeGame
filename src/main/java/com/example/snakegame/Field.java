package com.example.snakegame;

public class Field {
    private Cell[][] field;
    private int sizeX;
    private int sizeY;

    public Cell[][] getField() {
        return field;
    }

    public Field(int sizeX, int sizeY) {
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        field = new Cell[sizeX][sizeY];
        for (int x = 0; x < sizeX; x++) {
            for (int y = 0; y < sizeY; y++) {
                field[x][y] = new Cell(x,y);
            }
        }
    }

    public Cell getCell(int x, int y) {
        return field[x][y];
    }
    public Cell getCell(Coord coord) {
        return field[coord.getX()][coord.getY()];
    }
}
