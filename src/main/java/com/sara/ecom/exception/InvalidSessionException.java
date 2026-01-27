package com.sara.ecom.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when the JWT is valid but the user record is missing (e.g. deleted after token was issued).
 * Results in 401 so the client clears the token and redirects to login.
 */
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class InvalidSessionException extends RuntimeException {

    public InvalidSessionException(String message) {
        super(message);
    }
}
