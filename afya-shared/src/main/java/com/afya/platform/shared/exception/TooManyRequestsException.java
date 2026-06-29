package com.afya.platform.shared.exception;

import org.springframework.http.HttpStatus;

/**
 * Levée lorsqu'un quota de requêtes est dépassé (HTTP 429), par exemple en cas de
 * trop nombreuses tentatives de connexion infructueuses (protection anti-force brute).
 */
public class TooManyRequestsException extends DomainException {

    public TooManyRequestsException(String message) {
        super(HttpStatus.TOO_MANY_REQUESTS, message);
    }
}
