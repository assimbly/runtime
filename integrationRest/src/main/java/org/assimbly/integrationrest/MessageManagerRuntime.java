package org.assimbly.integrationrest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.servlet.http.HttpServletRequest;
import org.assimbly.integration.Integration;
import org.assimbly.util.rest.ResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.NativeWebRequest;

import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Resource to return information about the currently running Spring profiles.
 */
@ControllerAdvice
@RestController
@RequestMapping("/api")
public class MessageManagerRuntime {

	protected Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private IntegrationRuntime integrationRuntime;

    private Integration integration;

    /**
     * POST  /integration/send : Send messages to an step (fire and forget).
     *
     * @return if message has been send
     * @throws Exception Message send failure
     */
    @PostMapping(
            path = "/integration/send/{numberOfTimes}",
            consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> send(
            @RequestBody Optional<String> requestBody,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType,
            @RequestHeader(value = "uri", required = false) String uri,
            @RequestHeader(value = "stepId", required = false) String stepId,
            @RequestHeader(value = "serviceid", required = false) String serviceId,
            @RequestHeader(value = "serviceKeys", required = false) String serviceKeys,
            @RequestHeader(value = "headerKeys", required = false) String headerKeys,
            @PathVariable(value = "numberOfTimes") Integer numberOfTimes
    ) throws Exception {

        String body = requestBody.orElse(" ");

        TreeMap<String, String> serviceMap;

        TreeMap<String, Object> headerMap = new TreeMap<>();

        integration = integrationRuntime.getIntegration();

        try {
            if(serviceId != null && !serviceId.isBlank()) {
                serviceMap = toStringTreeMap(getMap(serviceKeys));
                serviceMap.put("to." + stepId + ".uri",uri);
                serviceMap.put("to." + stepId + ".service.id",serviceId);
                setService(serviceMap,stepId);
            }

            if(headerKeys != null && !headerKeys.isBlank()) {
                headerMap = getMap(headerKeys);
            }

            if(headerMap.isEmpty()){
                integration.send(uri,body,numberOfTimes);
            }else {
                integration.sendWithHeaders(uri, body, headerMap, numberOfTimes);
            }

            return ResponseUtil.createSuccessResponse(1L, mediaType,"/integration/send","Sent successfully");
        } catch (Exception e) {
            log.error("Send message to " + uri + " failed",e);
            return ResponseUtil.createFailureResponse(1L, mediaType,"/integration/send","Error: " + e.getMessage() + " Cause: " + e.getCause());
        }
    }

    /**
     * POST  /integration/sendrequest : Send request messages to an step.
     *
     * @return the reply message
     * @throws Exception Message send failure
     */
    @PostMapping(
            path = "/integration/sendrequest",
            consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> sendRequest(
            @RequestBody Optional<String> requestBody,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType,
            @RequestHeader(value = "uri", required = false) String uri,
            @RequestHeader(value = "stepId", required = false) String stepId,
            @RequestHeader(value = "serviceid", required = false) String serviceId,
            @RequestHeader(value = "serviceKeys", required = false) String serviceKeys,
            @RequestHeader(value = "headerKeys", required = false) String headerKeys
    ) throws Exception {

        String body = requestBody.orElse(" ");
        String result;

        integration = integrationRuntime.getIntegration();

        TreeMap<String, String> serviceMap;

        TreeMap<String, Object> headerMap = new TreeMap<>();

        try {
            if(serviceId != null && !serviceId.isBlank()) {
                serviceMap = toStringTreeMap(getMap(serviceKeys));
                serviceMap.put("to." + stepId + ".uri",uri);
                serviceMap.put("to." + stepId + ".service.id",serviceId);
                setService(serviceMap,stepId);
            }

            if(headerKeys != null && !headerKeys.isBlank()) {
                headerMap = getMap(headerKeys);
            }

            if(headerMap.isEmpty()){
                result = integration.sendRequest(uri,body);
            }else {
                result = integration.sendRequestWithHeaders(uri, body, headerMap);
            }

            return ResponseUtil.createSuccessResponse(1L, mediaType,"/integration/sendrequest",result);
        } catch (Exception e) {
            log.error("Send reuqest message to " + uri + " failed",e);
            return ResponseUtil.createFailureResponse(1L, mediaType,"/integration/sendrequest",e.getMessage());
        }
    }

    // Generates a generic error response (exceptions outside try catch):
    @ExceptionHandler({Exception.class})
    public ResponseEntity<String> integrationErrorHandler(Exception error, NativeWebRequest request) throws Exception {

        HttpServletRequest httpServletRequest = request.getNativeRequest(HttpServletRequest.class);
        String mediaType = httpServletRequest != null ? httpServletRequest.getHeader("ACCEPT") : "application/json";
        String path = httpServletRequest != null ? httpServletRequest.getRequestURI() : "unknown";
        String message = error.getMessage();

        return ResponseUtil.createFailureResponse(1L, mediaType, path, message);
    }

    private  TreeMap<String, Object> getMap(String message) throws JsonProcessingException {

        return new ObjectMapper().readValue(message, new TypeReference<TreeMap<String, Object>>(){});

    }

    private  TreeMap<String, String> toStringTreeMap(TreeMap<String, Object> map) {

        TreeMap<String,String> newMap =new TreeMap<>();

        for(Map.Entry<String,Object> entry : map.entrySet()) {
            newMap.put(entry.getKey(), (String) entry.getValue());
        }

        return newMap;

    }


    private void setService(TreeMap<String, String> serviceMap, String stepId) throws Exception {
        integration = integrationRuntime.getIntegration();
        integration.setConnection(serviceMap,"to." + stepId + ".service.id");
    }

}
