package org.assimbly.integrationrest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Parameter;
import org.assimbly.integration.Integration;

import org.assimbly.util.rest.ResponseUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.NativeWebRequest;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * Resource to return information about the currently running Spring profiles.
 */
@ControllerAdvice
@RestController
@RequestMapping("/api")
public class MessageManagerResource {

	protected Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private IntegrationResource integrationResource;

    Integration integration;

    /**
     * POST  /integration/{integrationId}/send : Send messages to an step (fire and forget).
     *
     * @param integrationId (gatewayId)
     * @return if message has been send
     * @throws Exception Message send failure
     */
    @PostMapping(path = "/integration/{integrationId}/send/{numberOfTimes}", consumes =  {"text/plain","application/xml","application/json"}, produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> send(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType,
                                       @RequestHeader(name = "uri", required = false) String uri,
                                       @RequestHeader(name = "stepId", required = false) String stepId,
                                       @RequestHeader(name = "serviceid", required = false) String serviceId,
                                       @RequestHeader(name = "serviceKeys", required = false) String serviceKeys,
                                       @RequestHeader(name = "headerKeys", required = false) String headerKeys,
                                       @PathVariable Integer numberOfTimes,
                                       @PathVariable Long integrationId,
                                       @RequestBody Optional<String> requestBody) throws Exception {

        String body = requestBody.orElse(" ");

        TreeMap<String, String> serviceMap;

        TreeMap<String, Object> headerMap = new TreeMap<>();

        integration = integrationResource.getIntegration();

        try {
            if(serviceId != null && !serviceId.isBlank()) {
                serviceMap = toStringTreeMap(getMap(serviceKeys));
                serviceMap.put("to." + stepId + ".uri",uri);
                serviceMap.put("to." + stepId + ".service.id",serviceId);
                setService(serviceMap,stepId);
            }

            if(headerKeys != null && !headerKeys.isBlank()) {
                headerMap = getMap(headerKeys);;
            }

            if(headerMap.isEmpty()){
                integration.send(uri,body,numberOfTimes);
            }else {
                integration.sendWithHeaders(uri, body, headerMap, numberOfTimes);
            }

            return ResponseUtil.createSuccessResponse(integrationId, mediaType,"/integration/{integrationId}/send","Sent succesfully");
        } catch (Exception e) {
            log.error("Send message to " + uri + " failed",e);
            return ResponseUtil.createFailureResponse(integrationId, mediaType,"/integration/{integrationId}/send","Error: " + e.getMessage() + " Cause: " + e.getCause());
        }
    }

    /**
     * POST  /integration/{integrationId}/sendrequest : Send request messages to an step.
     *
     * @param integrationId (gatewayId)
     * @return the reply message
     * @throws Exception Message send failure
     */
    @PostMapping(path = "/integration/{integrationId}/sendrequest", consumes =  {"text/plain","application/xml","application/json"}, produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> sendRequest(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType,
                                       @RequestHeader(name = "uri", required = false) String uri,
                                       @RequestHeader(name = "stepId", required = false) String stepId,
                                       @RequestHeader(name = "serviceid", required = false) String serviceId,
                                       @RequestHeader(name = "serviceKeys", required = false) String serviceKeys,
                                       @RequestHeader(name = "headerKeys", required = false) String headerKeys,
                                       @PathVariable Long integrationId,
                                       @RequestBody Optional<String> requestBody) throws Exception {

        String body = requestBody.orElse(" ");
        String result;

        integration = integrationResource.getIntegration();

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
                headerMap = getMap(headerKeys);;
            }

            if(headerMap.isEmpty()){
                result = integration.sendRequest(uri,body);
            }else {
                result = integration.sendRequestWithHeaders(uri, body, headerMap);
            }

            return ResponseUtil.createSuccessResponse(integrationId, mediaType,"/integration/{integrationId}/send",result);
        } catch (Exception e) {
            log.error("Send reuqest message to " + uri + " failed",e);
            return ResponseUtil.createFailureResponse(integrationId, mediaType,"/integration/{integrationId}/send",e.getMessage());
        }
    }

    // Generates a generic error response (exceptions outside try catch):
    @ExceptionHandler({Exception.class})
    public ResponseEntity<String> integrationErrorHandler(Exception error, NativeWebRequest request) throws Exception {

    	Long integrationId = 0L; // set integrationid to 0, as we may get a string value
    	String mediaType = request.getNativeRequest(HttpServletRequest.class).getHeader("ACCEPT");
    	String path = request.getNativeRequest(HttpServletRequest.class).getRequestURI();
    	String message = error.getMessage();

    	return ResponseUtil.createFailureResponse(integrationId, mediaType,path,message);
    }

    private  TreeMap<String, Object> getMap(String message) throws JsonProcessingException {

        TreeMap<String, Object> map = new ObjectMapper().readValue(message, new TypeReference<TreeMap<String, Object>>(){});

        return map;

    }

    private  TreeMap<String, String> toStringTreeMap(TreeMap<String, Object> map) throws JsonProcessingException {

        TreeMap<String,String> newMap =new TreeMap<String,String>();

        for(Map.Entry<String,Object> entry : map.entrySet()) {
            newMap.put(entry.getKey(), (String) entry.getValue());
        }

        return newMap;

    }


    private void setService(TreeMap<String, String> serviceMap, String stepId) throws Exception {
        integration = integrationResource.getIntegration();
        integration.setConnection(serviceMap,"to." + stepId + ".service.id");
    }

}
