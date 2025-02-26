package org.assimbly.dil.validation.expressions;

import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XPathCompiler;
import org.assimbly.dil.validation.Validator;
import org.assimbly.dil.validation.beans.Expression;
import org.assimbly.util.error.ValidationErrorMessage;

public class XPathValidator implements Validator {

    @Override
    public ValidationErrorMessage validate(Expression expression){
        try {
            Processor proc = new Processor(false);
            XPathCompiler xpath = proc.newXPathCompiler();

            xpath.compile(expression.getExpression());
        } catch (SaxonApiException e) {
            if(expression.getName() == null) {
                return new ValidationErrorMessage(e.getMessage());
            }

            return new ValidationErrorMessage("[" + expression.getName() + "]: " + e.getMessage());
        }

        return null;
    }
}

