package org.assimbly.dil.validation.expressions;

import com.jayway.jsonpath.InvalidPathException;
import com.jayway.jsonpath.JsonPath;
import org.apache.camel.jsonpath.JsonPathLanguage;
import org.assimbly.dil.validation.beans.Expression;
import org.assimbly.dil.validation.Validator;
import org.assimbly.util.error.ValidationErrorMessage;

public class JsonPathValidator implements Validator {

    @Override
    public ValidationErrorMessage validate(Expression expression){
        try {
            JsonPath.compile(expression.getExpression());
        } catch(InvalidPathException e) {
            if(expression.getName() == null) {
                return new ValidationErrorMessage(e.getMessage());
            }

            return new ValidationErrorMessage("[" + expression.getName() + "]: " + e.getMessage());
        }

        return null;
    }
}
