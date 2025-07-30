package org.assimbly.dil.validation;

import org.apache.camel.Exchange;
import org.apache.camel.language.groovy.GroovyExpression;
import org.apache.camel.model.language.JavaScriptExpression;
import org.assimbly.dil.validation.beans.script.EvaluationRequest;
import org.assimbly.dil.validation.beans.script.EvaluationResponse;
import org.assimbly.dil.validation.beans.script.ExchangeDto;
import org.assimbly.dil.validation.beans.script.ScriptDto;
import org.assimbly.dil.validation.scripts.ExchangeMarshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
            return switch (scriptDto.getLanguage()) {
                case "js" -> validateJavaScript(evaluationRequest.getExchange(), scriptDto.getScript());
                case "groovy" -> validateGroovyScript(evaluationRequest.getExchange(), scriptDto.getScript());
                default -> createBadRequestResponse(evaluationRequest.getExchange(), "Unsupported scripting language");
            };
        } catch (Exception e) {
            log.error("Invalid script: '", e);
            return createBadRequestResponse(evaluationRequest.getExchange(), "Invalid script: '" + e.getMessage() + "'");
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
            return createBadRequestResponse(exchangeDto, e.getMessage());
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
            return createBadRequestResponse(exchangeDto, e.getMessage());
        }

    }

    private EvaluationResponse createOKRequestResponse(ExchangeDto exchange, String message) {
        return new EvaluationResponse(exchange, message, 1);
    }

    private EvaluationResponse createBadRequestResponse(ExchangeDto exchange, String message) {
        return new EvaluationResponse(exchange, message, -1);
    }
}