package org.assimbly.dil.validation.beans;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Expression {

    private String id;

    private String name;
    private String expressionValue;
    private String expressionType;
    private String nextNode;

    public Expression() { }

    public Expression(String name, String expressionValue, String expressionType, String nextNode) {
        this.name = name;
        this.expressionValue = expressionValue;
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
        return expressionValue;
    }

    public void setExpression(String expressionValue) {
        this.expressionValue = expressionValue;}

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
}
