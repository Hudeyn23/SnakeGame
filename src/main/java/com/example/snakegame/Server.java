package com.example.snakegame;

import java.net.ServerSocket;

public class Server extends Thread {
    private int port;

    public Server(int port) {
        this.port = port;
    }

    @Override
    public void run() {
        ServerSocket serverSocket = new ServerSocket(port);

    }
}
