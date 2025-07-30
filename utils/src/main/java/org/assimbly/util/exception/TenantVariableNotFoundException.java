package org.assimbly.util.exception;

public class TenantVariableNotFoundException extends RuntimeException {

    public TenantVariableNotFoundException() {}

    public TenantVariableNotFoundException(String s) {
        super(s);
    }

    public TenantVariableNotFoundException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public TenantVariableNotFoundException(Throwable throwable) {
        super(throwable);
    }

}
