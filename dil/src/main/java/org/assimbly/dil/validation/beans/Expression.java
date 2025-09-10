package org.assimbly.dil.validation.beans;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Expression {

    private String id;

    private String name;
    private String expression;
    private String expressionType;
    private String nextNode;
    private boolean valid;
    private String message;

    public Expression() { }

    public Expression(String name, String expression, String expressionType, String nextNode) {
        this.name = name;
        this.expression = expression;
        this.expressionType = expressionType;
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

    public String getExpressionType() {
        return expressionType;
    }

    public void setExpressionType(String expressionType) {
        this.expressionType = expressionType;
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
