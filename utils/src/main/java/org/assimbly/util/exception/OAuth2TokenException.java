package org.assimbly.util.exception;

public class OAuth2TokenException extends RuntimeException {

    public OAuth2TokenException() {
    }

    public OAuth2TokenException(String s) {
        super(s);
    }

    public OAuth2TokenException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public OAuth2TokenException(Throwable throwable) {
        super(throwable);
    }
}
