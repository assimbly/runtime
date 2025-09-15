package org.assimbly.integrationrest.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.assimbly.util.rest.ResponseUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.NativeWebRequest;

@ControllerAdvice
public class RuntimeExceptionHandler {

    // Generates a generic error response (exceptions outside try catch):
    @ExceptionHandler({Exception.class})
    public ResponseEntity<String> integrationErrorHandler(Exception error, NativeWebRequest request) {

        HttpServletRequest httpServletRequest = request.getNativeRequest(HttpServletRequest.class);
        String mediaType = httpServletRequest != null ? httpServletRequest.getHeader("ACCEPT") : "application/json";
        String path = httpServletRequest != null ? httpServletRequest.getRequestURI() : "unknown";
        String message = error.getMessage();

        return ResponseUtil.createFailureResponse(1L, mediaType,path,message);
    }

}
