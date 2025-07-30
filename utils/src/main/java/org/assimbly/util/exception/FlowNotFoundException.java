package org.assimbly.util.exception;

public class FlowNotFoundException extends RuntimeException {

    public FlowNotFoundException() {}

    public FlowNotFoundException(String s) {
        super(s);
    }

    public FlowNotFoundException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public FlowNotFoundException(Throwable throwable) {
        super(throwable);
    }
}
