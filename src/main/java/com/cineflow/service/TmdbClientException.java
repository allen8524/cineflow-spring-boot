package com.cineflow.service;

public class TmdbClientException extends RuntimeException {

    public TmdbClientException(String message) {
        super(message);
    }

    public TmdbClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
