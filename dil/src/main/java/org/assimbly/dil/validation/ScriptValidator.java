package org.assimbly.dil.validation;

import org.apache.camel.Exchange;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.language.groovy.GroovyExpression;
import org.apache.camel.model.language.JavaScriptExpression;
import org.assimbly.dil.validation.beans.script.EvaluationRequest;
import org.assimbly.dil.validation.beans.script.EvaluationResponse;
import org.assimbly.dil.validation.beans.script.ExchangeDto;
import org.assimbly.dil.validation.beans.script.ScriptDto;
import org.assimbly.dil.validation.scripts.ExchangeMarshaller;
import org.assimbly.sandbox.executors.GroovySandboxExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class ScriptValidator {

    protected Logger log = LoggerFactory.getLogger(getClass());

    public EvaluationResponse validate(EvaluationRequest evaluationRequest) {

        if (evaluationRequest == null) {
            return createBadRequestResponse(null,"Empty requests aren't allowed");
        }
        if (evaluationRequest.getScript() == null || evaluationRequest.getExchange() == null) {
            return createBadRequestResponse(evaluationRequest.getExchange(),"Empty requests aren't allowed");
        }

        ScriptDto scriptDto = evaluationRequest.getScript();

        if (scriptDto.getScript() == null) {
            return createBadRequestResponse(evaluationRequest.getExchange(), "Script body cannot be null");
        }

        try {
            switch(scriptDto.getLanguage()) {
                case "js":
                    return validateJavaScript(evaluationRequest.getExchange(), scriptDto.getScript());
                case "groovy":
                    if(scriptDto.isStrictSecureMode()) {
                        return validateStrictGroovyScript(evaluationRequest.getExchange(), scriptDto.getScript());
                    } else {
                        return validateGroovyScript(evaluationRequest.getExchange(), scriptDto.getScript());
                    }
                default:
                    return createBadRequestResponse(evaluationRequest.getExchange(), "Unsupported scripting language");
            }
        } catch (Exception e) {
            log.error("Invalid script: '", e);
            return createBadRequestResponse(evaluationRequest.getExchange(), "Invalid script: '" + e.getMessage() + "'");
        }
    }

    private EvaluationResponse validateStrictGroovyScript(ExchangeDto exchangeDto, String script) {
        try {
            // 1. Unmarshall the test data
            Exchange exchangeRequest = ExchangeMarshaller.unmarshall(exchangeDto);

            // 2. Use the dedicated Sandbox Executor logic
            // We call a modified version or the same executor to ensure
            // the ClassLoader isolation and SecurityManager are active.
            GroovySandboxExecutor.execute(script, exchangeRequest);

            // 3. Marshall the result back
            ExchangeDto exchangeDtoResponse = ExchangeMarshaller.marshall(exchangeRequest);

            // Use the script's body or a specific variable as the 'response' string
            String scriptOutput = String.valueOf(exchangeRequest.getIn().getBody());

            return createOKRequestResponse(exchangeDtoResponse, scriptOutput);

        } catch (SecurityException | RuntimeCamelException e) {
            // This catches the BLACKLIST_PATTERN or SecurityManager violations
            log.error("Sandbox Security Violation: ", e);
            return createBadRequestResponse(exchangeDto, "Security Error: " + e.getMessage());
        } catch (org.codehaus.groovy.control.CompilationFailedException e) {
            // This catches syntax errors
            log.error("Groovy Syntax Error: ", e);
            return createBadRequestResponse(exchangeDto, "Syntax Error: " + e.getMessage());
        } catch (Exception e) {
            log.error("Execution error during validation: ", e);
            return createBadRequestResponse(exchangeDto, "Execution Error: " + e.getMessage());
        }
    }

    private EvaluationResponse validateGroovyScript(ExchangeDto exchangeDto, String script) {
        try {
            GroovyExpression groovyExpression = new GroovyExpression(script);
            Exchange exchangeRequest = ExchangeMarshaller.unmarshall(exchangeDto);
            Object response = groovyExpression.evaluate(exchangeRequest, String.class);
            ExchangeDto exchangeDtoResponse = ExchangeMarshaller.marshall(exchangeRequest);
            return createOKRequestResponse(exchangeDtoResponse, String.valueOf(response));
        } catch (Exception e) {
            log.error("Invalid groovy script: '", e);
            return createBadRequestResponse(exchangeDto, "Invalid groovy script: '" + e.getMessage() + "'");
        }
    }

    private EvaluationResponse validateJavaScript(ExchangeDto exchangeDto, String script) {

        try {
            JavaScriptExpression javaScriptEvaluator = new JavaScriptExpression(script);
            Exchange exchangeRequest = ExchangeMarshaller.unmarshall(exchangeDto);
            Object response = javaScriptEvaluator.evaluate(exchangeRequest, String.class);
            ExchangeDto exchangeDtoResponse = ExchangeMarshaller.marshall(exchangeRequest);
            return createOKRequestResponse(exchangeDtoResponse, String.valueOf(response));
        } catch (Exception e) {
            log.error("Invalid javascript: '", e);
            return createBadRequestResponse(exchangeDto, "Invalid javascript: '" + e.getMessage() + "'");
        }

    }

    private EvaluationResponse createOKRequestResponse(ExchangeDto exchange, String message) {
        return new EvaluationResponse(exchange, message, 1);
    }

    private EvaluationResponse createBadRequestResponse(ExchangeDto exchange, String message) {
        return new EvaluationResponse(exchange, message, -1);
    }
}