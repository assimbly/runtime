package org.assimbly.util.exception;

public class EnvironmentException extends RuntimeException{

    public EnvironmentException() {}

    public EnvironmentException(String s) {
        super(s);
    }

    public EnvironmentException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public EnvironmentException(Throwable throwable) {
        super(throwable);
    }
}
