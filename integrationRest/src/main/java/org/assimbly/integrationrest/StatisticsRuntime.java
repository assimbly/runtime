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
 * Resource to return information about the currently running Spring profiles.
 */
@ControllerAdvice
@RestController
@RequestMapping("/api")
public class StatisticsRuntime {

    protected Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private IntegrationRuntime integrationRuntime;

    private Integration integration;

    private boolean plainResponse;


    public StatisticsRuntime() throws Exception {
    }

    //statistics of integrations, flows and steps

    @GetMapping(
            path = "/integration/stats",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> getStats(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType) throws Exception {

        plainResponse = true;
        integration = integrationRuntime.getIntegration();

        try {
            String stats = integration.getStats(mediaType);
            if(stats.startsWith("Error")||stats.startsWith("Warning")) {plainResponse = false;}
            return ResponseUtil.createSuccessResponse(1L, mediaType,"/integration/stats",stats,plainResponse);
        } catch (Exception e) {
            log.error("Get stats failed",e);
            return ResponseUtil.createFailureResponse(1L, mediaType,"/integration/stats",e.getMessage());
        }
    }

    @GetMapping(
            path = "/integration/messages",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> getMessages(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType) throws Exception {

        plainResponse = true;
        integration = integrationRuntime.getIntegration();

        try {
            String stats = integration.getMessages(mediaType);
            if(stats.startsWith("Error")||stats.startsWith("Warning")) {plainResponse = false;}
            return ResponseUtil.createSuccessResponse(1L, mediaType,"/integration/messages",stats,plainResponse);
        } catch (Exception e) {
            log.error("Get messages failed",e);
            return ResponseUtil.createFailureResponse(1L, mediaType,"/integration/messages",e.getMessage());
        }
    }

    @GetMapping(
            path = "/integration/statsbyflowids",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> getStatsByFlowIds(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @RequestHeader String flowIds) throws Exception {

        plainResponse = true;
        integration = integrationRuntime.getIntegration();

        try {
            String stats = integration.getStatsByFlowIds(flowIds, mediaType);
            if(stats.startsWith("Error")||stats.startsWith("Warning")) {plainResponse = false;}
            return ResponseUtil.createSuccessResponse(1L, mediaType,"/integration/statsbyflowids",stats,plainResponse);
        } catch (Exception e) {
            log.error("Get stats by flow ids failed",e);
            return ResponseUtil.createFailureResponse(1L, mediaType,"/integration/statsbyflowids",e.getMessage());
        }
    }

    @GetMapping(
            path = "/integration/flow/{flowId}/stats",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> getFlowStats(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @RequestHeader(defaultValue="false",value="FullStats") boolean fullStats, @RequestHeader(defaultValue="false",value="IncludeSteps") boolean includeSteps, @PathVariable(value = "flowId") String flowId) throws Exception {

        plainResponse = true;

        try {
            integration = integrationRuntime.getIntegration();

            String flowStats = integration.getFlowStats(flowId, fullStats, includeSteps, mediaType);
            if(flowStats.startsWith("Error")||flowStats.startsWith("Warning")) {plainResponse = false;}
            return ResponseUtil.createSuccessResponse(1L, mediaType,"/integration/flow/{flowId}/stats",flowStats,plainResponse);
        } catch (Exception e) {
            log.error("Get flowstats " + flowId + " failed",e);
            return ResponseUtil.createFailureResponse(1L, mediaType,"/integration/flow/{flowId}/stats",e.getMessage());
        }
    }

    @GetMapping(
            path = "/integration/flow/{flowId}/step/{stepId}/stats",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> getFlowStepStats(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @RequestHeader(defaultValue="false", value="FullStats") boolean fullStats, @PathVariable(value = "flowId") String flowId, @PathVariable(value = "stepId") String stepId) throws Exception {

        plainResponse = true;

        try {
            integration = integrationRuntime.getIntegration();

            String flowStats = integration.getFlowStepStats(flowId, stepId, fullStats, mediaType);
            if(flowStats.startsWith("Error")||flowStats.startsWith("Warning")) {plainResponse = false;}
            return ResponseUtil.createSuccessResponse(1L, mediaType,"/integration/flow/{flowId}/step/{stepId}/stats",flowStats,plainResponse);
        } catch (Exception e) {
            log.error("Get flowstats " + flowId + " for stepId=" + stepId + " failed",e);
            return ResponseUtil.createFailureResponse(1L, mediaType,"/integration/flow/{flowId}/step/{stepId}/stats",e.getMessage());
        }
    }

    @GetMapping(
            path = "/integration/flow/{flowId}/messages",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> getFlowMessages(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @RequestHeader(required=true,defaultValue="false",value="IncludeSteps") boolean includeSteps, @PathVariable(value = "flowId") String flowId) throws Exception {

        try {
            integration = integrationRuntime.getIntegration();

            String numberOfMessages = integration.getFlowMessages(flowId, includeSteps, mediaType);
            return ResponseUtil.createSuccessResponse(1L, mediaType,"/integration/flow/{flowId}/messages",numberOfMessages,true);
        } catch (Exception e) {
            log.error("Get messages for flow " + flowId + " failed",e);
            return ResponseUtil.createFailureResponseWithHeaders(1L, mediaType,"/integration/flow/{flowId}/messages",e.getMessage(),"unable to get total messages of flow " + flowId,flowId);
        }
    }

    @GetMapping(
            path = "/integration/flow/{flowId}/messages/total",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> getFlowTotalMessages(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable(value = "flowId") String flowId) throws Exception {

        try {
            integration = integrationRuntime.getIntegration();

            String numberOfMessages = integration.getFlowTotalMessages(flowId);
            return ResponseUtil.createSuccessResponseWithHeaders(1L, mediaType,"/integration/flow/{flowId}/messages/total",numberOfMessages,numberOfMessages,flowId);
        } catch (Exception e) {
            log.error("Get total messages for flow " + flowId + " failed",e);
            return ResponseUtil.createFailureResponseWithHeaders(1L, mediaType,"/integration/flow/{flowId}/messages/total",e.getMessage(),"unable to get total messages of flow " + flowId,flowId);
        }
    }

    @GetMapping(
            path = "/integration/flow/{flowId}/messages/completed",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> getFlowCompletedMessages(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable(value = "flowId") String flowId) throws Exception {

        try {
            integration = integrationRuntime.getIntegration();

            String completedMessages = integration.getFlowCompletedMessages(flowId);
            return ResponseUtil.createSuccessResponseWithHeaders(1L, mediaType,"/integration/flow/{flowId}/messages/completed",completedMessages,completedMessages,flowId);
        } catch (Exception e) {
            log.error("Get completed messages for flow " + flowId + " failed",e);
            return ResponseUtil.createFailureResponseWithHeaders(1L, mediaType,"/integration/flow/{flowId}/messages/completed",e.getMessage(),"unable to get completed messages of flow " + flowId,flowId);
        }
    }

    @GetMapping(
            path = "/integration/flow/{flowId}/messages/failed",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> getFlowFailedMessages(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable(value = "flowId") String flowId) throws Exception {

        try {
            integration = integrationRuntime.getIntegration();

            String failedMessages = integration.getFlowFailedMessages(flowId);
            return ResponseUtil.createSuccessResponseWithHeaders(1L, mediaType,"/integration/flow/{flowId}/messages/failed",failedMessages,failedMessages,flowId);
        } catch (Exception e) {
            log.error("Get failed messages for flow " + flowId + " failed",e);
            return ResponseUtil.createFailureResponseWithHeaders(1L, mediaType,"/integration/flow/{flowId}/messages/failed",e.getMessage(),"unable to get failed messages of flow " + flowId,flowId);
        }
    }

    @GetMapping(
            path = "/integration/flow/{flowId}/messages/pending",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> getFlowPendingMessages(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable(value = "flowId") String flowId) throws Exception {

        try {
            integration = integrationRuntime.getIntegration();

            String failedMessages = integration.getFlowPendingMessages(flowId);
            return ResponseUtil.createSuccessResponseWithHeaders(1L, mediaType,"/integration/flow/{flowId}/messages/pending",failedMessages,failedMessages,flowId);
        } catch (Exception e) {
            log.error("Get pending messages for flow " + flowId + " failed",e);
            return ResponseUtil.createFailureResponseWithHeaders(1L, mediaType,"/integration/flow/{flowId}/messages/pending",e.getMessage(),"unable to get failed messages of flow " + flowId,flowId);
        }
    }

    @GetMapping(
            path = "/integration/flow/{flowId}/step/{stepId}/messages",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> getStepMessages(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable(value = "flowId") String flowId, @PathVariable(value = "stepId") String stepId) throws Exception {

        try {
            integration = integrationRuntime.getIntegration();

            String numberOfMessages = integration.getStepMessages(flowId, stepId, mediaType);
            return ResponseUtil.createSuccessResponse(1L, mediaType,"/integration/flow/{flowId}/step/{stepId}/messages",numberOfMessages,true);
        } catch (Exception e) {
            log.error("Get messages for flow " + flowId + " for step " + stepId + " failed",e);
            return ResponseUtil.createFailureResponseWithHeaders(1L, mediaType,"/integration/flow/{flowId}/step/{stepId}/messages",e.getMessage(),"unable to get total messages of flow " + flowId,flowId);
        }

    }

    @GetMapping(
            path = "/integration/metrics",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> getMetrics(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType) throws Exception {

        plainResponse = true;
        integration = integrationRuntime.getIntegration();

        try {
            String metrics = integration.getMetrics(mediaType);
            if(metrics.startsWith("Error")||metrics.startsWith("Warning")) {plainResponse = false;}
            return ResponseUtil.createSuccessResponse(1L, mediaType,"/integration/metrics",metrics,plainResponse);
        } catch (Exception e) {
            log.error("Get metrics failed",e);
            return ResponseUtil.createFailureResponse(1L, mediaType,"/integration/metrics",e.getMessage());
        }
    }

    @GetMapping(
            path = "/integration/historymetrics",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> getHistoryMetrics(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType) throws Exception {

        plainResponse = true;
        integration = integrationRuntime.getIntegration();

        try {
            String metrics = integration.getHistoryMetrics(mediaType);
            if(metrics.startsWith("Error")||metrics.startsWith("Warning")) {plainResponse = false;}
            return ResponseUtil.createSuccessResponse(1L, mediaType,"/integration/historymetrics",metrics,plainResponse);
        } catch (Exception e) {
            log.error("Get history metrics failed",e);
            return ResponseUtil.createFailureResponse(1L, mediaType,"/integration/historymetrics",e.getMessage());
        }
    }

}
