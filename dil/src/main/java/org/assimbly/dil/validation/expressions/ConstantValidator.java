package org.assimbly.dil.validation.expressions;

import org.apache.camel.language.constant.ConstantLanguage;
import org.assimbly.dil.validation.Validator;
import org.assimbly.dil.validation.beans.Expression;
import org.assimbly.util.error.ValidationErrorMessage;

public class ConstantValidator implements Validator {

    @Override
    public ValidationErrorMessage validate(Expression expression){

        try {
            ConstantLanguage.constant(expression.getExpression());
        } catch (Exception e) {
            if(expression.getName() == null) {
                return new ValidationErrorMessage(e.getMessage());
            }

            return new ValidationErrorMessage("[" + expression.getName() + "]: " + e.getMessage());
        }

        return null;
    }
}
