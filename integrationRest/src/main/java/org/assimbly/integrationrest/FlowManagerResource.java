package org.assimbly.integrationrest;

import io.swagger.v3.oas.annotations.Parameter;
import org.assimbly.docconverter.DocConverter;
import org.assimbly.integration.Integration;
import org.assimbly.util.rest.ResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.NativeWebRequest;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.TreeMap;

/**
 * Resource to return information about the currently running Spring profiles.
 */
@ControllerAdvice
@RestController
@RequestMapping("/api")
public class FlowManagerResource {

	protected Logger log = LoggerFactory.getLogger(getClass());
	
    @Autowired
    private IntegrationResource integrationResource;

    private Integration integration;

    private String flowId;

    private boolean plainResponse;

    private String status;

    @Autowired
    private SimpMessageSendingOperations messagingTemplate;

    //manage flows
    @GetMapping(path = "/integration/{integrationId}/flow/start/{flowId}", produces = {"application/xml","application/json","text/plain"})
    public ResponseEntity<String> startflow(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable String flowId) throws Exception {

        plainResponse = true;

        try {

            integration = integrationResource.getIntegration();

            status = integration.startFlow(flowId);

            if(mediaType.equals("application/xml")){
                status = DocConverter.convertJsonToXml(status);
            }

            //Send message to websocket
            if (this.messagingTemplate != null) {
                this.messagingTemplate.convertAndSend("/topic/" + flowId + "/event", status);
            }

            if (status.contains("Started flow successfully")) {
                return ResponseUtil.createSuccessResponse(integrationId, mediaType,"/integration/{integrationId}/flow/start/{flowId}",status,plainResponse);
            } else {
                log.error("Start flow " + flowId + " failed. Status: " + status);
                return ResponseUtil.createFailureResponse(integrationId, mediaType, "/integration/{integrationId}/flow/start/{flowId}", status, plainResponse);
            }
        } catch (Exception e) {
            log.error("Start flow " + flowId + " failed",e);
            return ResponseUtil.createFailureResponseWithHeaders(integrationId, mediaType, "/integration/{integrationId}/flow/start/{flowId}", e.getMessage(), "unable to start flow " + flowId, flowId);
        }

    }

    @GetMapping(path = "/integration/{integrationId}/flow/stop/{flowId}", produces = {"application/xml","application/json","text/plain"})
    public ResponseEntity<String>  stopflow(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable String flowId) throws Exception {

        plainResponse = true;

        try {

            integration = integrationResource.getIntegration();

            status = integration.stopFlow(flowId);

            if(mediaType.equals("application/xml")){
                status = DocConverter.convertJsonToXml(status);
            }

            //Send message to websocket
            if (this.messagingTemplate != null) {
                this.messagingTemplate.convertAndSend("/topic/" + flowId + "/event", status);
            }

            if (status.contains("Stopped flow successfully")) {
                return ResponseUtil.createSuccessResponse(integrationId, mediaType,"/integration/{integrationId}/flow/stop/{flowId}",status,plainResponse);
            } else {
                log.error("Stop flow " + flowId + " failed. Status: " + status);
                return ResponseUtil.createFailureResponse(integrationId, mediaType, "/integration/{integrationId}/flow/stop/{flowId}", status, plainResponse);
            }
        } catch (Exception e) {
            log.error("Stop flow " + flowId + " failed",e);
            return ResponseUtil.createFailureResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/stop/{flowId}",e.getMessage(),"unable to stop flow " + flowId,flowId);
        }

    }

    @GetMapping(path = "/integration/{integrationId}/flow/restart/{flowId}", produces = {"application/xml","application/json","text/plain"})
    public ResponseEntity<String>  restartflow(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable String flowId) throws Exception {

        plainResponse = true;

        try {
            integration = integrationResource.getIntegration();

            status = integration.restartFlow(flowId);

            if(mediaType.equals("application/xml")){
                status = DocConverter.convertJsonToXml(status);
            }

            //Send message to websocket
            if (this.messagingTemplate != null) {
                this.messagingTemplate.convertAndSend("/topic/" + flowId + "/event", status);
            }

            if (status.contains("Started flow successfully")) {
                return ResponseUtil.createSuccessResponse(integrationId, mediaType,"/integration/{integrationId}/flow/restart/{flowId}",status,plainResponse);
            } else {
                log.error("Restart flow " + flowId + " failed. Status: " + status);
                return ResponseUtil.createFailureResponse(integrationId, mediaType, "/integration/{integrationId}/flow/restart/{flowId}", status, plainResponse);
            }
        } catch (Exception e) {
            log.error("Restart flow " + flowId + " failed",e);
            return ResponseUtil.createFailureResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/restart/{flowId}",e.getMessage(),"unable to restart flow " + flowId,flowId);
        }

    }

