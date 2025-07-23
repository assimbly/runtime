package org.assimbly.dil.validation.expressions;

import org.apache.camel.catalog.DefaultCamelCatalog;
import org.apache.camel.catalog.LanguageValidationResult;
import org.apache.camel.language.constant.ConstantLanguage;
import org.assimbly.dil.validation.beans.Expression;
import org.assimbly.dil.validation.Validator;
import org.assimbly.util.error.ValidationErrorMessage;

public class ConstantValidator implements Validator {

    @Override
    public ValidationErrorMessage validate(Expression expression){

        if(expression.getName() == null || expression.getName().isEmpty())
            return new ValidationErrorMessage("Header name cannot be empty");
        else if(expression.getExpression() == null || expression.getExpression().isEmpty())
            return new ValidationErrorMessage("Expression cannot be empty");
        else {

            DefaultCamelCatalog catalog = new DefaultCamelCatalog();
            LanguageValidationResult result = catalog.validateLanguageExpression(ClassLoader.getSystemClassLoader(), "constant", expression.getExpression());

            if(result.isSuccess())
                return null;
            else{
                return new ValidationErrorMessage(result.getError());
            }

        }

    }
}
