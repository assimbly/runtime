package org.assimbly.integrationrest;

import org.springframework.web.bind.annotation.*;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.assimbly.integration.Integration;
import org.assimbly.util.rest.ResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import tools.jackson.core.JacksonException;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Resource to return information about the currently running Spring profiles.
 */
@Tag(name = "Messages", description = "Send messages to a step")
@RestController
@RequestMapping("/api")
public class MessageManagerRuntime {

	protected Logger log = LoggerFactory.getLogger(getClass());

    private final Integration integration;

    public MessageManagerRuntime(IntegrationRuntime integrationRuntime) {
        this.integration = integrationRuntime.getIntegration();
    }

    /**
     * POST  /integration/send : Send messages to an step (fire and forget).
     *
     * @return if message has been send
     */
    @Operation(summary = "Send a message (fire and forget)")
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
    ) {

        String body = requestBody.orElse(" ");

        TreeMap<String, String> serviceMap;

        TreeMap<String, Object> headerMap = new TreeMap<>();

        try {
            requireUri(uri);
            if (numberOfTimes < 1) {
                throw new IllegalArgumentException("numberOfTimes must be 1 or more");
            }

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

            return ResponseUtil.createSuccessResponse(1L, mediaType,"/integration/send/{numberOfTimes}","Sent successfully");
        } catch (Exception e) {
            log.error("Send message to {} failed", uri, e);
            return ResponseUtil.createFailureResponse(1L, mediaType,"/integration/send/{numberOfTimes}","Error: " + e.getMessage() + " Cause: " + e.getCause());
        }
    }

    /**
     * POST  /integration/sendrequest : Send request messages to an step.
     *
     * @return the reply message
     */
    @Operation(summary = "Send a request message and return the reply")
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
    ) {

        String body = requestBody.orElse(" ");
        String result;

        TreeMap<String, String> serviceMap;

        TreeMap<String, Object> headerMap = new TreeMap<>();

        try {
            requireUri(uri);

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
            log.error("Send reuqest message to {} failed", uri, e);
            return ResponseUtil.createFailureResponse(1L, mediaType,"/integration/sendrequest",e.getMessage());
        }
    }


    // Missing or impossible input is answered with what is wrong, instead of the text of a NullPointerException
    private static void requireUri(String uri) {
        if (uri == null || uri.isBlank()) {
            throw new IllegalArgumentException("The uri header is required");
        }
    }

    private  TreeMap<String, Object> getMap(String message) throws JacksonException {

        if (message == null || message.isBlank()) {
            return new TreeMap<>();
        }

        return new ObjectMapper().readValue(message, new TypeReference<>() {
        });

    }

    static TreeMap<String, String> toStringTreeMap(TreeMap<String, Object> map) {

        TreeMap<String,String> newMap =new TreeMap<>();

        for(Map.Entry<String,Object> entry : map.entrySet()) {
            // a number or boolean in the JSON is a valid value too
            newMap.put(entry.getKey(), Objects.toString(entry.getValue(), null));
        }

        return newMap;

    }


    private void setService(TreeMap<String, String> serviceMap, String stepId) throws Exception {
        integration.setConnection(serviceMap,"to." + stepId + ".service.id");
    }

}