    @GetMapping(path = "/integration/{integrationId}/flow/pause/{flowId}", produces = {"application/xml","application/json","text/plain"})
    public ResponseEntity<String>  pauseflow(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable String flowId) throws Exception {

        plainResponse = true;

        try {
            integration = integrationResource.getIntegration();

            status = integration.pauseFlow(flowId);

            if(mediaType.equals("application/xml")){
                status = DocConverter.convertJsonToXml(status);
            }

            //Send message to websocket
            if (this.messagingTemplate != null) {
                this.messagingTemplate.convertAndSend("/topic/" + flowId + "/event", status);
            }

            if (status.contains("Paused flow successfully")) {
                return ResponseUtil.createSuccessResponse(integrationId, mediaType,"/integration/{integrationId}/flow/pause/{flowId}",status,plainResponse);
            } else {
                log.error("Pause flow " + flowId + " failed. Status: " + status);
                return ResponseUtil.createFailureResponse(integrationId, mediaType, "/integration/{integrationId}/flow/pause/{flowId}", status, plainResponse);
            }
        } catch (Exception e) {
            log.error("Paused flow " + flowId + " failed",e);
            return ResponseUtil.createFailureResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/pause/{flowId}",e.getMessage(),"unable to pause flow " + flowId,flowId);
        }

    }

    @GetMapping(path = "/integration/{integrationId}/flow/resume/{flowId}" , produces = {"application/xml","application/json","text/plain"})
    public ResponseEntity<String> resumeflow(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable String flowId) throws Exception {

        plainResponse = true;

        try {
            integration = integrationResource.getIntegration();

            status = integration.resumeFlow(flowId);

            if(mediaType.equals("application/xml")){
                status = DocConverter.convertJsonToXml(status);
            }

            //Send message to websocket
            if (this.messagingTemplate != null) {
                this.messagingTemplate.convertAndSend("/topic/" + flowId + "/event", status);
            }

            if (status.contains("Resumed flow successfully")) {
                return ResponseUtil.createSuccessResponse(integrationId, mediaType,"/integration/{integrationId}/flow/resume/{flowId}",status,plainResponse);
            } else {
                log.error("Resume flow " + flowId + " failed. Status: " + status);
                return ResponseUtil.createFailureResponse(integrationId, mediaType, "/integration/{integrationId}/flow/resume/{flowId}", status, plainResponse);
            }
        } catch (Exception e) {
            log.error("Resume flow " + flowId + " failed",e);
            return ResponseUtil.createFailureResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/resume/{flowId}",e.getMessage(),"unable to resume flow " + flowId,flowId);
        }
    }

	
	@PostMapping(path = "/integration/{integrationId}/flow/test/{flowId}", consumes =  {"application/xml"}, produces = {"application/xml","application/json","text/plain"})
    public ResponseEntity<String> testflow(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @RequestHeader(value = "StopTest", defaultValue = "false") boolean stopTest, @PathVariable Long integrationId, @PathVariable String flowId, @RequestBody String configuration) throws Exception {

        plainResponse = true;

        try {
            integration = integrationResource.getIntegration();

            status = integration.testFlow(flowId, mediaType, configuration, stopTest);

            if(mediaType.equals("application/xml")){
                status = DocConverter.convertJsonToXml(status);
            }

            //Send message to websocket
            if (this.messagingTemplate != null) {
                this.messagingTemplate.convertAndSend("/topic/" + flowId + "/event", status);
            }

            if (status.equals("started") || status.equals("stopped")) {
                return ResponseUtil.createSuccessResponse(integrationId, mediaType,"/integration/{integrationId}/flow/test/{flowId}",status,plainResponse);
            } else {
                log.error("Test flow " + flowId + " failed. Status: " + status);
                return ResponseUtil.createFailureResponse(integrationId, mediaType, "/integration/{integrationId}/flow/test/{flowId}", status, plainResponse);
            }
        } catch (Exception e) {
            log.error("Test flow " + flowId + " failed",e);
            return ResponseUtil.createFailureResponseWithHeaders(integrationId, mediaType, "/integration/{integrationId}/flow/test/{flowId}", e.getMessage(), "unable to test flow " + flowId, flowId);
        }

    }

	
	@PostMapping(path = "/integration/{integrationId}/flow/routes/{flowId}", consumes =  {"application/xml"}, produces = {"application/xml","application/json","text/plain"})
    public ResponseEntity<String> flowRoutes(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable String flowId, @RequestBody String configuration) throws Exception {

        try {
            integration = integrationResource.getIntegration();

            status = integration.routesFlow(flowId, mediaType, configuration);
            if (status.equals("started")) {
                if (this.messagingTemplate != null) {
                    this.messagingTemplate.convertAndSend("/topic/" + flowId + "/event", "event:started");
                }
                return ResponseUtil.createSuccessResponseWithHeaders(integrationId, mediaType, "/integration/{integrationId}/flow/routes/{flowId}", "started flow " + flowId, "started flow " + flowId, flowId);
            } else {
                throw new Exception(status);
            }
        } catch (Exception e) {
            log.error("Get routes status for flow " + flowId + " failed",e);
            return ResponseUtil.createFailureResponseWithHeaders(integrationId, mediaType, "/integration/{integrationId}/flow/routes/{flowId}", e.getMessage(), "unable to start flow " + flowId, flowId);
        }

    }

