package org.assimbly.dil.validation;

import org.apache.camel.catalog.DefaultCamelCatalog;
import org.apache.camel.catalog.LanguageValidationResult;
import org.assimbly.dil.validation.beans.ValidationExpression;

import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XPathCompiler;

import java.util.List;

public class ExpressionsValidator {

    private final DefaultCamelCatalog catalog = new DefaultCamelCatalog();

    public List<ValidationExpression> validate(List<ValidationExpression> expressions, boolean isPredicate) {

        for (ValidationExpression expression : expressions) {
            validateExpression(expression, isPredicate);
        }

        return expressions;

    }

    private void validateExpression(ValidationExpression expression, boolean isPredicate) {

        if (isBlank(expression.getName())) {
            expression.setValid(false);
            expression.setMessage("Header name cannot be empty");
            return;
        }

        if (isBlank(expression.getExpression())) {
            expression.setValid(false);
            expression.setMessage("Expression cannot be empty");
            return;
        }

        if (expression.getLanguage().equalsIgnoreCase("xpath")) {
            String result = validateXpathExpression(expression.getExpression());
            if(result.equalsIgnoreCase("valid")){
                expression.setValid(true);
            }else{
                expression.setValid(false);
                expression.setMessage(result);
            }
            return;
        }

        LanguageValidationResult result = isPredicate
                ? catalog.validateLanguagePredicate(ClassLoader.getSystemClassLoader(), expression.getLanguage(), expression.getExpression())
                : catalog.validateLanguageExpression(ClassLoader.getSystemClassLoader(), expression.getLanguage(), expression.getExpression());

        expression.setValid(result.isSuccess());
        if (!result.isSuccess()) {
            expression.setMessage(result.getError());
        }

    }

    private boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    private String validateXpathExpression(String expression){

        try {
            Processor proc = new Processor(false);
            XPathCompiler xpath = proc.newXPathCompiler();
            xpath.compile(expression);
        } catch (SaxonApiException e) {
            return e.getMessage();
        }

        return "valid";

    }

}