package org.assimbly.integrationrest;

import io.swagger.v3.oas.annotations.Parameter;
import org.assimbly.docconverter.DocConverter;
import org.assimbly.integration.Integration;
import org.assimbly.util.rest.ResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


/**
 * Resource to return information about the stats of flows.
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
    public ResponseEntity<String> getStats(@Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType) throws Exception {

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
            path = "/integration/stats/steps",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> getRoutesStats(@Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType) throws Exception {

        plainResponse = true;
        integration = integrationRuntime.getIntegration();

        try {
            String stats = integration.getStepsStats(mediaType);
            if(stats.startsWith("Error")||stats.startsWith("Warning")) {plainResponse = false;}
            return ResponseUtil.createSuccessResponse(1L, mediaType,"/integration/stats/steps",stats,plainResponse);
        } catch (Exception e) {
            log.error("Get stats failed",e);
            return ResponseUtil.createFailureResponse(1L, mediaType,"/integration/stats/steps",e.getMessage());
        }
    }

    @GetMapping(
            path = "/integration/stats/flows",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> getFlowsStats(@Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType) throws Exception {

        plainResponse = true;
        integration = integrationRuntime.getIntegration();

        try {
            String stats = integration.getFlowsStats(mediaType);
            if(stats.startsWith("Error")||stats.startsWith("Warning")) {plainResponse = false;}
            return ResponseUtil.createSuccessResponse(1L, mediaType,"/integration/stats/flows",stats,plainResponse);
        } catch (Exception e) {
            log.error("Get stats failed",e);
            return ResponseUtil.createFailureResponse(1L, mediaType,"/integration/stats/flows",e.getMessage());
        }
    }


    @GetMapping(
            path = "/integration/messages",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> getMessages(@Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType) throws Exception {

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

    @PostMapping(
            path = "/integration/statsbyflowids",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> getStatsByFlowIds(
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType,
            @RequestBody String flowIds,
            @RequestHeader(required = false, defaultValue = "", value = "filter") String filter
    ) throws Exception {

        plainResponse = true;
        integration = integrationRuntime.getIntegration();

        try {
            String stats = integration.getStatsByFlowIds(flowIds, filter, mediaType);

            if(mediaType.contains("xml")) {
                stats = DocConverter.convertJsonToXml(stats);
            }

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
    public ResponseEntity<String> getFlowStats(
            @PathVariable(value = "flowId") String flowId,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType,
            @RequestHeader(required = false, defaultValue = "false", value = "FullStats") boolean fullStats,
            @RequestHeader(required = false, defaultValue = "false", value = "IncludeMetaData") boolean includeMetaData,
            @RequestHeader(required = false, defaultValue = "false", value = "IncludeSteps") boolean includeSteps,
            @RequestHeader(required = false, defaultValue = "",value = "filter") String filter
    ) throws Exception {

        plainResponse = true;

        try {
            integration = integrationRuntime.getIntegration();

            String flowStats = integration.getFlowStats(flowId, fullStats, includeMetaData, includeSteps, filter);
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
    public ResponseEntity<String> getFlowStepStats(
            @PathVariable(value = "flowId") String flowId,
            @PathVariable(value = "stepId") String stepId,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType,
            @RequestHeader(defaultValue = "false", value = "FullStats") boolean fullStats
    ) throws Exception {

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
    public ResponseEntity<String> getFlowMessages(
            @PathVariable(value = "flowId") String flowId,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType,
            @RequestHeader(required = false, defaultValue = "false", value = "IncludeSteps") boolean includeSteps
    ) throws Exception {

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
    public ResponseEntity<String> getFlowTotalMessages(
            @PathVariable(value = "flowId") String flowId,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType
    ) throws Exception {

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
    public ResponseEntity<String> getFlowCompletedMessages(
            @PathVariable(value = "flowId") String flowId,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType
    ) throws Exception {

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
    public ResponseEntity<String> getFlowFailedMessages(
            @PathVariable(value = "flowId") String flowId,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType
    ) throws Exception {

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
    public ResponseEntity<String> getFlowPendingMessages(
            @PathVariable(value = "flowId") String flowId,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType
    ) throws Exception {

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
    public ResponseEntity<String> getStepMessages(
            @PathVariable(value = "flowId") String flowId,
            @PathVariable(value = "stepId") String stepId,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType
    ) throws Exception {

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
    public ResponseEntity<String> getMetrics(@Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType) throws Exception {

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
    public ResponseEntity<String> getHistoryMetrics(@Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType) throws Exception {

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
