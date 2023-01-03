package org.assimbly.dil.validation.expressions;

import org.apache.camel.language.simple.SimpleLanguage;
import org.apache.camel.language.simple.types.SimpleIllegalSyntaxException;
import org.assimbly.dil.validation.beans.Expression;
import org.assimbly.dil.validation.Validator;
import org.assimbly.util.error.ValidationErrorMessage;

public class SimpleValidator implements Validator {


    @Override
    public ValidationErrorMessage validate(Expression expression){
        try {
            SimpleLanguage.simple(expression.getExpression());
        } catch (SimpleIllegalSyntaxException e) {
            if(expression.getName() == null) {
                return new ValidationErrorMessage(e.getMessage());
            }

            return new ValidationErrorMessage("[" + expression.getName() + "]: " + e.getMessage());
        }

        return null;
    }

}