	@PostMapping(path = "/integration/{integrationId}/flow/fileinstall/{flowId}", consumes =  {"application/xml"}, produces = {"application/xml","application/json","text/plain"})
    public ResponseEntity<String> fileInstallFlow(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable String flowId, @RequestBody String configuration) throws Exception {

        try {
            integration = integrationResource.getIntegration();

            status = integration.fileInstallFlow(flowId, mediaType, configuration);
            if (status.equals("saved")) {
                return ResponseUtil.createSuccessResponseWithHeaders(integrationId, mediaType, "/integration/{integrationId}/flow/fileinstall/{flowId}", "flow " + flowId + " saved in the deploy directory", "flow " + flowId + " saved in the deploy directory", flowId);
            } else {
                throw new Exception(status);
            }
        } catch (Exception e) {
            log.error("FileInstall flow " + flowId + " failed",e);
            return ResponseUtil.createFailureResponseWithHeaders(integrationId, mediaType, "/integration/{integrationId}/flow/fileinstall/{flowId}", e.getMessage(), "unable to save flow " + flowId, flowId);
        }

    }	

	@GetMapping(path = "/integration/{integrationId}/flow/fileuninstall/{flowId}", produces = {"application/xml","application/json","text/plain"})
    public ResponseEntity<String> fileUninstallFlow(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable String flowId) throws Exception {

        try {
            integration = integrationResource.getIntegration();

            status = integration.fileUninstallFlow(flowId, mediaType);
            if (status.equals("deleted")) {
                return ResponseUtil.createSuccessResponseWithHeaders(integrationId, mediaType, "/integration/{integrationId}/flow/fileuninstall/{flowId}", "flow " + flowId + " deleted from deploy directory", "flow " + flowId + " deleted from the deploy directory", flowId);
            } else {
                throw new Exception(status);
            }
        } catch (Exception e) {
            log.error("FileUnstall flow " + flowId + " failed",e);
            return ResponseUtil.createFailureResponseWithHeaders(integrationId, mediaType, "/integration/{integrationId}/flow/fileinstall/{flowId}", e.getMessage(), "unable to save flow " + flowId, flowId);
        }

    }	


