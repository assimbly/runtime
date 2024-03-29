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
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.NativeWebRequest;

import javax.servlet.http.HttpServletRequest;
import java.util.List;


/**
 * Resource to return information about the currently running Spring profiles.
 */
@ControllerAdvice
@RestController
@RequestMapping("/api")
public class FlowManagerRuntime {

    protected Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private IntegrationRuntime integrationRuntime;

    private Integration integration;

    private String flowId;

    private boolean plainResponse;

    private String status;

    @Autowired
    private SimpMessageSendingOperations messagingTemplate;

    //manage flows
    @GetMapping(
            path = "/integration/{integrationId}/flow/{flowId}/start",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> startFlow(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @RequestHeader(required=false,defaultValue="3000",value="timeout") long timeout, @PathVariable Long integrationId, @PathVariable String flowId) throws Exception {

        plainResponse = true;

        try {

            integration = integrationRuntime.getIntegration();

            status = integration.startFlow(flowId, timeout);

            //Send message to websocket
            if (this.messagingTemplate != null) {
                this.messagingTemplate.convertAndSend("/topic/" + flowId + "/event", status);
            }

            if(mediaType.equals("application/xml")){
                status = DocConverter.convertJsonToXml(status);
            }

            if (status.contains("successfully")) {
                log.info("FlowManager Report:\n\n" + status);
                return ResponseUtil.createSuccessResponse(integrationId, mediaType,"/integration/{integrationId}/flow/{flowId}/start",status,plainResponse);
            } else {
                log.error("FlowManager Report:\n\n" + status);
                return ResponseUtil.createFailureResponse(integrationId, mediaType, "/integration/{integrationId}/flow/{flowId}/start", status, plainResponse);
            }
        } catch (Exception e) {
            log.error("FlowManager Report:\n\n" + status);
            return ResponseUtil.createFailureResponseWithHeaders(integrationId, mediaType, "/integration/{integrationId}/flow/start/{flowId}", e.getMessage(), "unable to start flow " + flowId, flowId);
        }

    }

    @GetMapping(
            path = "/integration/{integrationId}/flow/{flowId}/stop",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String>  stopFlow(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @RequestHeader(required=false,defaultValue="3000",value="timeout") long timeout, @PathVariable Long integrationId, @PathVariable String flowId) throws Exception {

        plainResponse = true;

        try {

            integration = integrationRuntime.getIntegration();

            status = integration.stopFlow(flowId, timeout);

            if(mediaType.equals("application/xml")){
                status = DocConverter.convertJsonToXml(status);
            }

            //Send message to websocket
            if (this.messagingTemplate != null) {
                this.messagingTemplate.convertAndSend("/topic/" + flowId + "/event", status);
            }

            if (status.contains("Stopped flow successfully")) {
                log.info("FlowManager Report:\n\n" + status);
                return ResponseUtil.createSuccessResponse(integrationId, mediaType,"/integration/{integrationId}/flow/{flowId}/stop",status,plainResponse);
            } else {
                log.error("FlowManager Report:\n\n" + status);
                return ResponseUtil.createFailureResponse(integrationId, mediaType, "/integration/{integrationId}/flow/{flowId}/stop/", status, plainResponse);
            }
        } catch (Exception e) {
            log.error("Stop flow " + flowId + " failed",e);
            return ResponseUtil.createFailureResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/{flowId}/stop",e.getMessage(),"unable to stop flow " + flowId,flowId);
        }

    }

    @GetMapping(
            path = "/integration/{integrationId}/flow/{flowId}/restart",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String>  restartFlow(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @RequestHeader(required=false,defaultValue="3000",value="timeout") long timeout, @PathVariable Long integrationId, @PathVariable String flowId) throws Exception {

        plainResponse = true;

        try {
            integration = integrationRuntime.getIntegration();

            status = integration.restartFlow(flowId, timeout);

            //Send message to websocket
            if (this.messagingTemplate != null) {
                this.messagingTemplate.convertAndSend("/topic/" + flowId + "/event", status);
            }

            if(mediaType.equals("application/xml")){
                status = DocConverter.convertJsonToXml(status);
            }

            if (status.contains("Started flow successfully")) {
                log.info("FlowManager Report:\n\n" + status);
                return ResponseUtil.createSuccessResponse(integrationId, mediaType,"/integration/{integrationId}/flow/{flowId}/restart",status,plainResponse);
            } else {
                log.error("FlowManager Report:\n\n" + status);
                return ResponseUtil.createFailureResponse(integrationId, mediaType, "/integration/{integrationId}/flow/{flowId}/restart", status, plainResponse);
            }
        } catch (Exception e) {
            log.error("Restart flow " + flowId + " failed",e);
            return ResponseUtil.createFailureResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/{flowId}/restart",e.getMessage(),"unable to restart flow " + flowId,flowId);
        }

    }

    @GetMapping(
            path = "/integration/{integrationId}/flow/{flowId}/pause",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String>  pauseFlow(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable String flowId) throws Exception {

        plainResponse = true;

        try {
            integration = integrationRuntime.getIntegration();

            status = integration.pauseFlow(flowId);

            //Send message to websocket
            if (this.messagingTemplate != null) {
                this.messagingTemplate.convertAndSend("/topic/" + flowId + "/event", status);
            }

            if(mediaType.equals("application/xml")){
                status = DocConverter.convertJsonToXml(status);
            }

            if (status.contains("Paused flow successfully")) {
                log.info("FlowManager Report:\n\n" + status);
                return ResponseUtil.createSuccessResponse(integrationId, mediaType,"/integration/{integrationId}/flow/{flowId}/pause",status,plainResponse);
            } else {
                log.error("FlowManager Report:\n\n" + status);
                return ResponseUtil.createFailureResponse(integrationId, mediaType, "/integration/{integrationId}/flow/{flowId}/pause", status, plainResponse);
            }
        } catch (Exception e) {
            log.error("Paused flow " + flowId + " failed",e);
            return ResponseUtil.createFailureResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/{flowId}/pause",e.getMessage(),"unable to pause flow " + flowId,flowId);
        }

    }

    @GetMapping(
            path = "/integration/{integrationId}/flow/{flowId}/resume" ,
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> resumeFlow(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable String flowId) throws Exception {

        plainResponse = true;

        try {
            integration = integrationRuntime.getIntegration();

            status = integration.resumeFlow(flowId);

            //Send message to websocket
            if (this.messagingTemplate != null) {
                this.messagingTemplate.convertAndSend("/topic/" + flowId + "/event", status);
            }

            if(mediaType.equals("application/xml")){
                status = DocConverter.convertJsonToXml(status);
            }

            if (status.contains("Resumed flow successfully")) {
                log.info("FlowManager Report:\n\n" + status);
                return ResponseUtil.createSuccessResponse(integrationId, mediaType,"/integration/{integrationId}/flow/{flowId}/resume",status,plainResponse);
            } else {
                log.error("FlowManager Report:\n\n" + status);
                return ResponseUtil.createFailureResponse(integrationId, mediaType, "/integration/{integrationId}/flow/{flowId}/resume", status, plainResponse);
            }
        } catch (Exception e) {
            log.error("Resume flow " + flowId + " failed",e);
            return ResponseUtil.createFailureResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/{flowId}/resume",e.getMessage(),"unable to resume flow " + flowId,flowId);
        }
    }

    @PostMapping(
            path = "/integration/{integrationId}/flow/{flowId}/routes",
            consumes = {MediaType.APPLICATION_XML_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> flowRoutes(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable String flowId, @RequestBody String configuration) throws Exception {

        try {
            integration = integrationRuntime.getIntegration();

            status = integration.routesFlow(flowId, mediaType, configuration);
            if (status.equals("started")) {
                if (this.messagingTemplate != null) {
                    this.messagingTemplate.convertAndSend("/topic/" + flowId + "/event", "event:started");
                }
                return ResponseUtil.createSuccessResponseWithHeaders(integrationId, mediaType, "/integration/{integrationId}/flow/{flowId}/routes", "started flow " + flowId, "started flow " + flowId, flowId);
            } else {
                throw new Exception(status);
            }
        } catch (Exception e) {
            log.error("Get routes status for flow " + flowId + " failed",e);
            return ResponseUtil.createFailureResponseWithHeaders(integrationId, mediaType, "/integration/{integrationId}/flow/{flowId}/routes", e.getMessage(), "unable to start flow " + flowId, flowId);
        }

    }

    @PostMapping(
            path = "/integration/{integrationId}/route/{routeId}/install",
            consumes =  {MediaType.APPLICATION_XML_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> installRoute(@Parameter(hidden = true) @RequestHeader("Content-Type") String contentType, @Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @RequestHeader(required=false,defaultValue="3000",value="timeout") long timeout, @PathVariable Long integrationId, @PathVariable String routeId, @RequestBody String route) throws Exception {

        plainResponse = true;

        log.info("Install routeId: " + routeId + ". Configuration:\n\n" + route);

        try {
            integration = integrationRuntime.getIntegration();

            status = integration.installRoute(routeId, route);

            if(mediaType.equals("application/xml")){
                status = DocConverter.convertJsonToXml(status);
            }

            if (status.contains("successfully")) {
                log.info("FlowManager Report:\n\n" + status);
                return ResponseUtil.createSuccessResponse(integrationId, mediaType,"/integration/{integrationId}/route/{flowId}/install",status,plainResponse);
            } else {
                log.error("FlowManager Report:\n\n" + status);
                return ResponseUtil.createFailureResponse(integrationId, mediaType, "/integration/{integrationId}/route/{routeId}/install", status, plainResponse);
            }
        } catch (Exception e) {
            log.error("Test flow " + flowId + " failed",e);
            return ResponseUtil.createFailureResponseWithHeaders(integrationId, mediaType, "/integration/{integrationId}/route/{routeId}/install", e.getMessage(), "unable to run route " + routeId, routeId);
        }

    }


    @PostMapping(
            path = "/integration/{integrationId}/flow/{flowId}/install",
            consumes =  {MediaType.APPLICATION_XML_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> installFlow(@Parameter(hidden = true) @RequestHeader("Content-Type") String contentType, @Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @RequestHeader(required=false,defaultValue="3000",value="timeout") long timeout, @PathVariable Long integrationId, @PathVariable String flowId, @RequestBody String configuration) throws Exception {

        plainResponse = true;

        log.info("Install flowId: " + flowId + ". Configuration:\n\n" + configuration);

        try {
            integration = integrationRuntime.getIntegration();

            status = integration.installFlow(flowId, timeout, contentType, configuration);

            //Send message to websocket
            if (this.messagingTemplate != null) {
                this.messagingTemplate.convertAndSend("/topic/" + flowId + "/event", status);
            }

            if(mediaType.equals("application/xml")){
                status = DocConverter.convertJsonToXml(status);
            }

            if (status.contains("Started flow successfully")) {
                log.info("FlowManager Report:\n\n" + status);
                return ResponseUtil.createSuccessResponse(integrationId, mediaType,"/integration/{integrationId}/flow/{flowId}/install",status,plainResponse);
            } else {
                log.error("FlowManager Report:\n\n" + status);
                return ResponseUtil.createFailureResponse(integrationId, mediaType, "/integration/{integrationId}/flow/{flowId}/install", status, plainResponse);
            }
        } catch (Exception e) {
            log.error("Test flow " + flowId + " failed",e);
            return ResponseUtil.createFailureResponseWithHeaders(integrationId, mediaType, "/integration/{integrationId}/flow/{flowId}/install", e.getMessage(), "unable to test flow " + flowId, flowId);
        }

    }

    @DeleteMapping(
            path = "/integration/{integrationId}/flow/{flowId}/uninstall",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> uninstallFlow(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @RequestHeader(required=false,defaultValue="3000",value="timeout") long timeout, @PathVariable Long integrationId, @PathVariable String flowId) throws Exception {

        plainResponse = true;

        try {

            integration = integrationRuntime.getIntegration();

            status = integration.uninstallFlow(flowId, timeout);

            //Send message to websocket
            if (this.messagingTemplate != null) {
                this.messagingTemplate.convertAndSend("/topic/" + flowId + "/event", status);
            }

            if(mediaType.equals("application/xml")){
                status = DocConverter.convertJsonToXml(status);
            }

            if (status.contains("Stopped flow successfully")) {
                log.info("Uninstalled flow " + flowId + " successfully. Report: " + status);
                return ResponseUtil.createSuccessResponse(integrationId, mediaType,"/integration/{integrationId}/flow/{flowId}/uninstall",status,plainResponse);
            } else {
                log.error("FlowManager Report:\n\n" + status);
                return ResponseUtil.createFailureResponse(integrationId, mediaType, "/integration/{integrationId}/flow/{flowId}/uninstall", status, plainResponse);
            }
        } catch (Exception e) {
            log.error("Stop flow " + flowId + " failed",e);
            return ResponseUtil.createFailureResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/{flowId}/uninstall",e.getMessage(),"unable to stop flow " + flowId,flowId);
        }

    }

    @PostMapping(
            path = "/integration/{integrationId}/flow/{flowId}/install/file",
            consumes = {MediaType.APPLICATION_XML_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> fileInstallFlow(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable String flowId, @RequestBody String configuration) throws Exception {

        try {
            integration = integrationRuntime.getIntegration();

            status = integration.fileInstallFlow(flowId, configuration);

            if (status.equals("saved")) {
                log.info("FlowManager Report:\n\n" + status);
                return ResponseUtil.createSuccessResponseWithHeaders(integrationId, mediaType, "/integration/{integrationId}/flow/{flowId}/install/file", "flow " + flowId + " saved in the deploy directory", "flow " + flowId + " saved in the deploy directory", flowId);
            } else {
                log.error("FlowManager Report:\n\n" + status);
                throw new Exception(status);
            }
        } catch (Exception e) {
            log.error("FileInstall flow " + flowId + " failed",e);
            return ResponseUtil.createFailureResponseWithHeaders(integrationId, mediaType, "/integration/{integrationId}/flow/{flowId}/install/file", e.getMessage(), "unable to save flow " + flowId, flowId);
        }

    }

    @DeleteMapping(
            path = "/integration/{integrationId}/flow/{flowId}/uninstall/file",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> fileUninstallFlow(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable String flowId) throws Exception {

        try {
            integration = integrationRuntime.getIntegration();

            status = integration.fileUninstallFlow(flowId);

            if (status.equals("deleted")) {
                log.info("FlowManager Report:\n\n" + status);
                return ResponseUtil.createSuccessResponseWithHeaders(integrationId, mediaType, "/integration/{integrationId}/flow/{flowId}/uninstall/file", "flow " + flowId + " deleted from deploy directory", "flow " + flowId + " deleted from the deploy directory", flowId);
            } else {
                log.error("FlowManager Report:\n\n" + status);
                throw new Exception(status);
            }
        } catch (Exception e) {
            log.error("FileUnstall flow " + flowId + " failed",e);
            return ResponseUtil.createFailureResponseWithHeaders(integrationId, mediaType, "/integration/{integrationId}/flow/{flowId}/uninstall/file", e.getMessage(), "unable to save flow " + flowId, flowId);
        }

    }


    @GetMapping(
            path = "/integration/{integrationId}/flow/{flowId}/isstarted",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> isFlowStarted(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable String flowId) throws Exception {

        try {
            //integrationResource.init();
            integration = integrationRuntime.getIntegration();

            boolean started = integration.isFlowStarted(flowId);
            String isStarted = Boolean.toString(started);
            return ResponseUtil.createSuccessResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/{flowId}/isstarted",isStarted,isStarted,flowId);
        } catch (Exception e) {
            log.error("Get if flow " + flowId + " is started failed",e);
            return ResponseUtil.createFailureResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/{flowId}/isstarted",e.getMessage(),"unable to get status for flow " + flowId,flowId);
        }

    }

    @GetMapping(
            path = "/integration/{integrationId}/flow/{flowId}/info",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> getFlowInfo(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable String flowId) throws Exception {

        plainResponse = true;

        try {
            integration = integrationRuntime.getIntegration();
            String info = integration.getFlowInfo(flowId, mediaType);
            return ResponseUtil.createSuccessResponse(integrationId, mediaType,"/integration/{integrationId}/flow/{flowId}/info",info,true);
        } catch (Exception e) {
            log.error("Get status of flow " + flowId + " failed",e);
            return ResponseUtil.createFailureResponse(integrationId, mediaType,"/integration/{integrationId}/flow/{flowId}/info",e.getMessage(),false);
        }

    }

    @GetMapping(
            path = "/integration/{integrationId}/flow/{flowId}/status",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> getFlowStatus(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable String flowId) throws Exception {

        try {
            integration = integrationRuntime.getIntegration();

            status = integration.getFlowStatus(flowId);
            return ResponseUtil.createSuccessResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/{flowId}/status",status,status,flowId);
        } catch (Exception e) {
            log.error("Get status of flow " + flowId + " failed",e);
            return ResponseUtil.createFailureResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/{flowId}/status",e.getMessage(),"unable to get status for flow " + flowId,flowId);
        }

    }

    @GetMapping(
            path = "/integration/{integrationId}/flow/{flowId}/uptime",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> getFlowUptime(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable String flowId) throws Exception {

        try {
            //integrationResource.init();
            integration = integrationRuntime.getIntegration();

            String uptime = integration.getFlowUptime(flowId);
            return ResponseUtil.createSuccessResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/{flowId}/uptime",uptime,uptime,flowId);
        } catch (Exception e) {
            log.error("Get uptime of " + flowId + " failed",e);
            return ResponseUtil.createFailureResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/{flowId}/uptime",e.getMessage(),"unable to get uptime flow " + flowId,flowId);
        }

    }

    @GetMapping(
            path = "/integration/{integrationId}/flow/{flowId}/lasterror",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> getFlowLastError(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable String flowId) throws Exception {

        try {
            //integrationResource.init();
            integration = integrationRuntime.getIntegration();

            String lastError = integration.getFlowLastError(flowId);
            return ResponseUtil.createSuccessResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/{flowId}/lasterror",lastError,lastError,flowId);
        } catch (Exception e) {
            log.error("Get last error of flow " + flowId + " failed",e);
            return ResponseUtil.createFailureResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/{flowId}/lasterror",e.getMessage(),"unable to get last error for flow " + flowId,flowId);
        }
    }

    @GetMapping(
            path = "/integration/{integrationId}/flow/{flowId}/alerts",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> getFlowAlertsLog(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable String flowId) throws Exception {

        try {
            integration = integrationRuntime.getIntegration();

            String log = integration.getFlowAlertsLog(flowId,100);
            return ResponseUtil.createSuccessResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/{flowId}/alerts",log,log,flowId);
        } catch (Exception e) {
            log.error("Get alerts for flow " + flowId + " failed",e);
            return ResponseUtil.createFailureResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/{flowId}/alerts",e.getMessage(),"unable to get failed log of flow" + flowId,flowId);
        }
    }

    @GetMapping(
            path = "/integration/{integrationId}/flow/{flowId}/alerts/count",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> getFlowNumberOfAlerts(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable String flowId) throws Exception {

        try {
            integration = integrationRuntime.getIntegration();
            String numberOfEntries = integration.getFlowAlertsCount(flowId);

            return ResponseUtil.createSuccessResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/{flowId}/alerts/count",numberOfEntries,numberOfEntries,flowId);
        } catch (Exception e) {
            log.error("Get number of alerts for flow " + flowId + " failed",e);
            return ResponseUtil.createFailureResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/{flowId}/alerts/count",e.getMessage(),"unable to get failed entries of flow log" + flowId,flowId);
        }
    }

    @GetMapping(
            path = "/integration/{integrationId}/flow/{flowId}/events",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> getFlowEvents(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable String flowId) throws Exception {

        try {
            integration = integrationRuntime.getIntegration();
            String log = integration.getFlowEventsLog(flowId,100);
            return ResponseUtil.createSuccessResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/{flowId}/events",log,log,flowId);
        } catch (Exception e) {
            log.error("Get events log for flow " + flowId + " failed",e);

            return ResponseUtil.createFailureResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/{flowId}/events",e.getMessage(),"unable to get event log of flow " + flowId,flowId);
        }
    }

    @PostMapping(
            path = "/integration/{integrationId}/flow/maintenance/{time}",
            consumes = {MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> setMaintenance(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @RequestHeader(required=false,defaultValue="3000",value="timeout") long timeout, @PathVariable Long integrationId, @PathVariable Long time, @RequestBody List<String> ids) throws Exception {

        try {

            //pass spring variable into new Thread (outside of Spring context)
            final SimpMessageSendingOperations messagingTemplate2 = messagingTemplate;

            integration = integrationRuntime.getIntegration();

            Thread thread = new Thread(new Runnable()
            {

                private SimpMessageSendingOperations messagingTemplate = messagingTemplate2;

                public void run()
                {

                    try {
                        for(String id : ids) {
                            flowId = id;
                            status = integration.getFlowStatus(flowId);
                            if(status.equals("started")) {
                                String report = integration.pauseFlow(flowId);
                                status = integration.getFlowStatus(flowId);
                                if(status.equals("suspended") || status.equals("stopped")) {
                                    if(this.messagingTemplate!=null) {
                                        this.messagingTemplate.convertAndSend("/topic/" + flowId + "/event",report);
                                    }
                                }else {
                                    throw new Exception(status);
                                }
                            }
                        }

                        Thread.sleep(time);

                        for(String id : ids) {

                            flowId = id;
                            status = integration.getFlowStatus(flowId);
                            if(status.equals("suspended")) {
                                String report = integration.startFlow(flowId, timeout);
                                if(this.messagingTemplate!=null) {
                                    this.messagingTemplate.convertAndSend("/topic/" + flowId + "/event",report);
                                }
                            }
                        }

                    } catch (Exception e) {
                        log.error("Set maintenance failed",e);
                    }
                }
            });

            // start the thread
            thread.start();

            return ResponseUtil.createSuccessResponse(integrationId, mediaType,"/integration/{integrationId}/flow/maintenance/{time}","Set flows into maintenance mode for " + time + " miliseconds");
        } catch (Exception e) {
            log.error("Set maintenance failed",e);
            return ResponseUtil.createFailureResponse(integrationId, mediaType,"/integration/{integrationId}/flow/maintenance/{time}",e.getMessage());
        }
    }


    // Generates a generic error response (exceptions outside try catch):
    @ExceptionHandler({Exception.class})
    public ResponseEntity<String> integrationErrorHandler(Exception error, NativeWebRequest request) throws Exception {

        log.error("IntegrationErrorHandler", error);

        Long integrationId = 0L; // set integrationid to 0, as we may get a string value
        String mediaType = request.getNativeRequest(HttpServletRequest.class).getHeader("ACCEPT");
        String path = request.getNativeRequest(HttpServletRequest.class).getRequestURI();
        String message = error.getMessage();

        return ResponseUtil.createFailureResponse(integrationId, mediaType,path,message);
    }

}
