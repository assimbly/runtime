package org.assimbly.dil.validation;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.ExchangeHelper;
import org.assimbly.dil.blocks.beans.UuidExtensionFunction;
import org.assimbly.dil.validation.beans.script.EvaluationRequest;
import org.assimbly.dil.validation.beans.script.EvaluationResponse;
import org.assimbly.dil.validation.beans.script.ExchangeDto;
import org.assimbly.dil.validation.beans.script.ScriptDto;
import org.assimbly.dil.validation.scripts.ExchangeMarshaller;
import org.assimbly.sandbox.executors.GroovySandboxExecutor;
import org.codehaus.groovy.control.CompilationFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.Map;

/**
 * Runs a script once on a sample message, the way the Script Step's kamelet (kamelets/{language}-script.kamelet.yaml)
 * runs it in a Flow, and answers with the resulting body or the error.
 */
public class ScriptValidator {

    protected Logger log = LoggerFactory.getLogger(getClass());

    /** Script Step languages that the kamelet runs as a Camel language with setBody, by the Camel language name. */
    private static final Map<String, String> CAMEL_LANGUAGES = Map.of(
            "datasonnet", "datasonnet",
            "jactl", "jactl",
            "javascript", "js",
            "js", "js",
            "python", "python3",
            "simple", "simple"
    );

    /** The endpoints and template headers of the jslt-script and xslt-script kamelets. */
    private static final String JSLT_URI = "jslt:script?allowTemplateFromHeader=true";
    private static final String JSLT_HEADER = "CamelJsltString";
    private static final String XSLT_URI = "xslt-saxon:xslt/script-placeholder.xsl?allowTemplateFromHeader=true&saxonExtensionFunctions=#uuid-function";
    private static final String XSLT_HEADER = "CamelXsltStylesheet";

    public EvaluationResponse validate(EvaluationRequest evaluationRequest) {

        if (evaluationRequest == null) {
            return createBadRequestResponse(null,"Empty requests aren't allowed");
        }
        if (evaluationRequest.getScript() == null || evaluationRequest.getExchange() == null) {
            return createBadRequestResponse(evaluationRequest.getExchange(),"Empty requests aren't allowed");
        }

        ScriptDto scriptDto = evaluationRequest.getScript();
        ExchangeDto exchangeDto = evaluationRequest.getExchange();

        if (scriptDto.getScript() == null) {
            return createBadRequestResponse(exchangeDto, "Script body cannot be null");
        }

        String language = scriptDto.getLanguage() == null ? "" : scriptDto.getLanguage().toLowerCase(Locale.ROOT);
        if (!language.equals("groovy") && !language.equals("jslt") && !language.equals("xslt") && !CAMEL_LANGUAGES.containsKey(language)) {
            return createBadRequestResponse(exchangeDto, "Unsupported scripting language: '" + scriptDto.getLanguage() + "'");
        }

        if (exchangeDto.getBody() == null && (language.equals("jslt") || language.equals("xslt"))) {
            String format = language.equals("jslt") ? "JSON" : "XML";
            return createBadRequestResponse(exchangeDto, language.toUpperCase(Locale.ROOT) + " transforms the message body, and there is none: give a sample body in " + format + " to try the script.");
        }

        try (CamelContext context = new DefaultCamelContext()) {
            context.getRegistry().bind("uuid-function", new UuidExtensionFunction());
            context.start();

            Exchange exchange = ExchangeMarshaller.unmarshall(exchangeDto, context);

            switch (language) {
                case "groovy" -> GroovySandboxExecutor.execute(scriptDto.getScript(), exchange);
                case "jslt" -> transform(context, exchange, JSLT_URI, JSLT_HEADER, scriptDto.getScript());
                case "xslt" -> transform(context, exchange, XSLT_URI, XSLT_HEADER, scriptDto.getScript());
                default -> evaluate(context, exchange, CAMEL_LANGUAGES.get(language), scriptDto.getScript());
            }

            return createOKRequestResponse(ExchangeMarshaller.marshall(exchange), exchange.getIn().getBody(String.class));

        } catch (Exception e) {
            log.warn("Invalid {} script: {}", language, e.getMessage());
            return createBadRequestResponse(exchangeDto, describe(e));
        }
    }

    /** What the kamelet's setBody does: the script's result becomes the body. */
    private void evaluate(CamelContext context, Exchange exchange, String camelLanguage, String script) {
        Expression expression = context.resolveLanguage(camelLanguage).createExpression(script);
        expression.init(context);
        exchange.getIn().setBody(expression.evaluate(exchange, Object.class));
        if (exchange.getException() != null) {
            throw new IllegalStateException(exchange.getException().getMessage(), exchange.getException());
        }
    }

    /** What the jslt-script and xslt-script kamelets do: send the message to the component with the script as template header. */
    private void transform(CamelContext context, Exchange exchange, String uri, String templateHeader, String script) throws Exception {
        exchange.getIn().setHeader(templateHeader, script);
        try (ProducerTemplate template = context.createProducerTemplate()) {
            template.send(uri, exchange);
        }
        // The XSLT component answers on the OUT message; the next Step of a Flow reads it as IN.
        ExchangeHelper.prepareOutToIn(exchange);
        exchange.getIn().removeHeader(templateHeader);
        if (exchange.getException() != null) {
            throw exchange.getException();
        }
    }

    /** The error as the script's author needs it: what the language reported, not the layers of Camel around it. */
    private static String describe(Exception e) {
        Throwable root = e;
        while (root.getCause() != null && root.getCause() != root) {
            if (root instanceof CompilationFailedException) {
                break;
            }
            root = root.getCause();
        }
        String message = root.getMessage();
        if (message == null || message.isBlank()) {
            message = root.getClass().getSimpleName();
        }
        if (root instanceof CompilationFailedException) {
            return "Syntax Error: " + message;
        }
        if (root instanceof SecurityException) {
            return "Security Error: " + message;
        }
        return message;
    }

    private EvaluationResponse createOKRequestResponse(ExchangeDto exchange, String message) {
        return new EvaluationResponse(exchange, message, 1);
    }

    private EvaluationResponse createBadRequestResponse(ExchangeDto exchange, String message) {
        return new EvaluationResponse(exchange, message, -1);
    }
}
