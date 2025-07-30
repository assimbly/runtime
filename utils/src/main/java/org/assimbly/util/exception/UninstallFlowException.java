package org.assimbly.util.exception;

public class UninstallFlowException extends RuntimeException {

    public UninstallFlowException() {}

    public UninstallFlowException(String s) {
        super(s);
    }

    public UninstallFlowException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public UninstallFlowException(Throwable throwable) {
        super(throwable);
    }
}
