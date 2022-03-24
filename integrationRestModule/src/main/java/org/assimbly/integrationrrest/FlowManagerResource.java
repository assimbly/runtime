package org.assimbly.integrationrest;

import io.swagger.v3.oas.annotations.Parameter;
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
import java.util.Optional;
import java.util.TreeMap;

/**
 * Resource to return information about the currently running Spring profiles.
 */
@ControllerAdvice
@RestController
@RequestMapping("/api")
public class FlowManagerResource {

    private final Logger log = LoggerFactory.getLogger(FlowManagerResource.class);

    @Autowired
    private IntegrationResource integrationResource;

    Integration integration;

    private String flowId;
    private String endpointId;

    private boolean plainResponse;

    private String status;

    @Autowired
    private SimpMessageSendingOperations messagingTemplate;

    //manage flows
    @GetMapping(path = "/integration/{integrationId}/flow/start/{flowId}", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> startflow(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable String flowId) throws Exception {

        try {
            ////integrationResource.init();
            integration = integrationResource.getIntegration();

            status = integration.startFlow(flowId);
            if (status.equals("started")) {
                if (this.messagingTemplate != null) {
					System.out.println("sending to topic start");	
                    this.messagingTemplate.convertAndSend("/topic/" + flowId + "/event", "event:started");
                }
                return ResponseUtil.createSuccessResponseWithHeaders(integrationId, mediaType, "/integration/{integrationId}/flow/start/{flowId}", "started flow " + flowId, "started flow " + flowId, flowId);
            } else {
                throw new Exception(status);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseUtil.createFailureResponseWithHeaders(integrationId, mediaType, "/integration/{integrationId}/flow/start/{flowId}", e.getMessage(), "unable to start flow " + flowId, flowId);
        }

    }

    @GetMapping(path = "/integration/{integrationId}/flow/stop/{flowId}", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String>  stopflow(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable String flowId) throws Exception {

        try {
            ////integrationResource.init();
            integration = integrationResource.getIntegration();

            status = integration.stopFlow(flowId);
            if(status.equals("stopped")) {
                if(this.messagingTemplate!=null) {
					System.out.println("sending to topic stop");	
                    this.messagingTemplate.convertAndSend("/topic/" + flowId + "/event","event:stopped");
                }
                return ResponseUtil.createSuccessResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/stop/{flowId}","stopped flow " + flowId,"stopped flow " + flowId,flowId);
            }else {
                throw new Exception(status);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseUtil.createFailureResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/stop/{flowId}",e.getMessage(),"unable to stop flow " + flowId,flowId);
        }

    }

    @GetMapping(path = "/integration/{integrationId}/flow/restart/{flowId}", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String>  restartflow(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable String flowId) throws Exception {

        try {
            ////integrationResource.init();
            integration = integrationResource.getIntegration();

            status = integration.restartFlow(flowId);
            if(status.equals("started")) {
                if(this.messagingTemplate!=null) {
                    this.messagingTemplate.convertAndSend("/topic/" + flowId + "/event","event:restarted");
                }
                return ResponseUtil.createSuccessResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/restart/{flowId}","restarted","restarted flow " + flowId,flowId);
            }else {
                throw new Exception(status);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseUtil.createFailureResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/restart/{flowId}",e.getMessage(),"unable to restart flow " + flowId,flowId);
        }

    }

    @GetMapping(path = "/integration/{integrationId}/flow/pause/{flowId}", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String>  pauseflow(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable String flowDd) throws Exception {

        try {
            ////integrationResource.init();
            integration = integrationResource.getIntegration();

            status = integration.pauseFlow(flowId);
            if(status.equals("suspended") || status.equals("stopped")) {
                if(this.messagingTemplate!=null) {
                    this.messagingTemplate.convertAndSend("/topic/" + flowId + "/event","event:suspended");
                }
                return ResponseUtil.createSuccessResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/pause/{flowId}","paused","paused flow " + flowId,flowId);
            }else {
                throw new Exception(status);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseUtil.createFailureResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/pause/{flowId}",e.getMessage(),"unable to pause flow " + flowId,flowId);
        }

    }

    @GetMapping(path = "/integration/{integrationId}/flow/resume/{flowId}" , produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> resumeflow(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable String flowId) throws Exception {

        try {
            ////integrationResource.init();
            integration = integrationResource.getIntegration();

            status = integration.resumeFlow(flowId);
            if(status.equals("started")) {
                if(this.messagingTemplate!=null) {
                    this.messagingTemplate.convertAndSend("/topic/" + flowId + "/event","event:resumed");
                }
                return ResponseUtil.createSuccessResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/resume/{flowId}","resumed","resumed flow " + flowId,flowId);
            }else {
                throw new Exception(status);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseUtil.createFailureResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/resume/{flowId}",e.getMessage(),"unable to resume flow " + flowId,flowId);
        }
    }


    @GetMapping(path = "/integration/{integrationId}/flow/isstarted/{flowId}", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> isFlowStarted(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable String flowId) throws Exception {

        try {
            //integrationResource.init();
            integration = integrationResource.getIntegration();
   
            boolean started = integration.isFlowStarted(flowId);
            String isStarted = Boolean.toString(started);
            return ResponseUtil.createSuccessResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/status/{flowId}",isStarted,isStarted,flowId);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseUtil.createFailureResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/status/{flowId}",e.getMessage(),"unable to get status for flow " + flowId,flowId);
        }

    }


    @GetMapping(path = "/integration/{integrationId}/flow/status/{flowId}", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> getFlowStatus(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable String flowId) throws Exception {

        try {
            //integrationResource.init();
            integration = integrationResource.getIntegration();

            status = integration.getFlowStatus(flowId);
            return ResponseUtil.createSuccessResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/status/{flowId}",status,status,flowId);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseUtil.createFailureResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/status/{flowId}",e.getMessage(),"unable to get status for flow " + flowId,flowId);
        }

    }

    @GetMapping(path = "/integration/{integrationId}/flow/uptime/{flowId}", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> getFlowUptime(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable String flowId) throws Exception {

        try {
            //integrationResource.init();
            integration = integrationResource.getIntegration();

            String uptime = integration.getFlowUptime(flowId);
            return ResponseUtil.createSuccessResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/uptime/{flowId}",uptime,uptime,flowId);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseUtil.createFailureResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/uptime/{flowId}",e.getMessage(),"unable to get uptime flow " + flowId,flowId);
        }

    }

  @GetMapping(path = "/integration/{integrationId}/hasflow/{flowId}", produces = {"text/plain","application/xml","application/json"})
  public ResponseEntity<String> hasFlow(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable String flowId) throws Exception {

		try {
            integration = integrationResource.getIntegration();
            Boolean hasFlow = integration.hasFlow(flowId);
			return ResponseUtil.createSuccessResponse(integrationId, mediaType,"/integration/{integrationId}/hasflow/{flowId}",hasFlow.toString());
		} catch (Exception e) {
   			e.printStackTrace();
			return ResponseUtil.createFailureResponse(integrationId, mediaType,"/integration/{integrationId}/hasflow/{flowId}",e.getMessage());
		}

   }


    @GetMapping(path = "/integration/{integrationId}/flow/stats/{flowId}/{endpointid}", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> getFlowStats(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable String flowId, @PathVariable Long endpointid) throws Exception {

        plainResponse = true;

        try {
            //integrationResource.init();
            integration = integrationResource.getIntegration();

            endpointId = endpointid.toString();

            String flowStats = integration.getFlowStats(flowId, endpointId, mediaType);
            if(flowStats.startsWith("Error")||flowStats.startsWith("Warning")) {plainResponse = false;}
            return ResponseUtil.createSuccessResponse(integrationId, mediaType,"/integration/{integrationId}/flow/stats/{flowId}",flowStats,plainResponse);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseUtil.createFailureResponse(integrationId, mediaType,"/integration/{integrationId}/flow/stats/{flowId}",e.getMessage());
        }
    }

    @GetMapping(path = "/integration/{integrationId}/flow/lasterror/{flowId}", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> getFlowLastError(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable String flowId) throws Exception {

		try {
            //integrationResource.init();
            integration = integrationResource.getIntegration();

    		String lastError = integration.getFlowLastError(flowId);
			return ResponseUtil.createSuccessResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/lasterror/{flowId}",lastError,lastError,flowId);
		} catch (Exception e) {
   			e.printStackTrace();
			return ResponseUtil.createFailureResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/lasterror/{flowId}",e.getMessage(),"unable to get last error for flow " + flowId,flowId);
		}
    }

    @GetMapping(path = "/integration/{integrationId}/flow/totalmessages/{flowId}", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> getFlowTotalMessages(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable String flowId) throws Exception {

		try {
            //integrationResource.init();
            integration = integrationResource.getIntegration();

			String numberOfMessages = integration.getFlowTotalMessages(flowId);
			return ResponseUtil.createSuccessResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/totalmessages/{flowId}",numberOfMessages,numberOfMessages,flowId);
		} catch (Exception e) {
   			e.printStackTrace();
			return ResponseUtil.createFailureResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/totalmessages/{flowId}",e.getMessage(),"unable to get total messages of flow " + flowId,flowId);
		}
    }

    @GetMapping(path = "/integration/{integrationId}/flow/completedmessages/{flowId}", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> getFlowCompletedMessages(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable String flowId) throws Exception {

		try {
            //integrationResource.init();
            integration = integrationResource.getIntegration();

    		String completedMessages = integration.getFlowCompletedMessages(flowId);
			return ResponseUtil.createSuccessResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/completedmessages/{flowId}",completedMessages,completedMessages,flowId);
		} catch (Exception e) {
   			e.printStackTrace();
			return ResponseUtil.createFailureResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/completedmessages/{flowId}",e.getMessage(),"unable to get completed messages of flow " + flowId,flowId);
		}
    }

    @GetMapping(path = "/integration/{integrationId}/flow/failedmessages/{flowId}", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> getFlowFailedMessages(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable String flowId) throws Exception {

		try {
            ////integrationResource.init();
            integration = integrationResource.getIntegration();

    		String failedMessages = integration.getFlowFailedMessages(flowId);
			return ResponseUtil.createSuccessResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/failedmessages/{flowId}",failedMessages,failedMessages,flowId);
		} catch (Exception e) {
   			e.printStackTrace();
			return ResponseUtil.createFailureResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/failedmessages/{flowId}",e.getMessage(),"unable to get failed messages of flow " + flowId,flowId);
		}
    }

    @GetMapping(path = "/integration/{integrationId}/flow/alerts/{flowId}", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> getFlowAlertsLog(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable String flowId) throws Exception {

		try {
            integration = integrationResource.getIntegration();

            String log = integration.getFlowAlertsLog(flowId,100);
			return ResponseUtil.createSuccessResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/failedlog/{flowId}",log,log,flowId);
		} catch (Exception e) {
   			e.printStackTrace();
			return ResponseUtil.createFailureResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/failedmessages/{flowId}",e.getMessage(),"unable to get failed log of flow" + flowId,flowId);
		}
    }

    @GetMapping(path = "/integration/{integrationId}/flow/numberofalerts/{flowId}", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> getFlowNumberOfAlerts(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable String flowId) throws Exception {

		try {
        	integration = integrationResource.getIntegration();
            String numberOfEntries = integration.getFlowAlertsCount(flowId);

            return ResponseUtil.createSuccessResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/failedlogentries/{flowId}",numberOfEntries,numberOfEntries,flowId);
		} catch (Exception e) {
   			e.printStackTrace();
			return ResponseUtil.createFailureResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/failedlogentries/{flowId}",e.getMessage(),"unable to get failed entries of flow log" + flowId,flowId);
		}
    }

    @GetMapping(path = "/integration/{integrationId}/numberofalerts", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> getIntegrationNumberOfAlerts(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId) throws Exception {

		try {
            integration = integrationResource.getIntegration();
            TreeMap<String,String> numberOfEntriesList = integration.getIntegrationAlertsCount();

			return ResponseUtil.createSuccessResponse(integrationId, mediaType,"/integration/{integrationId}/flow/failedlog}",numberOfEntriesList.toString());
		} catch (Exception e) {
   			e.printStackTrace();
			return ResponseUtil.createFailureResponse(integrationId, mediaType,"/integration/{integrationId}/flow/failedmessages",e.getMessage());
		}
    }

    @GetMapping(path = "/integration/{integrationId}/flow/eventlog/{flowId}", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> getFlowEventLog(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable String flowId) throws Exception {

		try {
            integration = integrationResource.getIntegration();
            String log = integration.getFlowEventsLog(flowId,100);
			return ResponseUtil.createSuccessResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/eventlog/{flowId}",log,log,flowId);
		} catch (Exception e) {
   			e.printStackTrace();
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

                SimpMessageSendingOperations messagingTemplate = messagingTemplate2;

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
                                if(status.equals("started")) {
                                    if(this.messagingTemplate!=null) {
                                        this.messagingTemplate.convertAndSend("/topic/" + flowId + "/event","event:resumed");
                                    }
                                }
                            }
                        }

                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            });

            // start the thread
            thread.start();

            return ResponseUtil.createSuccessResponse(integrationId, mediaType,"/integration/{integrationId}/maintenance/{time}","Set flows into maintenance mode for " + time + " miliseconds");
        } catch (Exception e) {
            e.printStackTrace();
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
