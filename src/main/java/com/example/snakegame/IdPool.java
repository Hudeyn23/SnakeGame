package com.example.snakegame;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class IdPool {
    private int size;
    private int initSize;
    private Queue<Integer> idQueue;

    public IdPool(int size) {
        this.size = size;
        initSize = size;
        idQueue = new ConcurrentLinkedQueue<>();
        for (int i = 0; i < size; i++) {
            idQueue.add(i);
        }
    }

    public int getId() {
        if (idQueue.isEmpty()) {
            for (int i = size; i < size + initSize; i++) {
                idQueue.add(i);
            }
        }
        return idQueue.remove();
    }

    public void freeId(int id) {
        idQueue.add(id);
    }


}
