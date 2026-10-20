package org.assimbly.integrationrest;

import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.assimbly.docconverter.DocConverter;
import org.assimbly.integration.Integration;
import org.assimbly.util.rest.ResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;


/**
 * Resource to return information about the stats of flows.
 */
@Tag(name = "Statistics", description = "Statistics, exchanges, transactions and metrics")
@ControllerAdvice
@RestController
@RequestMapping("/api")
public class StatisticsRuntime {

    protected Logger log = LoggerFactory.getLogger(getClass());

    private boolean plainResponse;

    private final Integration integration;

    public StatisticsRuntime(IntegrationRuntime integrationRuntime) {
        this.integration = integrationRuntime.getIntegration();
    }

    //statistics of integrations, flows and steps

    @Operation(summary = "Get statistics")
    @GetMapping(
            path = "/integration/stats",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> getStats(@Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType) {

        plainResponse = true;
        
        try {
            String stats = integration.getStats(mediaType);
            if(stats.startsWith("Error")||stats.startsWith("Warning")) {plainResponse = false;}
            return ResponseUtil.createSuccessResponse(1L, mediaType,"/integration/stats",stats,plainResponse);
        } catch (Exception e) {
            log.error("Get stats failed",e);
            return ResponseUtil.createFailureResponse(1L, mediaType,"/integration/stats",e.getMessage());
        }
    }

    @Operation(summary = "Get statistics per step")
    @GetMapping(
            path = "/integration/stats/steps",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> getRoutesStats(@Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType) {

        plainResponse = true;

        try {
            String stats = integration.getStepsStats(mediaType);
            if(stats.startsWith("Error")||stats.startsWith("Warning")) {plainResponse = false;}
            return ResponseUtil.createSuccessResponse(1L, mediaType,"/integration/stats/steps",stats,plainResponse);
        } catch (Exception e) {
            log.error("Get stats failed",e);
            return ResponseUtil.createFailureResponse(1L, mediaType,"/integration/stats/steps",e.getMessage());
        }
    }

    @Operation(summary = "Get statistics per flow")
    @GetMapping(
            path = "/integration/stats/flows",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> getFlowsStats(
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType,
            @RequestHeader(required = false, defaultValue = "true", value = "FullStats") boolean fullStats
    ) {

        plainResponse = true;

        try {
            String stats = integration.getFlowsStats(fullStats, mediaType);
            if(stats.startsWith("Error")||stats.startsWith("Warning")) {plainResponse = false;}
            return ResponseUtil.createSuccessResponse(1L, mediaType,"/integration/stats/flows",stats,plainResponse);
        } catch (Exception e) {
            log.error("Get stats failed",e);
            return ResponseUtil.createFailureResponse(1L, mediaType,"/integration/stats/flows",e.getMessage());
        }
    }

    @Operation(summary = "Get statistics of the given flows")
    @PostMapping(
            path = "/integration/statsbyflowids",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> getStatsByFlowIds(
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType,
            @RequestBody String flowIds,
            @RequestHeader(required = false, defaultValue = "", value = "filter") String filter
    ) {

        plainResponse = true;

        try {
            String stats = integration.getStatsByFlowIds(flowIds, filter, mediaType);

            if(mediaType.contains("xml")) {
                stats = DocConverter.jsonToXml(stats);
            }

            if(stats.startsWith("Error")||stats.startsWith("Warning")) {plainResponse = false;}
            return ResponseUtil.createSuccessResponse(1L, mediaType,"/integration/statsbyflowids", stats, plainResponse);
        } catch (Exception e) {
            log.error("Get stats by flow ids failed",e);
            return ResponseUtil.createFailureResponse(1L, mediaType,"/integration/statsbyflowids",e.getMessage());
        }
    }

    @Operation(summary = "Reset all statistics")
    @PostMapping(
            path = "/integration/stats/reset",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> resetStats(
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType
    ) {

        try {

            log.info("Reset statistics for all flows");

            integration.resetStats();

            return ResponseUtil.createSuccessResponse(1L, mediaType,"/integration/stats/reset", "success", false);
        } catch (Exception e) {
            log.error("Reset stats for all flows failed", e);
            return ResponseUtil.createFailureResponse(1L, mediaType,"/integration/stats/reset",e.getMessage());
        }

    }

    @Operation(summary = "Reset the statistics of a flow")
    @PostMapping(
            path = "/integration/flow/{flowId}/stats/reset",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> resetFlowStats(
            @PathVariable(value = "flowId") String flowId,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType
    ) {

        try {

            log.info("Reset statistics for flow {}", flowId);

            integration.resetFlowStats(flowId);

            return ResponseUtil.createSuccessResponse(1L, mediaType,"/integration/flow/{flowId}/stats/reset", "success", false);
        } catch (Exception e) {
            log.error("Reset stats for flow {} failed", flowId, e);
            return ResponseUtil.createFailureResponse(1L, mediaType,"/integration/flow/{flowId}/stats/reset",e.getMessage());
        }

    }

    @Operation(summary = "Get statistics of a flow")
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
    ) {

        plainResponse = true;

        try {

            String stats = integration.getFlowStats(flowId, fullStats, includeMetaData, includeSteps, filter);

            if(mediaType.contains("xml")) {
                stats = DocConverter.jsonToXml(stats);
            }

            if(stats.startsWith("Error")||stats.startsWith("Warning")) {plainResponse = false;}
            return ResponseUtil.createSuccessResponse(1L, mediaType,"/integration/flow/{flowId}/stats", stats, plainResponse);
        } catch (Exception e) {
            log.error("Get flowstats {} failed", flowId, e);
            return ResponseUtil.createFailureResponse(1L, mediaType,"/integration/flow/{flowId}/stats",e.getMessage());
        }
    }

    @Operation(summary = "Get statistics of a step")
    @GetMapping(
            path = "/integration/flow/{flowId}/step/{stepId}/stats",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> getFlowStepStats(
            @PathVariable(value = "flowId") String flowId,
            @PathVariable(value = "stepId") String stepId,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType,
            @RequestHeader(defaultValue = "false", value = "FullStats") boolean fullStats
    ) {

        plainResponse = true;

        try {

            String flowStepStats = integration.getFlowStepStats(flowId, stepId, fullStats);
            if(flowStepStats.startsWith("Error")||flowStepStats.startsWith("Warning")) {plainResponse = false;}

            if (mediaType.contains("xml")) {
                flowStepStats = DocConverter.jsonToXml(flowStepStats);
            }
            return ResponseUtil.createSuccessResponse(1L, mediaType,"/integration/flow/{flowId}/step/{stepId}/stats",flowStepStats,plainResponse);
        } catch (Exception e) {
            log.error("Get flowstats {} for stepId={} failed", flowId, stepId, e);
            return ResponseUtil.createFailureResponse(1L, mediaType,"/integration/flow/{flowId}/step/{stepId}/stats",e.getMessage());
        }
    }

    @Operation(summary = "Get the exchanges of a flow (each message counted once per step)")
    @GetMapping(
            path = "/integration/flow/{flowId}/exchanges",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> getFlowExchanges(
            @PathVariable(value = "flowId") String flowId,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType,
            @RequestHeader(required = false, defaultValue = "false", value = "IncludeSteps") boolean includeSteps
    ) {

        try {
            String exchanges = integration.getFlowExchanges(flowId, includeSteps, mediaType);
            return ResponseUtil.createSuccessResponse(1L, mediaType,"/integration/flow/{flowId}/exchanges",exchanges,true);
        } catch (Exception e) {
            log.error("Get exchanges for flow {} failed", flowId, e);
            return ResponseUtil.createFailureResponseWithHeaders(1L, mediaType,"/integration/flow/{flowId}/exchanges",e.getMessage(),"unable to get exchanges of flow " + flowId,flowId);
        }
    }

    @Operation(summary = "Get the transactions of a flow (each message counted once)")
    @GetMapping(
            path = "/integration/flow/{flowId}/transactions",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> getFlowTransactions(
            @PathVariable(value = "flowId") String flowId,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType
    ) {

        try {
            String transactions = integration.getFlowTransactions(flowId, mediaType);
            return ResponseUtil.createSuccessResponse(1L, mediaType,"/integration/flow/{flowId}/transactions",transactions,true);
        } catch (Exception e) {
            log.error("Get transactions for flow {} failed", flowId, e);
            return ResponseUtil.createFailureResponseWithHeaders(1L, mediaType,"/integration/flow/{flowId}/transactions",e.getMessage(),"unable to get transactions of flow " + flowId,flowId);
        }
    }

    @Operation(summary = "Count all exchanges of a flow")
    @GetMapping(
            path = "/integration/flow/{flowId}/exchanges/total",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> getFlowTotalExchanges(
            @PathVariable(value = "flowId") String flowId,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType
    ) {

        try {
            String totalExchanges = integration.getFlowTotalExchanges(flowId);
            return ResponseUtil.createSuccessResponseWithHeaders(1L, mediaType,"/integration/flow/{flowId}/exchanges/total",totalExchanges,totalExchanges,flowId);
        } catch (Exception e) {
            log.error("Get total exchanges for flow {} failed", flowId, e);
            return ResponseUtil.createFailureResponseWithHeaders(1L, mediaType,"/integration/flow/{flowId}/exchanges/total",e.getMessage(),"unable to get total exchanges of flow " + flowId,flowId);
        }
    }

    @Operation(summary = "Count completed exchanges of a flow")
    @GetMapping(
            path = "/integration/flow/{flowId}/exchanges/completed",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> getFlowCompletedExchanges(
            @PathVariable(value = "flowId") String flowId,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType
    ) {

        try {
            String completedExchanges = integration.getFlowCompletedExchanges(flowId);
            return ResponseUtil.createSuccessResponseWithHeaders(1L, mediaType,"/integration/flow/{flowId}/exchanges/completed",completedExchanges,completedExchanges,flowId);
        } catch (Exception e) {
            log.error("Get completed exchanges for flow {} failed", flowId, e);
            return ResponseUtil.createFailureResponseWithHeaders(1L, mediaType,"/integration/flow/{flowId}/exchanges/completed",e.getMessage(),"unable to get completed exchanges of flow " + flowId,flowId);
        }
    }

    @Operation(summary = "Count failed exchanges of a flow")
    @GetMapping(
            path = "/integration/flow/{flowId}/exchanges/failed",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> getFlowFailedExchanges(
            @PathVariable(value = "flowId") String flowId,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType
    ) {

        try {
            String failedExchanges = integration.getFlowFailedExchanges(flowId);
            return ResponseUtil.createSuccessResponseWithHeaders(1L, mediaType,"/integration/flow/{flowId}/exchanges/failed",failedExchanges,failedExchanges,flowId);
        } catch (Exception e) {
            log.error("Get failed exchanges for flow {} failed", flowId, e);
            return ResponseUtil.createFailureResponseWithHeaders(1L, mediaType,"/integration/flow/{flowId}/exchanges/failed",e.getMessage(),"unable to get failed exchanges of flow " + flowId,flowId);
        }
    }

    @Operation(summary = "Count pending exchanges of a flow")
    @GetMapping(
            path = "/integration/flow/{flowId}/exchanges/pending",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> getFlowPendingExchanges(
            @PathVariable(value = "flowId") String flowId,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType
    ) {

        try {
            String pendingExchanges = integration.getFlowPendingExchanges(flowId);
            return ResponseUtil.createSuccessResponseWithHeaders(1L, mediaType,"/integration/flow/{flowId}/exchanges/pending",pendingExchanges,pendingExchanges,flowId);
        } catch (Exception e) {
            log.error("Get pending exchanges for flow {} failed", flowId, e);
            return ResponseUtil.createFailureResponseWithHeaders(1L, mediaType,"/integration/flow/{flowId}/exchanges/pending",e.getMessage(),"unable to get pending exchanges of flow " + flowId,flowId);
        }
    }

    @Operation(summary = "Count all transactions of a flow")
    @GetMapping(
            path = "/integration/flow/{flowId}/transactions/total",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> getFlowTotalTransactions(
            @PathVariable(value = "flowId") String flowId,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType
    ) {

        try {
            String totalTransactions = integration.getFlowTotalTransactions(flowId);
            return ResponseUtil.createSuccessResponseWithHeaders(1L, mediaType,"/integration/flow/{flowId}/transactions/total",totalTransactions,totalTransactions,flowId);
        } catch (Exception e) {
            log.error("Get total transactions for flow {} failed", flowId, e);
            return ResponseUtil.createFailureResponseWithHeaders(1L, mediaType,"/integration/flow/{flowId}/transactions/total",e.getMessage(),"unable to get total transactions of flow " + flowId,flowId);
        }
    }

    @Operation(summary = "Count completed transactions of a flow")
    @GetMapping(
            path = "/integration/flow/{flowId}/transactions/completed",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> getFlowCompletedTransactions(
            @PathVariable(value = "flowId") String flowId,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType
    ) {

        try {
            String completedTransactions = integration.getFlowCompletedTransactions(flowId);
            return ResponseUtil.createSuccessResponseWithHeaders(1L, mediaType,"/integration/flow/{flowId}/transactions/completed",completedTransactions,completedTransactions,flowId);
        } catch (Exception e) {
            log.error("Get completed transactions for flow {} failed", flowId, e);
            return ResponseUtil.createFailureResponseWithHeaders(1L, mediaType,"/integration/flow/{flowId}/transactions/completed",e.getMessage(),"unable to get completed transactions of flow " + flowId,flowId);
        }
    }

    @Operation(summary = "Count failed transactions of a flow")
    @GetMapping(
            path = "/integration/flow/{flowId}/transactions/failed",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> getFlowFailedTransactions(
            @PathVariable(value = "flowId") String flowId,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType
    ) {

        try {
            String failedTransactions = integration.getFlowFailedTransactions(flowId);
            return ResponseUtil.createSuccessResponseWithHeaders(1L, mediaType,"/integration/flow/{flowId}/transactions/failed",failedTransactions,failedTransactions,flowId);
        } catch (Exception e) {
            log.error("Get failed transactions for flow {} failed", flowId, e);
            return ResponseUtil.createFailureResponseWithHeaders(1L, mediaType,"/integration/flow/{flowId}/transactions/failed",e.getMessage(),"unable to get failed transactions of flow " + flowId,flowId);
        }
    }

    @Operation(summary = "Count pending transactions of a flow")
    @GetMapping(
            path = "/integration/flow/{flowId}/transactions/pending",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> getFlowPendingTransactions(
            @PathVariable(value = "flowId") String flowId,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType
    ) {

        try {
            String pendingTransactions = integration.getFlowPendingTransactions(flowId);
            return ResponseUtil.createSuccessResponseWithHeaders(1L, mediaType,"/integration/flow/{flowId}/transactions/pending",pendingTransactions,pendingTransactions,flowId);
        } catch (Exception e) {
            log.error("Get pending transactions for flow {} failed", flowId, e);
            return ResponseUtil.createFailureResponseWithHeaders(1L, mediaType,"/integration/flow/{flowId}/transactions/pending",e.getMessage(),"unable to get pending transactions of flow " + flowId,flowId);
        }
    }

    @Operation(summary = "Get the exchanges of a step")
    @GetMapping(
            path = "/integration/flow/{flowId}/step/{stepId}/exchanges",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> getStepExchanges(
            @PathVariable(value = "flowId") String flowId,
            @PathVariable(value = "stepId") String stepId,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType
    ) {

        try {
            String exchanges = integration.getStepExchanges(flowId, stepId, mediaType);
            return ResponseUtil.createSuccessResponse(1L, mediaType,"/integration/flow/{flowId}/step/{stepId}/exchanges",exchanges,true);
        } catch (Exception e) {
            log.error("Get exchanges for flow {} for step {} failed", flowId, stepId, e);
            return ResponseUtil.createFailureResponseWithHeaders(1L, mediaType,"/integration/flow/{flowId}/step/{stepId}/exchanges",e.getMessage(),"unable to get exchanges of step " + stepId + " of flow " + flowId,flowId);
        }

    }

    @Operation(summary = "Get metrics")
    @GetMapping(
            path = "/integration/metrics",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> getMetrics(@Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType) {

        plainResponse = true;
        

        try {
            String metrics = integration.getMetrics(mediaType);
            if(metrics.startsWith("Error")||metrics.startsWith("Warning")) {plainResponse = false;}
            return ResponseUtil.createSuccessResponse(1L, mediaType,"/integration/metrics",metrics,plainResponse);
        } catch (Exception e) {
            log.error("Get metrics failed",e);
            return ResponseUtil.createFailureResponse(1L, mediaType,"/integration/metrics",e.getMessage());
        }
    }

    @Operation(summary = "Get metrics history")
    @GetMapping(
            path = "/integration/historymetrics",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> getHistoryMetrics(@Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType) {

        plainResponse = true;

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