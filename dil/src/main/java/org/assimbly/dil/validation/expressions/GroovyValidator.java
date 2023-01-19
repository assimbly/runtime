package org.assimbly.dil.validation.expressions;

import org.assimbly.dil.validation.beans.Expression;
import org.assimbly.dil.validation.Validator;
import org.assimbly.util.error.ValidationErrorMessage;
import org.springframework.scripting.groovy.GroovyScriptEvaluator;
import org.springframework.scripting.support.StaticScriptSource;

public class GroovyValidator implements Validator {

    @Override
    public ValidationErrorMessage validate(Expression expression){

        if(expression.getName() == null || expression.getName().isEmpty())
            return new ValidationErrorMessage("Header name cannot be empty");
        else if(expression.getExpression() == null || expression.getExpression().isEmpty())
            return new ValidationErrorMessage("Expression cannot be empty");
        else {
            try {
                GroovyScriptEvaluator groovyScriptEvaluator = new GroovyScriptEvaluator();
                groovyScriptEvaluator.evaluate(new StaticScriptSource(expression.getExpression()));
            } catch (Exception e) {
                return new ValidationErrorMessage("[" + expression.getName() + "]: " + e.getMessage());
            }
        }

        return null;
    }
}
