package org.assimbly.dil.validation;

import org.assimbly.dil.validation.beans.Expression;
import org.assimbly.dil.validation.expressions.*;
import org.assimbly.util.error.ValidationErrorMessage;


import java.util.List;

public class ExpressionsValidator {

    private final JsonPathValidator jsonPathValidator = new JsonPathValidator();
    private final SimpleValidator simpleValidator = new SimpleValidator();
    private final XPathValidator xpathValidator = new XPathValidator();
    private final ConstantValidator constantValidator = new ConstantValidator();
    private final GroovyValidator groovyValidator = new GroovyValidator();

    public List<Expression> validate(List<Expression> expressions, boolean isPredicate) {

        for (Expression expression : expressions) {

            ValidationErrorMessage error;

            switch (expression.getExpressionType()) {
                case "constant":
                    error = constantValidator.validate(expression);
                    break;
                case "groovy":
                    error = groovyValidator.validate(expression);
                    break;
                case "jsonpath":
                    error = jsonPathValidator.validate(expression);
                    break;
                case "simple":
                    if (isPredicate) {
                        error = simpleValidator.validatePredicate(expression);
                    } else {
                        error = simpleValidator.validate(expression);
                    }
                    break;
                case "xpath":
                    error = xpathValidator.validate(expression);
                    break;
                default:
                    throw new RuntimeException("Could not validate the type of expression submitted.");
            }

            if (error != null) {
                expression.setValid(false);
                expression.setMessage(error.getError());
            } else {
                expression.setValid(true);
            }
        }

        return expressions;

    }

}
