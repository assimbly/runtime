package org.assimbly.broker.impl;

public class EndpointNotFoundException extends Exception {
    public EndpointNotFoundException(String errorMessage) {
        super(errorMessage);
    }
}