    @GetMapping(path = "/integration/{integrationId}/flow/isstarted/{flowId}", produces = {"application/xml","application/json","text/plain"})
    public ResponseEntity<String> isFlowStarted(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable String flowId) throws Exception {

        try {
            //integrationResource.init();
            integration = integrationResource.getIntegration();
   
            boolean started = integration.isFlowStarted(flowId);
            String isStarted = Boolean.toString(started);
            return ResponseUtil.createSuccessResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/status/{flowId}",isStarted,isStarted,flowId);
        } catch (Exception e) {
            log.error("Get if flow " + flowId + " is started failed",e);
            return ResponseUtil.createFailureResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/status/{flowId}",e.getMessage(),"unable to get status for flow " + flowId,flowId);
        }

    }

    @GetMapping(path = "/integration/{integrationId}/flow/info/{flowId}", produces = {"application/xml","application/json","text/plain"})
    public ResponseEntity<String> getFlowInfo(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable String flowId) throws Exception {

        plainResponse = true;

        try {
            integration = integrationResource.getIntegration();
            String info = integration.getFlowInfo(flowId, mediaType);
            return ResponseUtil.createSuccessResponse(integrationId, mediaType,"/integration/{integrationId}/flow/info/{flowId}",info,true);
        } catch (Exception e) {
            log.error("Get status of flow " + flowId + " failed",e);
            return ResponseUtil.createFailureResponse(integrationId, mediaType,"/integration/{integrationId}/flow/info/{flowId}",e.getMessage(),false);
        }

    }

    @GetMapping(path = "/integration/{integrationId}/flow/status/{flowId}", produces = {"application/xml","application/json","text/plain"})
    public ResponseEntity<String> getFlowStatus(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable String flowId) throws Exception {

        try {
            integration = integrationResource.getIntegration();

            status = integration.getFlowStatus(flowId);
            return ResponseUtil.createSuccessResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/status/{flowId}",status,status,flowId);
        } catch (Exception e) {
            log.error("Get status of flow " + flowId + " failed",e);
            return ResponseUtil.createFailureResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/status/{flowId}",e.getMessage(),"unable to get status for flow " + flowId,flowId);
        }

    }

    @GetMapping(path = "/integration/{integrationId}/flow/uptime/{flowId}", produces = {"application/xml","application/json","text/plain"})
    public ResponseEntity<String> getFlowUptime(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable String flowId) throws Exception {

        try {
            //integrationResource.init();
            integration = integrationResource.getIntegration();

            String uptime = integration.getFlowUptime(flowId);
            return ResponseUtil.createSuccessResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/uptime/{flowId}",uptime,uptime,flowId);
        } catch (Exception e) {
            log.error("Get uptime of " + flowId + " failed",e);
            return ResponseUtil.createFailureResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/uptime/{flowId}",e.getMessage(),"unable to get uptime flow " + flowId,flowId);
        }

    }

  @GetMapping(path = "/integration/{integrationId}/hasflow/{flowId}", produces = {"application/xml","application/json","text/plain"})
  public ResponseEntity<String> hasFlow(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable String flowId) throws Exception {

		try {
            integration = integrationResource.getIntegration();
            Boolean hasFlow = integration.hasFlow(flowId);
			return ResponseUtil.createSuccessResponse(integrationId, mediaType,"/integration/{integrationId}/hasflow/{flowId}",hasFlow.toString());
		} catch (Exception e) {
            log.error("Check if integration " + integrationId + " has flow " + flowId + " failed",e);
            return ResponseUtil.createFailureResponse(integrationId, mediaType,"/integration/{integrationId}/hasflow/{flowId}",e.getMessage());
		}

   }

    @GetMapping(path = "/integration/{integrationId}/flow/lasterror/{flowId}", produces = {"application/xml","application/json","text/plain"})
    public ResponseEntity<String> getFlowLastError(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable String flowId) throws Exception {

		try {
            //integrationResource.init();
            integration = integrationResource.getIntegration();

    		String lastError = integration.getFlowLastError(flowId);
			return ResponseUtil.createSuccessResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/lasterror/{flowId}",lastError,lastError,flowId);
		} catch (Exception e) {
            log.error("Get last error of flow " + flowId + " failed",e);
            return ResponseUtil.createFailureResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/lasterror/{flowId}",e.getMessage(),"unable to get last error for flow " + flowId,flowId);
		}
    }

