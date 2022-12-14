package org.assimbly.integrationrest;

import io.swagger.v3.oas.annotations.Parameter;
import org.assimbly.integration.Integration;
import org.assimbly.util.rest.ResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


/**
 * Resource to return information about the currently running Spring profiles.
 */
@ControllerAdvice
@RestController
@RequestMapping("/api")
public class StatisticsResource {

    protected Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private IntegrationResource integrationResource;

    private Integration integration;

    private boolean plainResponse;


    public StatisticsResource() throws Exception {
    }

    //statistics of integrations, flows and steps

    @GetMapping(path = "/integration/{integrationId}/stats", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> getStats(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId) throws Exception {

        plainResponse = true;
        integration = integrationResource.getIntegration();

        try {
            String stats = integration.getStats(mediaType);
            if(stats.startsWith("Error")||stats.startsWith("Warning")) {plainResponse = false;}
            return ResponseUtil.createSuccessResponse(integrationId, mediaType,"/integration/{integrationId}/stats",stats,plainResponse);
        } catch (Exception e) {
            log.error("Get stats failed",e);
            return ResponseUtil.createFailureResponse(integrationId, mediaType,"/integration/{integrationId}/stats",e.getMessage());
        }
    }

    @GetMapping(path = "/integration/{integrationId}/statsbyflowids", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> getStatsByFlowIds(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @RequestHeader String flowIds, @PathVariable Long integrationId) throws Exception {

        plainResponse = true;
        integration = integrationResource.getIntegration();

        try {
            String stats = integration.getStatsByFlowIds(flowIds, mediaType);
            if(stats.startsWith("Error")||stats.startsWith("Warning")) {plainResponse = false;}
            return ResponseUtil.createSuccessResponse(integrationId, mediaType,"/integration/{integrationId}/statsbyflowids",stats,plainResponse);
        } catch (Exception e) {
            log.error("Get stats failed",e);
            return ResponseUtil.createFailureResponse(integrationId, mediaType,"/integration/{integrationId}/statsbyflowids",e.getMessage());
        }
    }

    @GetMapping(path = "/integration/{integrationId}/flow/stats/{flowId}", produces = {"application/xml","application/json","text/plain"})
    public ResponseEntity<String> getFlowStats(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @RequestHeader("FullStats") boolean fullStats, @PathVariable Long integrationId, @PathVariable String flowId) throws Exception {

        plainResponse = true;

        try {
            //integrationResource.init();
            integration = integrationResource.getIntegration();

            String flowStats = integration.getFlowStats(flowId, fullStats, mediaType);
            if(flowStats.startsWith("Error")||flowStats.startsWith("Warning")) {plainResponse = false;}
            return ResponseUtil.createSuccessResponse(integrationId, mediaType,"/integration/{integrationId}/flow/stats/{flowId}",flowStats,plainResponse);
        } catch (Exception e) {
            log.error("Get flowstats " + flowId + " failed",e);
            return ResponseUtil.createFailureResponse(integrationId, mediaType,"/integration/{integrationId}/flow/stats/{flowId}",e.getMessage());
        }
    }

    @GetMapping(path = "/integration/{integrationId}/flow/stats/{flowId}/step/{stepId}", produces = {"application/xml","application/json","text/plain"})
    public ResponseEntity<String> getFlowStepStats(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @RequestHeader("FullStats") boolean fullStats, @PathVariable Long integrationId, @PathVariable String flowId, @PathVariable String stepId) throws Exception {

        plainResponse = true;

        try {
            integration = integrationResource.getIntegration();

            String flowStats = integration.getFlowStepStats(flowId, stepId, fullStats, mediaType);
            if(flowStats.startsWith("Error")||flowStats.startsWith("Warning")) {plainResponse = false;}
            return ResponseUtil.createSuccessResponse(integrationId, mediaType,"/integration/{integrationId}/flow/stats/{flowId}",flowStats,plainResponse);
        } catch (Exception e) {
            log.error("Get flowstats " + flowId + " with stepId=" + stepId + " failed",e);
            return ResponseUtil.createFailureResponse(integrationId, mediaType,"/integration/{integrationId}/flow/stats/{flowId}",e.getMessage());
        }
    }

    @GetMapping(path = "/integration/{integrationId}/flow/totalmessages/{flowId}", produces = {"application/xml","application/json","text/plain"})
    public ResponseEntity<String> getFlowTotalMessages(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable String flowId) throws Exception {

        try {
            //integrationResource.init();
            integration = integrationResource.getIntegration();

            String numberOfMessages = integration.getFlowTotalMessages(flowId);
            return ResponseUtil.createSuccessResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/totalmessages/{flowId}",numberOfMessages,numberOfMessages,flowId);
        } catch (Exception e) {
            log.error("Get total messages for flow " + flowId + " failed",e);
            return ResponseUtil.createFailureResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/totalmessages/{flowId}",e.getMessage(),"unable to get total messages of flow " + flowId,flowId);
        }
    }

    @GetMapping(path = "/integration/{integrationId}/flow/completedmessages/{flowId}", produces = {"application/xml","application/json","text/plain"})
    public ResponseEntity<String> getFlowCompletedMessages(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable String flowId) throws Exception {

        try {
            //integrationResource.init();
            integration = integrationResource.getIntegration();

            String completedMessages = integration.getFlowCompletedMessages(flowId);
            return ResponseUtil.createSuccessResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/completedmessages/{flowId}",completedMessages,completedMessages,flowId);
        } catch (Exception e) {
            log.error("Get completed messages for flow " + flowId + " failed",e);
            return ResponseUtil.createFailureResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/completedmessages/{flowId}",e.getMessage(),"unable to get completed messages of flow " + flowId,flowId);
        }
    }

    @GetMapping(path = "/integration/{integrationId}/flow/failedmessages/{flowId}", produces = {"application/xml","application/json","text/plain"})
    public ResponseEntity<String> getFlowFailedMessages(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable String flowId) throws Exception {

        try {
            ////integrationResource.init();
            integration = integrationResource.getIntegration();

            String failedMessages = integration.getFlowFailedMessages(flowId);
            return ResponseUtil.createSuccessResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/failedmessages/{flowId}",failedMessages,failedMessages,flowId);
        } catch (Exception e) {
            log.error("Get failed messages for flow " + flowId + " failed",e);
            return ResponseUtil.createFailureResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/failedmessages/{flowId}",e.getMessage(),"unable to get failed messages of flow " + flowId,flowId);
        }
    }

    @GetMapping(path = "/integration/{integrationId}/metrics", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> getMetrics(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId) throws Exception {

        plainResponse = true;
        integration = integrationResource.getIntegration();

        try {
            String metrics = integration.getMetrics(mediaType);
            if(metrics.startsWith("Error")||metrics.startsWith("Warning")) {plainResponse = false;}
            return ResponseUtil.createSuccessResponse(integrationId, mediaType,"/integration/{integrationId}/metrics",metrics,plainResponse);
        } catch (Exception e) {
            log.error("Get metrics failed",e);
            return ResponseUtil.createFailureResponse(integrationId, mediaType,"/integration/{integrationId}/metrics",e.getMessage());
        }
    }

    @GetMapping(path = "/integration/{integrationId}/historymetrics", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> getHistoryMetrics(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId) throws Exception {

        plainResponse = true;
        integration = integrationResource.getIntegration();

        try {
            String metrics = integration.getHistoryMetrics(mediaType);
            if(metrics.startsWith("Error")||metrics.startsWith("Warning")) {plainResponse = false;}
            return ResponseUtil.createSuccessResponse(integrationId, mediaType,"/integration/{integrationId}/historymetrics",metrics,plainResponse);
        } catch (Exception e) {
            log.error("Get history metrics failed",e);
            return ResponseUtil.createFailureResponse(integrationId, mediaType,"/integration/{integrationId}/historymetrics",e.getMessage());
        }
    }

}
