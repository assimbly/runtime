package org.assimbly.util.exception;

public class InvalidFileException extends RuntimeException {

    public InvalidFileException() {}

    public InvalidFileException(String s) {
        super(s);
    }

    public InvalidFileException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public InvalidFileException(Throwable throwable) {
        super(throwable);
    }

}
