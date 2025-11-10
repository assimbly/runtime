package org.assimbly.dil.validation.beans;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ValidationExpression {

    private String id;

    private String name;
    private String expression;
    private String language;
    private String nextNode;
    private boolean valid;
    private String message;

    public ValidationExpression() { }

    public ValidationExpression(String name, String expression, String language, String nextNode) {
        this.name = name;
        this.expression = expression;
        this.language = language;
        this.nextNode = nextNode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public String getNextNode() {
        return nextNode;
    }

    public void setNextNode(String nextNode) {
        this.nextNode = nextNode;
    }

    @JsonProperty("_id")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}