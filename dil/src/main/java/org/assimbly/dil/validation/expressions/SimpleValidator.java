package org.assimbly.dil.validation.expressions;

import org.apache.camel.catalog.DefaultCamelCatalog;
import org.apache.camel.catalog.LanguageValidationResult;
import org.assimbly.dil.validation.beans.Expression;
import org.assimbly.dil.validation.Validator;
import org.assimbly.util.error.ValidationErrorMessage;

public class SimpleValidator implements Validator {

    @Override
    public ValidationErrorMessage validate(Expression expression){

        DefaultCamelCatalog catalog = new DefaultCamelCatalog();
        LanguageValidationResult result = catalog.validateLanguageExpression(ClassLoader.getSystemClassLoader(), "simple", expression.getExpression());

        if(result.isSuccess())
            return null;
        else{
            return new ValidationErrorMessage("[" + expression.getName() + "]: " + result.getError());
        }

    }

    public ValidationErrorMessage validatePredicate(Expression expression){

        DefaultCamelCatalog catalog = new DefaultCamelCatalog();
        LanguageValidationResult result = catalog.validateLanguagePredicate(ClassLoader.getSystemClassLoader(), "simple", expression.getExpression());

        if(result.isSuccess())
            return null;
        else{
            return new ValidationErrorMessage("[" + expression.getName() + "]: " + result.getError());
        }

    }

}
