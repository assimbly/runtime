package org.assimbly.integrationrest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.servlet.http.HttpServletRequest;
import org.assimbly.docconverter.DocConverter;
import org.assimbly.integration.Integration;
import org.assimbly.util.rest.ResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.NativeWebRequest;


@ControllerAdvice
@RestController
@RequestMapping("/api")
public class FlowManagerRuntime {

    protected Logger log = LoggerFactory.getLogger(getClass());

    private String flowId;

    private final Integration integration;

    public FlowManagerRuntime(IntegrationRuntime integrationRuntime) {
        this.integration = integrationRuntime.getIntegration();
    }
    
    //manage flows
    @GetMapping(
            path = "/integration/flow/{flowId}/start",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> startFlow(
            @PathVariable(value = "flowId") String flowId,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType,
            @RequestHeader(required = false, defaultValue = "3000", value = "timeout") long timeout
    ) {

        String report = integration.startFlow(flowId, timeout);

        return response(mediaType, "start",report);

    }

    @GetMapping(
            path = "/integration/flow/{flowId}/stop",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String>  stopFlow(
            @PathVariable(value = "flowId") String flowId,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType,
            @RequestHeader(required = false, defaultValue = "3000", value = "timeout") long timeout
    ) {

        String report = integration.stopFlow(flowId, timeout);

        return response(mediaType, "stop",report);

    }

    @GetMapping(
            path = "/integration/flow/{flowId}/restart",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String>  restartFlow(
            @PathVariable(value = "flowId") String flowId,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType,
            @RequestHeader(required = false, defaultValue = "3000", value = "timeout") long timeout
    ) {

        String report = integration.restartFlow(flowId, timeout);

        return response(mediaType, "restart",report);

    }

    @GetMapping(
            path = "/integration/flow/{flowId}/pause",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String>  pauseFlow(
            @PathVariable(value = "flowId") String flowId,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType
    ) {

        String report = integration.pauseFlow(flowId);

        return response(mediaType, "pause",report);

    }

    @GetMapping(
            path = "/integration/flow/{flowId}/resume" ,
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> resumeFlow(
            @PathVariable(value = "flowId") String flowId,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType
    ) {

        String report = integration.resumeFlow(flowId);

        return response(mediaType, "resume",report);

    }

    @PostMapping(
            path = "/integration/route/{routeId}/install",
            consumes =  {MediaType.APPLICATION_XML_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> installRoute(
            @PathVariable(value = "routeId") String routeId,
            @RequestBody String route,
            @Parameter(hidden = true) @RequestHeader(value = "Content-Type") String contentType,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType
    ) {

        log.info("Install routeId: {}. Configuration:\n\n{}", routeId, route);

        try {

            if(contentType.equals("application/json")){
                route = DocConverter.convertJsonToXml(route);
            }

            String report = integration.installRoute(routeId, route);

            if(mediaType.equals("application/xml")){
                report = DocConverter.convertJsonToXml(report);
            }

            if (report.contains("successfully")) {
                return ResponseUtil.createSuccessResponse(1L, mediaType,"/integration/route/{flowId}/install",report,true);
            } else {
                log.error("FlowManager Report:\n\n{}", report);
                return ResponseUtil.createFailureResponse(1L, mediaType, "/integration/route/{routeId}/install", report, true);
            }

        } catch (Exception e) {
            log.error("Test flow {} failed", flowId, e);
            return ResponseUtil.createFailureResponseWithHeaders(1L, mediaType, "/integration/route/{routeId}/install", e.getMessage(), "unable to run route " + routeId, routeId);
        }

    }

    @Schema(description = "Flows", example = "")
    @PostMapping(
            path = "/integration/flow/{flowId}/install",
            consumes =  {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> installFlow(
            @PathVariable(value = "flowId") String flowId,
            @RequestBody String configuration,
            @Parameter(hidden = true) @RequestHeader(value = "Content-Type") String contentType,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType,
            @RequestHeader(required = false, defaultValue = "3000", value = "timeout") long timeout
    ) {

        log.info("Install flowId: {}. Configuration:\n\n{}", flowId, configuration);

        String report = integration.installFlow(flowId, timeout, contentType, configuration);

        return response(mediaType, "install",report);

    }

    @DeleteMapping(
            path = "/integration/flow/{flowId}/uninstall",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> uninstallFlow(
            @PathVariable(value = "flowId") String flowId,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType,
            @RequestHeader(required = false, defaultValue = "3000", value = "timeout") long timeout
    ) {

        String report = integration.uninstallFlow(flowId, timeout);

        return response(mediaType, "uninstall",report);

    }

    @Schema(description = "Flows", example = "")
    @PostMapping(
            path = "/integration/flow/{flowId}/test",
            consumes =  {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> testFlow(
            @PathVariable(value = "flowId") String flowId,
            @RequestBody String configuration,
            @Parameter(hidden = true) @RequestHeader(value = "Content-Type") String contentType,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType,
            @RequestHeader(required = false, defaultValue = "3000", value = "timeout") long timeout
    ) {

        log.info("Test flowId: {}. Configuration:\n\n{}", flowId, configuration);

        String report = integration.testFlow(flowId, timeout, contentType, configuration);

        return response(mediaType, "test",report);


    }

    @GetMapping(
            path = "/integration/flow/{flowId}/isstarted",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> isFlowStarted(
            @PathVariable(value = "flowId") String flowId,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType
    ) {

        try {
            boolean started = integration.isFlowStarted(flowId);
            String isStarted = Boolean.toString(started);
            return ResponseUtil.createSuccessResponseWithHeaders(1L, mediaType,"/integration/flow/{flowId}/isstarted",isStarted,isStarted,flowId);
        } catch (Exception e) {
            log.error("Get if flow {} is started failed", flowId, e);
            return ResponseUtil.createFailureResponseWithHeaders(1L, mediaType,"/integration/flow/{flowId}/isstarted",e.getMessage(),"unable to get report for flow " + flowId,flowId);
        }

    }

    @GetMapping(
            path = "/integration/flow/{flowId}/info",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> getFlowInfo(
            @PathVariable(value = "flowId") String flowId,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType
    ) {

        log.info("Get flow info for flowId: {}", flowId);

        try {

            String info = integration.getFlowInfo(flowId, mediaType);
            return ResponseUtil.createSuccessResponse(1L, mediaType,"/integration/flow/{flowId}/info",info,true);
        } catch (Exception e) {
            log.error("Get report of flow {} failed", flowId, e);
            return ResponseUtil.createFailureResponse(1L, mediaType,"/integration/flow/{flowId}/info",e.getMessage(),false);
        }

    }

    @GetMapping(
            path = "/integration/flow/{flowId}/uptime",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> getFlowUptime(
            @PathVariable(value = "flowId") String flowId,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType
    ) {

        try {
            String uptime = integration.getFlowUptime(flowId);
            return ResponseUtil.createSuccessResponseWithHeaders(1L, mediaType,"/integration/flow/{flowId}/uptime",uptime,uptime,flowId);
        } catch (Exception e) {
            log.error("Get uptime of {} failed", flowId, e);
            return ResponseUtil.createFailureResponseWithHeaders(1L, mediaType,"/integration/flow/{flowId}/uptime",e.getMessage(),"unable to get uptime flow " + flowId,flowId);
        }

    }

    @GetMapping(
            path = "/integration/flow/{flowId}/lasterror",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> getFlowLastError(
            @PathVariable(value = "flowId") String flowId,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType
    ) {

        try {
            String lastError = integration.getFlowLastError(flowId);
            return ResponseUtil.createSuccessResponseWithHeaders(1L, mediaType,"/integration/flow/{flowId}/lasterror",lastError,lastError,flowId);
        } catch (Exception e) {
            log.error("Get last error of flow {} failed", flowId, e);
            return ResponseUtil.createFailureResponseWithHeaders(1L, mediaType,"/integration/flow/{flowId}/lasterror",e.getMessage(),"unable to get last error for flow " + flowId,flowId);
        }
    }

    @GetMapping(
            path = "/integration/flow/{flowId}/alerts",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> getFlowAlertsLog(
            @PathVariable(value = "flowId") String flowId,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType
    ) {

        try {
            String alertsLog = integration.getFlowAlertsLog(flowId);
            return ResponseUtil.createSuccessResponseWithHeaders(1L, mediaType,"/integration/flow/{flowId}/alerts",alertsLog,alertsLog,flowId);
        } catch (Exception e) {
            log.error("Get alerts for flow {} failed", flowId, e);
            return ResponseUtil.createFailureResponseWithHeaders(1L, mediaType,"/integration/flow/{flowId}/alerts",e.getMessage(),"unable to get failed log of flow" + flowId,flowId);
        }
    }

    @GetMapping(
            path = "/integration/flow/{flowId}/alerts/count",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> getFlowNumberOfAlerts(
            @PathVariable(value = "flowId") String flowId,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType
    ) {

        try {

            String numberOfEntries = Long.toString(integration.getFlowAlertsCount(flowId));

            return ResponseUtil.createSuccessResponseWithHeaders(1L, mediaType,"/integration/flow/{flowId}/alerts/count",numberOfEntries,numberOfEntries,flowId);
        } catch (Exception e) {
            log.error("Get number of alerts for flow {} failed", flowId, e);
            return ResponseUtil.createFailureResponseWithHeaders(1L, mediaType,"/integration/flow/{flowId}/alerts/count",e.getMessage(),"unable to get failed entries of flow log" + flowId,flowId);
        }
    }

    @GetMapping(
            path = "/integration/flow/{flowId}/events",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> getFlowEvents(
            @PathVariable(value = "flowId") String flowId,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType
    ) {

        try {
            String eventsLog = integration.getFlowEventsLog(flowId,100);
            return ResponseUtil.createSuccessResponseWithHeaders(1L, mediaType,"/integration/flow/{flowId}/events",eventsLog,eventsLog,flowId);
        } catch (Exception e) {
            log.error("Get events log for flow {} failed", flowId, e);
            return ResponseUtil.createFailureResponseWithHeaders(1L, mediaType,"/integration/flow/{flowId}/events",e.getMessage(),"unable to get event log of flow " + flowId,flowId);
        }
    }

    @GetMapping(
            path = "/integration/flow/{flowId}/status",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> getFlowStatus(
            @PathVariable(value = "flowId") String flowId,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType
    ) {

        try {
            String status = integration.getFlowStatus(flowId);
            return ResponseUtil.createSuccessResponseWithHeaders(1L, mediaType,"/integration/flow/{flowId}/status",status,status,flowId);
        } catch (Exception e) {
            log.error("Get status of flow {} failed", flowId, e);
            return ResponseUtil.createFailureResponseWithHeaders(1L, mediaType,"/integration/flow/{flowId}/status",e.getMessage(),"unable to get status for flow " + flowId,flowId);
        }

    }

    // Generates a generic error response (exceptions outside try catch):
    @ExceptionHandler({Exception.class})
    public ResponseEntity<String> integrationErrorHandler(Exception error, NativeWebRequest request) {

        log.error("IntegrationErrorHandler", error);

        HttpServletRequest httpServletRequest = request.getNativeRequest(HttpServletRequest.class);
        String mediaType = httpServletRequest != null ? httpServletRequest.getHeader("ACCEPT") : "application/json";
        String path = httpServletRequest != null ? httpServletRequest.getRequestURI() : "unknown";
        String message = error.getMessage();

        return ResponseUtil.createFailureResponse(1L, mediaType,path,message);
    }
    private ResponseEntity<String> response(String mediaType, String endpoint, String response) {

        String path = "/integration/flow/{flowId}/" + endpoint;

        try {

            boolean isuccessfully = isSuccessResponse(response);

            if (mediaType.equals("application/xml")) {
                response = DocConverter.convertJsonToXml(response);
            }

            if (isuccessfully) {
                return ResponseUtil.createSuccessResponse(1L, mediaType, path, response, true);
            }

        }catch (Exception e) {
            log.error("FlowManager Report failed:\n\n{}", response);
            return ResponseUtil.createFailureResponse(1L, mediaType, path, response, true);
        }

        log.error("FlowManager Report:\n\n{}", response);
        return ResponseUtil.createFailureResponse(1L, mediaType, path, response, true);

    }

    private boolean isSuccessResponse(String response) throws JsonProcessingException {

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(response);

        String status = root.path("flow").path("status").asText();

        return status.equals("success");

    }

}
