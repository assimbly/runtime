package org.assimbly.dil.validation;

import org.assimbly.dil.validation.beans.Expression;
import org.assimbly.util.error.ValidationErrorMessage;

public interface Validator {

    ValidationErrorMessage validate(Expression expression);
}
