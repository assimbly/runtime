package org.assimbly.dil.validation;

import groovy.lang.GroovyShell;
import org.apache.camel.Exchange;
import org.apache.camel.RuntimeCamelException;
//import org.apache.camel.language.js.JavaScriptExpression;
import org.apache.camel.model.language.JavaScriptExpression;
import org.assimbly.dil.validation.beans.script.EvaluationRequest;
import org.assimbly.dil.validation.beans.script.EvaluationResponse;
import org.assimbly.dil.validation.beans.script.ExchangeDto;
import org.assimbly.dil.validation.beans.script.ScriptDto;
import org.assimbly.dil.validation.scripts.ExchangeMarshaller;
import org.assimbly.sandbox.executors.GroovySandboxExecutor;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.SecureASTCustomizer;
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
            // 1. Setup the same Sandbox Security
            SecureASTCustomizer customizer = new SecureASTCustomizer();
            customizer.setDisallowedReceivers(Arrays.asList(
                    "java.lang.System", "java.lang.Runtime", "java.util.TimeZone",
                    "java.util.Locale", "java.lang.Class", "java.lang.ClassLoader",
                    "java.lang.Thread", "java.lang.ThreadGroup", "java.lang.reflect.Method",
                    "java.lang.reflect.Field", "java.lang.reflect.Constructor"
            ));

            CompilerConfiguration config = new CompilerConfiguration();
            config.addCompilationCustomizers(customizer);

            // 2. Prepare the data
            Exchange exchangeRequest = ExchangeMarshaller.unmarshall(exchangeDto);

            // 3. Evaluate using the Secure Shell instead of GroovyExpression
            GroovyShell shell = new GroovyShell(config);

            // Standard bindings for Camel-like scripts
            shell.setProperty("exchange", exchangeRequest);
            shell.setProperty("request", exchangeRequest.getIn());
            shell.setProperty("headers", exchangeRequest.getIn().getHeaders());
            shell.setProperty("body", exchangeRequest.getIn().getBody());

            Object response = shell.evaluate(script);

            // 4. Finalize response
            ExchangeDto exchangeDtoResponse = ExchangeMarshaller.marshall(exchangeRequest);
            return createOKRequestResponse(exchangeDtoResponse, String.valueOf(response));

        } catch (SecurityException | org.codehaus.groovy.control.CompilationFailedException e) {
            // This catches script hacks like TimeZone.setDefault() or syntax errors
            log.error("Sandbox Security Violation or Syntax Error: ", e);
            return createBadRequestResponse(exchangeDto, "Security/Validation Error: " + e.getMessage());
        } catch (Exception e) {
            log.error("Execution error: ", e);
            return createBadRequestResponse(exchangeDto, "Execution Error: " + e.getMessage());
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