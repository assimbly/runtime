package org.assimbly.dil.blocks.beans;

import org.json.JSONObject;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.http.common.DefaultHttpBinding;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.assimbly.util.helper.XmlHelper;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.*;
import java.util.Calendar;
import java.util.concurrent.TimeoutException;

public class CustomHttpBinding extends DefaultHttpBinding {

    protected Logger log = LoggerFactory.getLogger(getClass());

    private static final String DEFAULT_ERROR_MESSAGE = "Something went wrong calling the HTTP service. Please refer to the logs for more information.";
    private static final String EMPTY_BODY_MESSAGE = "HTTP service couldn't read your request, did you supply a correct body?";

    private static final String HTTP_RESPONSE_TIME = "ResponseTime";

    @Override
    public void writeResponse(Exchange exchange, HttpServletResponse response) throws IOException {
        Message target = exchange.hasOut() ? exchange.getMessage() : exchange.getIn();
        if (exchange.isFailed()) {
            if (exchange.getException() != null) {
                addResponseTimeHeader(exchange, target);
                try {
                    doWriteExceptionResponse(target, exchange.getException(), response);    
                } catch (Exception e) {
                    log.error("Cannot write response: " + e.getMessage());
                }
                
            } else {
                addResponseTimeHeader(exchange, target);
                // it must be a fault, no need to check for the fault flag on the message
                doWriteFaultResponse(target, response, exchange);
            }
        } else {
            if (exchange.hasOut()) {
                // just copy the protocol relates header if we do not have them
                copyProtocolHeaders(exchange.getIn(), exchange.getMessage());
            }
            addResponseTimeHeader(exchange, target);
            doWriteResponse(target, response, exchange);
        }
    }

    private void doWriteExceptionResponse(Message target, Throwable exception, HttpServletResponse response) throws Exception {
        String accept = target.getHeader("Accept", String.class);

        if (exception instanceof TimeoutException) {
            response.setStatus(HttpServletResponse.SC_GATEWAY_TIMEOUT);
            response.setContentType("text/plain");
            response.getWriter().write("Timeout error");
        } else {
            String infoMessage;
            String message = null;

            if(target.getBody() == null) {
                infoMessage = EMPTY_BODY_MESSAGE;
            } else {
                infoMessage = DEFAULT_ERROR_MESSAGE;
            }

            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

            if(accept != null){
                if(accept.contains("application/xml") || accept.contains("text/xml")){
                    message = generateXmlResponse(response.getStatus(), infoMessage, exception.toString());

                    response.setContentType("text/xml");
                }

                if(accept.contains("application/json")){
                    message = generateJsonResponse(response.getStatus(), infoMessage, exception.toString());

                    response.setContentType("application/json");
                }
            }

            if(message == null) {
                message = generateJsonResponse(response.getStatus(), infoMessage, exception.toString());
            }

            response.getWriter().write(message);
        }
    }

    private String generateJsonResponse(int code, String info, String error) throws Exception {
        
        JSONObject response = new JSONObject();

        response.put("code", code);
        response.put("info", info);
        response.put("error", error);

        return response.toString(2);
    }

    private String generateXmlResponse(int code, String info, String error) {
        
        Document doc = XmlHelper.newDocument();

        if(doc == null) {
            return "<Response>Something went wrong generating the response message</Response>";
        }

        Element rootElement = doc.createElement("Response");

        Element codeElement = doc.createElement("Code");
        codeElement.setTextContent(String.valueOf(code));
        rootElement.appendChild(codeElement);

        Element infoElement = doc.createElement("Info");
        infoElement.setTextContent(String.valueOf(info));
        rootElement.appendChild(infoElement);

        Element errorElement = doc.createElement("Error");
        errorElement.setTextContent(error);
        rootElement.appendChild(errorElement);

        doc.appendChild(rootElement);

        return XmlHelper.prettyPrint(doc);
    }

    private void copyProtocolHeaders(Message request, Message response) {
        if (request.getHeader(Exchange.CONTENT_ENCODING) != null) {
            String contentEncoding = request.getHeader(Exchange.CONTENT_ENCODING, String.class);
            response.setHeader(Exchange.CONTENT_ENCODING, contentEncoding);
        }
        if (checkChunked(response, response.getExchange())) {
            response.setHeader(Exchange.TRANSFER_ENCODING, "chunked");
        }
    }

    private void addResponseTimeHeader(Exchange exchange, Message message) {
        Instant initInstant = Instant.ofEpochMilli(exchange.getCreated()); //created.toInstant();
        Instant nowInstant = Calendar.getInstance().toInstant();
        Duration duration = Duration.between(initInstant, nowInstant);

        message.setHeader(HTTP_RESPONSE_TIME, String.valueOf(duration.toMillis()));
    }
}