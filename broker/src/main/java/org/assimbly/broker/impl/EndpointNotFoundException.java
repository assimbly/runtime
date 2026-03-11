package org.assimbly.broker.impl;

import java.io.Serial;

public class EndpointNotFoundException extends Exception {

    @Serial
    private static final long serialVersionUID = 4328743;

    public EndpointNotFoundException(String errorMessage) {
        super(errorMessage);
    }

}