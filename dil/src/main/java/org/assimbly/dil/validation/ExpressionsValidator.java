package org.assimbly.dil.validation;

import org.assimbly.dil.validation.beans.Expression;
import org.assimbly.dil.validation.expressions.*;
import org.assimbly.util.error.ValidationErrorMessage;

import java.util.ArrayList;
import java.util.List;

public class ExpressionsValidator {

    private static final ValidationErrorMessage EMPTY_EXPRESSION_ERROR = new ValidationErrorMessage("Empty expressions aren't allowed!");

    private JsonPathValidator jsonPathValidator = new JsonPathValidator();
    private SimpleValidator simpleValidator = new SimpleValidator();
    private XPathValidator xpathValidator = new XPathValidator();
    private ConstantValidator constantValidator = new ConstantValidator();
    private GroovyValidator groovyValidator = new GroovyValidator();

    public List<ValidationErrorMessage> validate(List<Expression> expressions) {

        List<ValidationErrorMessage> validationErrors = new ArrayList<>();

        if (expressions.isEmpty()) {
            validationErrors.add(EMPTY_EXPRESSION_ERROR);
            return validationErrors;
        }

        for (Expression expression : expressions) {
            ValidationErrorMessage error;

            switch(expression.getExpressionType()) {
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
                    error = simpleValidator.validate(expression);
                    break;
                case "xpath":
                    error = xpathValidator.validate(expression);
                    break;
                default:
                    throw new RuntimeException("Could not validate the type of expression submitted.");
            }

            if (error != null) {
                validationErrors.add(error);
            }
        }

        if (validationErrors.isEmpty())
            return null;

        return validationErrors;
    }
}
