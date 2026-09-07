package com.example.bookingsystem.exception;

public class UnauthorizedBookingException extends RuntimeException {
    public UnauthorizedBookingException(String message) {
        super(message);
    }
}

