package org.assimbly.dil.validation;

import org.assimbly.dil.validation.beans.Expression;
import org.assimbly.dil.validation.expressions.*;
import org.assimbly.util.error.ValidationErrorMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ExpressionsValidator {

    private static final ValidationErrorMessage EMPTY_EXPRESSION_ERROR = new ValidationErrorMessage("Empty expressions aren't allowed!");

    List<ValidationErrorMessage> validationErrors = new ArrayList<>();

    private final JsonPathValidator jsonPathValidator = new JsonPathValidator();
    private final SimpleValidator simpleValidator = new SimpleValidator();
    private final XPathValidator xpathValidator = new XPathValidator();
    private final ConstantValidator constantValidator = new ConstantValidator();
    private final GroovyValidator groovyValidator = new GroovyValidator();

    public List<ValidationErrorMessage> validate(List<Expression> expressions, boolean isPredicate) {

        isExpressionsEmpty(expressions);

        checkExpressions(expressions,isPredicate);

        if (validationErrors.isEmpty())
            return Collections.emptyList();

        return validationErrors;
    }

    private void checkExpressions(List<Expression> expressions, boolean isPredicate) {

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
                    if(isPredicate){
                        error = simpleValidator.validatePredicate(expression);
                    }else{
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
                validationErrors.add(error);
            }

        }

    }

    private void  isExpressionsEmpty(List<Expression> expressions) {
        if (expressions.isEmpty()) {
            validationErrors.add(EMPTY_EXPRESSION_ERROR);
        }
    }


}
