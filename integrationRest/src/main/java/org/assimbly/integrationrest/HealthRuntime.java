package org.assimbly.integrationrest;

import io.swagger.v3.oas.annotations.Parameter;
import org.assimbly.integration.Integration;
import org.assimbly.util.rest.ResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


/**
 * Resource to return information about the health of flows.
 */
@ControllerAdvice
@RestController
@RequestMapping("/api")
public class HealthRuntime {

    protected Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private IntegrationRuntime integrationRuntime;

    private Integration integration;

    private boolean plainResponse;


    public HealthRuntime() throws Exception {
    }

    //healtchecks of flows and steps

    @GetMapping(
            path = "/integration/health",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> getHealth(
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType,
            @RequestHeader(required = true, defaultValue = "route", value = "Type") String type) throws Exception {

        plainResponse = true;
        integration = integrationRuntime.getIntegration();

        try {
            String health = integration.getHealth(type, mediaType);
            if(health.startsWith("Error")||health.startsWith("Warning")) {plainResponse = false;}
            return ResponseUtil.createSuccessResponse(1L, mediaType,"/integration/Health",health,plainResponse);
        } catch (Exception e) {
            log.error("Get health failed",e);
            return ResponseUtil.createFailureResponse(1L, mediaType,"/integration/Health",e.getMessage());
        }
    }

    @PostMapping(
            path = "/integration/healthbyflowids",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> getHealthByFlowIds(
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType,
            @RequestBody String flowIds,
            @RequestHeader(required = true, defaultValue = "route", value = "Type") String type
    ) throws Exception {

        plainResponse = true;
        integration = integrationRuntime.getIntegration();

        try {
            String Health = integration.getHealthByFlowIds(flowIds, type, mediaType);
            if(Health.startsWith("Error")||Health.startsWith("Warning")) {plainResponse = false;}
            return ResponseUtil.createSuccessResponse(1L, mediaType,"/integration/healthbyflowids",Health,plainResponse);
        } catch (Exception e) {
            log.error("Get Health by flow ids failed",e);
            return ResponseUtil.createFailureResponse(1L, mediaType,"/integration/healthbyflowids",e.getMessage());
        }
    }

    @GetMapping(
            path = "/integration/flow/{flowId}/health",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> getFlowHealth(
            @PathVariable(value = "flowId") String flowId,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType,
            @RequestHeader(required = true, defaultValue = "route", value = "Type") String type,
            @RequestHeader(required = false, defaultValue = "false", value = "IncludeSteps") boolean includeSteps,
            @RequestHeader(required = false, defaultValue = "false", value = "IncludeError") boolean includeError,
            @RequestHeader(required = false, defaultValue = "false", value = "IncludeDetails") boolean includeDetails
    ) throws Exception {

        plainResponse = true;

        try {
            integration = integrationRuntime.getIntegration();

            String flowHealth = integration.getFlowHealth(flowId, type, includeSteps, includeError, includeDetails, mediaType);
            if(flowHealth.startsWith("Error")||flowHealth.startsWith("Warning")) {plainResponse = false;}
            return ResponseUtil.createSuccessResponse(1L, mediaType,"/integration/flow/{flowId}/health",flowHealth,plainResponse);
        } catch (Exception e) {
            log.error("Get flowHealth " + flowId + " failed",e);
            return ResponseUtil.createFailureResponse(1L, mediaType,"/integration/flow/{flowId}/health",e.getMessage());
        }
    }

    @GetMapping(
            path = "/integration/flow/{flowId}/step/{stepId}/health",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> getFlowStepHealth(
            @PathVariable(value = "flowId") String flowId,
            @PathVariable(value = "stepId") String stepId,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType,
            @RequestHeader(required = true, defaultValue = "route", value = "Type") String type,
            @RequestHeader(required = false, defaultValue = "false", value = "IncludeError") boolean includeError,
            @RequestHeader(required = false, defaultValue = "false", value = "IncludeDetails") boolean includeDetails
    ) throws Exception {

        plainResponse = true;

        try {
            integration = integrationRuntime.getIntegration();

            String flowHealth = integration.getFlowStepHealth(flowId, stepId, type, includeError, includeDetails, mediaType);
            if(flowHealth.startsWith("Error")||flowHealth.startsWith("Warning")) {plainResponse = false;}
            return ResponseUtil.createSuccessResponse(1L, mediaType,"/integration/flow/{flowId}/step/{stepId}/health",flowHealth,plainResponse);
        } catch (Exception e) {
            log.error("Get flowHealth " + flowId + " for stepId=" + stepId + " failed",e);
            return ResponseUtil.createFailureResponse(1L, mediaType,"/integration/flow/{flowId}/step/{stepId}/health",e.getMessage());
        }
    }

}
