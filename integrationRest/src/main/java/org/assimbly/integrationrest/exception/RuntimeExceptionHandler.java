package org.assimbly.integrationrest.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.assimbly.util.rest.ResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.NativeWebRequest;

@ControllerAdvice
public class RuntimeExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(RuntimeExceptionHandler.class);

    // The one generic error response of the REST API, for exceptions that an endpoint doesn't handle itself
    @ExceptionHandler({Exception.class})
    public ResponseEntity<String> integrationErrorHandler(Exception error, NativeWebRequest request) {

        HttpServletRequest httpServletRequest = request.getNativeRequest(HttpServletRequest.class);
        String accept = httpServletRequest != null ? httpServletRequest.getHeader("ACCEPT") : null;
        String mediaType = accept != null ? accept : "application/json";
        String path = httpServletRequest != null ? httpServletRequest.getRequestURI() : "unknown";
        String message = error.getMessage();

        log.error("Request {} failed", path, error);

        return ResponseUtil.createFailureResponse(1L, mediaType,path,message);
    }

}
