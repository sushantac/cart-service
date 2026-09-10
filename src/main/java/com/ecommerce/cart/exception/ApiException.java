package com.ecommerce.cart.exception;

public class ApiException extends RuntimeException {

    private final int status;
    private final String error;

    public ApiException(int status, String error, String message) {
        super(message);
        this.status = status;
        this.error = error;
    }

    public ApiException(String error, String message) {
        this(400, error, message);
    }

    public int getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }
}