    @GetMapping(path = "/integration/{integrationId}/flow/alerts/{flowId}", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> getFlowAlertsLog(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable String flowId) throws Exception {

		try {
            integration = integrationResource.getIntegration();

            String log = integration.getFlowAlertsLog(flowId,100);
			return ResponseUtil.createSuccessResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/failedlog/{flowId}",log,log,flowId);
		} catch (Exception e) {
            log.error("Get alerts log for flow " + flowId + " failed",e);
            return ResponseUtil.createFailureResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/failedmessages/{flowId}",e.getMessage(),"unable to get failed log of flow" + flowId,flowId);
		}
    }

    @GetMapping(path = "/integration/{integrationId}/flow/numberofalerts/{flowId}", produces = {"application/xml","application/json","text/plain"})
    public ResponseEntity<String> getFlowNumberOfAlerts(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable String flowId) throws Exception {

		try {
        	integration = integrationResource.getIntegration();
            String numberOfEntries = integration.getFlowAlertsCount(flowId);

            return ResponseUtil.createSuccessResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/failedlogentries/{flowId}",numberOfEntries,numberOfEntries,flowId);
		} catch (Exception e) {
            log.error("Get number of alerts for flow " + flowId + " failed",e);
            return ResponseUtil.createFailureResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/failedlogentries/{flowId}",e.getMessage(),"unable to get failed entries of flow log" + flowId,flowId);
		}
    }

    @GetMapping(path = "/integration/{integrationId}/numberofalerts", produces = {"application/xml","application/json","text/plain"})
    public ResponseEntity<String> getIntegrationNumberOfAlerts(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId) throws Exception {

		try {
            integration = integrationResource.getIntegration();
            TreeMap<String,String> numberOfEntriesList = integration.getIntegrationAlertsCount();

			return ResponseUtil.createSuccessResponse(integrationId, mediaType,"/integration/{integrationId}/flow/failedlog}",numberOfEntriesList.toString());
		} catch (Exception e) {
            log.error("Get number of alerts for integration " + integrationId + " failed",e);
            return ResponseUtil.createFailureResponse(integrationId, mediaType,"/integration/{integrationId}/flow/failedmessages",e.getMessage());
		}
    }

    @GetMapping(path = "/integration/{integrationId}/flow/eventlog/{flowId}", produces = {"application/xml","application/json","text/plain"})
    public ResponseEntity<String> getFlowEventLog(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable String flowId) throws Exception {

		try {
            integration = integrationResource.getIntegration();
            String log = integration.getFlowEventsLog(flowId,100);
			return ResponseUtil.createSuccessResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/eventlog/{flowId}",log,log,flowId);
		} catch (Exception e) {
            log.error("Get events log for flow " + flowId + " failed",e);

            return ResponseUtil.createFailureResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/eventlog/{flowId}",e.getMessage(),"unable to get event log of flow " + flowId,flowId);
		}
    }

    @PostMapping(path = "/integration/{integrationId}/maintenance/{time}", consumes = {"application/json"}, produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> setMaintenance(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable Long time, @RequestBody List<String> ids) throws Exception {

        try {

            //pass spring variable into new Thread (outside of Spring context)
            final SimpMessageSendingOperations messagingTemplate2 = messagingTemplate;

            integration = integrationResource.getIntegration();

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
                                status = integration.pauseFlow(flowId);
                                if(status.equals("suspended") || status.equals("stopped")) {
                                    if(this.messagingTemplate!=null) {
                                        this.messagingTemplate.convertAndSend("/topic/" + flowId + "/event","event:suspended");
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
                                status = integration.startFlow(flowId);
                                if(status.equals("started") && this.messagingTemplate!=null) {
                                    this.messagingTemplate.convertAndSend("/topic/" + flowId + "/event","event:resumed");
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

            return ResponseUtil.createSuccessResponse(integrationId, mediaType,"/integration/{integrationId}/maintenance/{time}","Set flows into maintenance mode for " + time + " miliseconds");
        } catch (Exception e) {
            log.error("Set maintenance failed",e);
            return ResponseUtil.createFailureResponse(integrationId, mediaType,"/integration/{integrationId}/maintenance/{time}",e.getMessage());
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

}
