package org.assimbly.dil.validation;

import org.assimbly.dil.validation.beans.ValidationExpression;
import org.assimbly.util.error.ValidationErrorMessage;

public interface Validator {

    ValidationErrorMessage validate(ValidationExpression expression);
}
