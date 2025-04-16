package com.ktae23;

public class HolidayClientException extends RuntimeException {

    public HolidayClientException(String message) {
        super(message);
    }

    public HolidayClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public HolidayClientException(Throwable cause) {
        super(cause);
    }
}
