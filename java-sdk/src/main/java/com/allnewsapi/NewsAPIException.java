package com.allnewsapi;

/**
 * Custom exception for NewsAPI errors.
 * Extends RuntimeException so callers are not forced to catch it.
 */
public class NewsAPIException extends RuntimeException {
    private final int statusCode;

    /**
     * Create a new NewsAPIException.
     *
     * @param statusCode the HTTP status code
     * @param message    the error message
     */
    public NewsAPIException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    /**
     * Get the HTTP status code associated with this error.
     *
     * @return the HTTP status code
     */
    public int getStatusCode() {
        return statusCode;
    }
}
