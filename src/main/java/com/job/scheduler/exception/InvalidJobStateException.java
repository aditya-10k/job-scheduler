package com.job.scheduler.exception;

public class InvalidJobStateException extends RuntimeException {

    public InvalidJobStateException(String message) {
        super(message);
    }
}
