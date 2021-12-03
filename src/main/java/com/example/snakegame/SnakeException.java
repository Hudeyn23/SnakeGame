package com.example.snakegame;

public class SnakeException extends Exception {
    public SnakeException() {
        super();
    }

    public SnakeException(String message) {
        super(message);
    }

    public SnakeException(String message, Throwable cause) {
        super(message, cause);
    }

    public SnakeException(Throwable cause) {
        super(cause);
    }

    protected SnakeException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
