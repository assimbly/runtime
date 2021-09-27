package org.assimbly.connectorrest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.ApiParam;
import org.assimbly.connector.Connector;

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

    private final Logger log = LoggerFactory.getLogger(MessageManagerResource.class);

    @Autowired
    private ConnectorResource connectorResource;

    Connector connector;

    /**
     * POST  /connector/{connectorId}/send : Send messages to an endpoint (fire and forget).
     *
     * @param connectorId (gatewayId)
     * @return if message has been send
     * @throws Exception Message send failure
     */
    @PostMapping(path = "/connector/{connectorId}/send/{numberOfTimes}", consumes =  {"text/plain","application/xml","application/json"}, produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> send(@ApiParam(hidden = true) @RequestHeader("Accept") String mediaType,
                                       @RequestHeader(name = "uri", required = false) String uri,
                                       @RequestHeader(name = "endpointid", required = false) String endpointId,
                                       @RequestHeader(name = "serviceid", required = false) String serviceId,
                                       @RequestHeader(name = "serviceKeys", required = false) String serviceKeys,
                                       @RequestHeader(name = "headerKeys", required = false) String headerKeys,
                                       @PathVariable Integer numberOfTimes,
                                       @PathVariable Long connectorId,
                                       @RequestBody Optional<String> requestBody) throws Exception {

        String body = requestBody.orElse(" ");

        TreeMap<String, String> serviceMap = new TreeMap<>();

        TreeMap<String, Object> headerMap = new TreeMap<>();

        connector = connectorResource.getConnector();

        try {
            if(serviceId != null && !serviceId.isBlank()) {
                serviceMap = toStringTreeMap(getMap(serviceKeys));
                serviceMap.put("to." + endpointId + ".uri",uri);
                serviceMap.put("to." + endpointId + ".service.id",serviceId);
                setService(serviceMap,endpointId);
            }

            if(headerKeys != null && !headerKeys.isBlank()) {
                headerMap = getMap(headerKeys);;
            }

            if(!headerMap.isEmpty()){
                connector.sendWithHeaders(uri, body, headerMap, numberOfTimes);
            }else {
                connector.send(uri,body,numberOfTimes);
            }

            return ResponseUtil.createSuccessResponse(connectorId, mediaType,"/connector/{connectorId}/send","Sent succesfully");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseUtil.createFailureResponse(connectorId, mediaType,"/connector/{connectorId}/send","Error: " + e.getMessage() + " Cause: " + e.getCause());
        }
    }

    /**
     * POST  /connector/{connectorId}/sendrequest : Send request messages to an endpoint.
     *
     * @param connectorId (gatewayId)
     * @return the reply message
     * @throws Exception Message send failure
     */
    @PostMapping(path = "/connector/{connectorId}/sendrequest", consumes =  {"text/plain","application/xml","application/json"}, produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> sendRequest(@ApiParam(hidden = true) @RequestHeader("Accept") String mediaType,
                                       @RequestHeader(name = "uri", required = false) String uri,
                                       @RequestHeader(name = "endpointid", required = false) String endpointId,
                                       @RequestHeader(name = "serviceid", required = false) String serviceId,
                                       @RequestHeader(name = "serviceKeys", required = false) String serviceKeys,
                                       @RequestHeader(name = "headerKeys", required = false) String headerKeys,
                                       @PathVariable Long connectorId,
                                       @RequestBody Optional<String> requestBody) throws Exception {

        String body = requestBody.orElse(" ");
        String result = "No reply";

        connector = connectorResource.getConnector();

        TreeMap<String, String> serviceMap = new TreeMap<>();

        TreeMap<String, Object> headerMap = new TreeMap<>();

        try {
            if(serviceId != null && !serviceId.isBlank()) {
                serviceMap = toStringTreeMap(getMap(serviceKeys));
                serviceMap.put("to." + endpointId + ".uri",uri);
                serviceMap.put("to." + endpointId + ".service.id",serviceId);
                setService(serviceMap,endpointId);
            }

            if(headerKeys != null && !headerKeys.isBlank()) {
                headerMap = getMap(headerKeys);;
            }

            if(!headerMap.isEmpty()){
                result = connector.sendRequestWithHeaders(uri, body, headerMap);
            }else {
                result = connector.sendRequest(uri,body);
            }

            return ResponseUtil.createSuccessResponse(connectorId, mediaType,"/connector/{connectorId}/send",result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseUtil.createFailureResponse(connectorId, mediaType,"/connector/{connectorId}/send",e.getMessage());
        }
    }

    // Generates a generic error response (exceptions outside try catch):
    @ExceptionHandler({Exception.class})
    public ResponseEntity<String> connectorErrorHandler(Exception error, NativeWebRequest request) throws Exception {

    	Long connectorId = 0L; // set connectorid to 0, as we may get a string value
    	String mediaType = request.getNativeRequest(HttpServletRequest.class).getHeader("ACCEPT");
    	String path = request.getNativeRequest(HttpServletRequest.class).getRequestURI();
    	String message = error.getMessage();

    	return ResponseUtil.createFailureResponse(connectorId, mediaType,path,message);
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


    private void setService(TreeMap<String, String> serviceMap, String endpointId) throws Exception {
        connector = connectorResource.getConnector();
        connector.setConnection(serviceMap,"to." + endpointId + ".service.id");
    }

}
