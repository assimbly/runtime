package org.assimbly.dil.validation.beans;

public class Regex {

    private String expression;

    public Regex() {}

    public Regex(String expression) {
        this.expression = expression;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }
}
