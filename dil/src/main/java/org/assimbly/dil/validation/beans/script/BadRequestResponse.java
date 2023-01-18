package org.assimbly.dil.validation.beans.script;

public class BadRequestResponse {

    private String message;

    @SuppressWarnings("unused")
    protected BadRequestResponse() {}

    public BadRequestResponse(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    @SuppressWarnings("unused")
    protected void setMessage(String message) {
        this.message = message;
    }
}
