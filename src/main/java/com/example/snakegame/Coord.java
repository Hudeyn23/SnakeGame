package com.example.snakegame;

public final class Coord {
    public final static Coord UP = new Coord(0, -1);
    public final static Coord DOWN = new Coord(0, 1);
    public final static Coord LEFT = new Coord(-1, 0);
    public final static Coord RIGHT = new Coord(1, 0);
    private final int x;
    private final int y;

    public Coord(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }
}
