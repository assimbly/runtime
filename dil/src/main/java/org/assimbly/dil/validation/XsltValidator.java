package org.assimbly.dil.validation;

import net.sf.saxon.lib.StandardURIResolver;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XsltCompiler;
import org.assimbly.dil.validation.saxon.SaxonConfiguration;
import org.assimbly.util.error.ValidationErrorMessage;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;
import java.util.ArrayList;
import java.util.List;

public class XsltValidator {

    private static final String BASE = "";

    private final Processor saxonProcessor;

    public XsltValidator() {
        this.saxonProcessor = new Processor(new SaxonConfiguration());
    }

    public List<ValidationErrorMessage> validate(String url, String xsltBody) {
        XsltCompiler xsltCompiler = saxonProcessor.newXsltCompiler();
        ValidationErrorListener errorListener = new ValidationErrorListener();
        xsltCompiler.setErrorListener(errorListener);

        try {
            Source source = getXsltSource(url, xsltBody);
            xsltCompiler.compile(source);
        } catch (TransformerException | SaxonApiException e) {
            errorListener.registerException(e);
        }

        return errorListener.errors;
    }

    private Source getXsltSource(String url, String xsltBody) throws TransformerException {
        if(url != null) {
            URIResolver uriResolver = new StandardURIResolver();
            return uriResolver.resolve(url, BASE);
        } else {
            if(xsltBody == null) {
                return new StreamSource(new java.io.StringReader(""));
            }

            return new StreamSource(new java.io.StringReader(xsltBody));
        }
    }

    private class ValidationErrorListener implements ErrorListener {

        protected final List<ValidationErrorMessage> errors = new ArrayList<>();

        public void registerException(Exception exception) {
            errors.add(new ValidationErrorMessage(exception.getMessage()));
        }

        @Override
        public void warning(TransformerException e) {
            registerException(e);
        }

        @Override
        public void error(TransformerException e) {
            registerException(e);
        }

        @Override
        public void fatalError(TransformerException e) {
            registerException(e);
        }
    }

}