package com.cineflow.service;

import lombok.Getter;

@Getter
public class TmdbClientException extends RuntimeException {

    public enum ErrorType {
        CONFIGURATION,
        NETWORK,
        UPSTREAM
    }

    private final ErrorType errorType;

    public TmdbClientException(ErrorType errorType, String message) {
        super(message);
        this.errorType = errorType;
    }

    public TmdbClientException(ErrorType errorType, String message, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
    }

    public static TmdbClientException configuration(String message) {
        return new TmdbClientException(ErrorType.CONFIGURATION, message);
    }

    public static TmdbClientException network(String message, Throwable cause) {
        return new TmdbClientException(ErrorType.NETWORK, message, cause);
    }

    public static TmdbClientException upstream(String message) {
        return new TmdbClientException(ErrorType.UPSTREAM, message);
    }

    public boolean isConfigurationError() {
        return errorType == ErrorType.CONFIGURATION;
    }

    public boolean isNetworkError() {
        return errorType == ErrorType.NETWORK;
    }
}
