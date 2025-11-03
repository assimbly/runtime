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
import java.time.Duration;
import java.time.Instant;
import java.util.Calendar;
import java.util.concurrent.TimeoutException;

public class CustomHttpBinding extends DefaultHttpBinding {

    protected Logger log = LoggerFactory.getLogger(getClass());

    private static final String DEFAULT_ERROR_MESSAGE = "Error processing request. Details available in the log/trace viewer.";

    private static final String EMPTY_BODY_MESSAGE = "HTTP service couldn't read your request, did you supply a correct body?";

    private static final String HTTP_RESPONSE_TIME = "ResponseTime";

    @Override
    public void writeResponse(Exchange exchange, HttpServletResponse response) throws IOException {

        Message message = exchange.getMessage();

        Exception exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);

        if (exception != null) {
            addResponseTimeHeader(exchange, message);
            try {
                doWriteExceptionResponse(message, exception, response);
            } catch (Exception e) {
                log.error("Cannot write response: {}", e.getMessage(), e);
            }

        } else {
            if (exchange.getMessage() != null) {
                // just copy the protocol relates header if we do not have them
                customCopyProtocolHeaders(exchange.getIn(), exchange.getMessage());
            }
            addResponseTimeHeader(exchange, message);
            doWriteResponse(message, response, exchange);
        }

    }

    private void doWriteExceptionResponse(Message message, Throwable exception, HttpServletResponse response) throws Exception {

        if (exception instanceof TimeoutException) {
            response.setStatus(HttpServletResponse.SC_GATEWAY_TIMEOUT);
            response.setContentType("text/plain");
            response.getWriter().write("Timeout error");
        }else {
            generateExceptionResponse(message, exception, response);
        }
    }

    private void generateExceptionResponse(Message message, Throwable exception, HttpServletResponse response) throws Exception {

        String accept = message.getHeader("Accept", String.class);
        String userAgent = message.getHeader("User-Agent", String.class);
        String infoMessage;
        String responseBody = null;

        if(message.getBody() == null) {
            infoMessage = EMPTY_BODY_MESSAGE;
        } else {
            infoMessage = DEFAULT_ERROR_MESSAGE;
        }

        boolean isBrowser = userAgent != null && (
                userAgent.contains("Firefox") ||
                        userAgent.contains("Chrome") ||
                        userAgent.contains("Safari")
        );

        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

        if(accept != null){
            if(isBrowser){
                responseBody = generateHtmlResponse(response.getStatus(), infoMessage, exception.toString());
                response.setContentType("text/html; charset=UTF-8");
            }else if(accept.contains("application/xml") || accept.contains("text/xml")){
                responseBody = generateXmlResponse(response.getStatus(), infoMessage, exception.toString());
                response.setContentType("text/xml");
            }else if(accept.contains("application/json")){
                responseBody = generateJsonResponse(response.getStatus(), infoMessage, exception.toString());
                response.setContentType("application/json");
            }
        }
        if(responseBody == null) {
            responseBody = generateJsonResponse(response.getStatus(), infoMessage, exception.toString());
        }

        response.getWriter().write(responseBody);

    }

    private String generateJsonResponse(int code, String info, String error) {

        JSONObject response = new JSONObject();

        response.put("code", code);
        response.put("info", info);
        response.put("error", error);

        return response.toString(2);
    }

    private String generateXmlResponse(int code, String info, String error) {

        return String.format("""
            <Response>
               <Code>%s</Code>
               <Info>%s</Info>
               <Error>%s</Error>
            </Response>
            """, code, info, error);

    }

    private String generateHtmlResponse(int code, String info, String error) {

        return String.format("""
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Error Report</title>
                    <style>
                        body {
                            font-family: Arial, sans-serif;
                            max-width: 800px;
                            margin: 50px auto;
                            padding: 20px;
                            background: #f5f5f5;
                        }
                
                        .report {
                            background: white;
                            padding: 30px;
                            border-radius: 8px;
                            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                        }
                
                        h1 {
                            color: #d32f2f;
                            margin-bottom: 30px;
                        }
                
                        .section {
                            margin-bottom: 20px;
                        }
                
                        .label {
                            font-weight: bold;
                            margin-bottom: 5px;
                            color: #555;
                        }
                
                        .value {
                            padding: 10px;
                            background: #f9f9f9;
                            border-left: 3px solid #d32f2f;
                        }
                
                        .code {
                            font-family: monospace;
                            font-size: 13px;
                            line-height: 1.5;
                        }
                    </style>
                </head>
                <body>
                    <div class="report">
                        <h1>Error Report</h1>

                        <div class="section">
                            <div class="label">Info</div>
                            <div class="value">%s</div>
                        </div>                
                        
                        <div class="section">
                            <div class="label">Code</div>
                            <div class="value">%s</div>
                        </div>
                        
                        <div class="section">
                            <div class="label">Error</div>
                            <div class="value code">%s</div>
                        </div>
                    </div>
                </body>
                </html>
            """, info, code, error);



    }

    private void customCopyProtocolHeaders(Message request, Message response) {
        if (request.getHeader(Exchange.CONTENT_ENCODING) != null) {
            String contentEncoding = request.getHeader(Exchange.CONTENT_ENCODING, String.class);
            response.setHeader(Exchange.CONTENT_ENCODING, contentEncoding);
        }
        if (checkChunked(response, response.getExchange())) {
            response.setHeader(Exchange.TRANSFER_ENCODING, "chunked");
        }
    }
    private void addResponseTimeHeader(Exchange exchange, Message message) {
        Instant initInstant = Instant.ofEpochMilli(exchange.getClock().getCreated());
        Instant nowInstant = Calendar.getInstance().toInstant();
        Duration duration = Duration.between(initInstant, nowInstant);
        message.setHeader(HTTP_RESPONSE_TIME, String.valueOf(duration.toMillis()));
    }

}