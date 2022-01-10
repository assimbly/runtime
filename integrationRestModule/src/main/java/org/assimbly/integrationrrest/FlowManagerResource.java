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
    @GetMapping(path = "/integration/{integrationId}/flow/start/{id}", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> startflow(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable Long id) throws Exception {

        try {
            ////integrationResource.init();
            integration = integrationResource.getIntegration();

            flowId = id.toString();
            status = integration.startFlow(flowId);
            if (status.equals("started")) {
                if (this.messagingTemplate != null) {
                    this.messagingTemplate.convertAndSend("/topic/" + flowId + "/event", "event:started");
                }
                return ResponseUtil.createSuccessResponseWithHeaders(integrationId, mediaType, "/integration/{integrationId}/flow/start/{id}", "started flow " + flowId, "started flow " + flowId, flowId);
            } else {
                throw new Exception(status);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseUtil.createFailureResponseWithHeaders(integrationId, mediaType, "/integration/{integrationId}/flow/start/{id}", e.getMessage(), "unable to start flow " + flowId, flowId);
        }

    }

    @GetMapping(path = "/integration/{integrationId}/flow/stop/{id}", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String>  stopflow(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable Long id) throws Exception {

        try {
            ////integrationResource.init();
            integration = integrationResource.getIntegration();

            flowId = id.toString();
            status = integration.stopFlow(flowId);
            if(status.equals("stopped")) {
                if(this.messagingTemplate!=null) {
                    this.messagingTemplate.convertAndSend("/topic/" + flowId + "/event","event:stopped");
                }
                return ResponseUtil.createSuccessResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/stop/{id}","stopped flow " + flowId,"stopped flow " + flowId,flowId);
            }else {
                throw new Exception(status);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseUtil.createFailureResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/stop/{id}",e.getMessage(),"unable to stop flow " + flowId,flowId);
        }

    }

    @GetMapping(path = "/integration/{integrationId}/flow/restart/{id}", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String>  restartflow(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable Long id) throws Exception {

        try {
            ////integrationResource.init();
            integration = integrationResource.getIntegration();

            flowId = id.toString();
            status = integration.restartFlow(flowId);
            if(status.equals("started")) {
                if(this.messagingTemplate!=null) {
                    this.messagingTemplate.convertAndSend("/topic/" + flowId + "/event","event:restarted");
                }
                return ResponseUtil.createSuccessResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/restart/{id}","restarted","restarted flow " + flowId,flowId);
            }else {
                throw new Exception(status);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseUtil.createFailureResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/restart/{id}",e.getMessage(),"unable to restart flow " + flowId,flowId);
        }

    }

    @GetMapping(path = "/integration/{integrationId}/flow/pause/{id}", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String>  pauseflow(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable Long id) throws Exception {

        try {
            ////integrationResource.init();
            integration = integrationResource.getIntegration();

            flowId = id.toString();
            status = integration.pauseFlow(flowId);
            if(status.equals("suspended") || status.equals("stopped")) {
                if(this.messagingTemplate!=null) {
                    this.messagingTemplate.convertAndSend("/topic/" + flowId + "/event","event:suspended");
                }
                return ResponseUtil.createSuccessResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/pause/{id}","paused","paused flow " + flowId,flowId);
            }else {
                throw new Exception(status);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseUtil.createFailureResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/pause/{id}",e.getMessage(),"unable to pause flow " + flowId,flowId);
        }

    }

    @GetMapping(path = "/integration/{integrationId}/flow/resume/{id}" , produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> resumeflow(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable Long id) throws Exception {

        try {
            ////integrationResource.init();
            integration = integrationResource.getIntegration();

            flowId = id.toString();
            status = integration.resumeFlow(flowId);
            if(status.equals("started")) {
                if(this.messagingTemplate!=null) {
                    this.messagingTemplate.convertAndSend("/topic/" + flowId + "/event","event:resumed");
                }
                return ResponseUtil.createSuccessResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/resume/{id}","resumed","resumed flow " + flowId,flowId);
            }else {
                throw new Exception(status);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseUtil.createFailureResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/resume/{id}",e.getMessage(),"unable to resume flow " + flowId,flowId);
        }
    }


    @GetMapping(path = "/integration/{integrationId}/flow/isstarted/{id}", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> isFlowStarted(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable Long id) throws Exception {

        try {
            //integrationResource.init();
            integration = integrationResource.getIntegration();

            flowId = id.toString();
            boolean started = integration.isFlowStarted(flowId);
            String isStarted = Boolean.toString(started);
            return ResponseUtil.createSuccessResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/status/{id}",isStarted,isStarted,flowId);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseUtil.createFailureResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/status/{id}",e.getMessage(),"unable to get status for flow " + flowId,flowId);
        }

    }


    @GetMapping(path = "/integration/{integrationId}/flow/status/{id}", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> getFlowStatus(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable Long id) throws Exception {

        try {
            //integrationResource.init();
            integration = integrationResource.getIntegration();

            flowId = id.toString();
            status = integration.getFlowStatus(flowId);
            return ResponseUtil.createSuccessResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/status/{id}",status,status,flowId);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseUtil.createFailureResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/status/{id}",e.getMessage(),"unable to get status for flow " + flowId,flowId);
        }

    }

    @GetMapping(path = "/integration/{integrationId}/flow/uptime/{id}", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> getFlowUptime(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable Long id) throws Exception {

        try {
            //integrationResource.init();
            integration = integrationResource.getIntegration();

            flowId = id.toString();
            String uptime = integration.getFlowUptime(flowId);
            return ResponseUtil.createSuccessResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/uptime/{id}",uptime,uptime,flowId);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseUtil.createFailureResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/uptime/{id}",e.getMessage(),"unable to get uptime flow " + flowId,flowId);
        }

    }

  @GetMapping(path = "/integration/{integrationId}/hasflow/{id}", produces = {"text/plain","application/xml","application/json"})
  public ResponseEntity<String> hasFlow(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable Long id) throws Exception {

		try {
            integration = integrationResource.getIntegration();
            Boolean hasFlow = integration.hasFlow(id.toString());
			return ResponseUtil.createSuccessResponse(integrationId, mediaType,"/integration/{integrationId}/hasflow/{id}",hasFlow.toString());
		} catch (Exception e) {
   			e.printStackTrace();
			return ResponseUtil.createFailureResponse(integrationId, mediaType,"/integration/{integrationId}/hasflow/{id}",e.getMessage());
		}

   }


    @GetMapping(path = "/integration/{integrationId}/flow/stats/{id}/{endpointid}", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> getFlowStats(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable Long id, @PathVariable Long endpointid) throws Exception {

        plainResponse = true;

        try {
            //integrationResource.init();
            integration = integrationResource.getIntegration();

            flowId = id.toString();
            endpointId = endpointid.toString();

            String flowStats = integration.getFlowStats(flowId, endpointId, mediaType);
            if(flowStats.startsWith("Error")||flowStats.startsWith("Warning")) {plainResponse = false;}
            return ResponseUtil.createSuccessResponse(integrationId, mediaType,"/integration/{integrationId}/flow/stats/{id}",flowStats,plainResponse);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseUtil.createFailureResponse(integrationId, mediaType,"/integration/{integrationId}/flow/stats/{id}",e.getMessage());
        }
    }

    @GetMapping(path = "/integration/{integrationId}/flow/lasterror/{id}", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> getFlowLastError(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable Long id) throws Exception {

		try {
            //integrationResource.init();
            integration = integrationResource.getIntegration();

            flowId = id.toString();
    		String lastError = integration.getFlowLastError(flowId);
			return ResponseUtil.createSuccessResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/lasterror/{id}",lastError,lastError,flowId);
		} catch (Exception e) {
   			e.printStackTrace();
			return ResponseUtil.createFailureResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/lasterror/{id}",e.getMessage(),"unable to get last error for flow " + flowId,flowId);
		}
    }

    @GetMapping(path = "/integration/{integrationId}/flow/totalmessages/{id}", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> getFlowTotalMessages(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable Long id) throws Exception {

		try {
            //integrationResource.init();
            integration = integrationResource.getIntegration();

            flowId = id.toString();
    		String numberOfMessages = integration.getFlowTotalMessages(flowId);
			return ResponseUtil.createSuccessResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/totalmessages/{id}",numberOfMessages,numberOfMessages,flowId);
		} catch (Exception e) {
   			e.printStackTrace();
			return ResponseUtil.createFailureResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/totalmessages/{id}",e.getMessage(),"unable to get total messages of flow " + flowId,flowId);
		}
    }

    @GetMapping(path = "/integration/{integrationId}/flow/completedmessages/{id}", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> getFlowCompletedMessages(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable Long id) throws Exception {

		try {
            //integrationResource.init();
            integration = integrationResource.getIntegration();

            flowId = id.toString();
    		String completedMessages = integration.getFlowCompletedMessages(flowId);
			return ResponseUtil.createSuccessResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/completedmessages/{id}",completedMessages,completedMessages,flowId);
		} catch (Exception e) {
   			e.printStackTrace();
			return ResponseUtil.createFailureResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/completedmessages/{id}",e.getMessage(),"unable to get completed messages of flow " + flowId,flowId);
		}
    }

    @GetMapping(path = "/integration/{integrationId}/flow/failedmessages/{id}", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> getFlowFailedMessages(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable Long id) throws Exception {

		try {
            ////integrationResource.init();
            integration = integrationResource.getIntegration();

            flowId = id.toString();
    		String failedMessages = integration.getFlowFailedMessages(flowId);
			return ResponseUtil.createSuccessResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/failedmessages/{id}",failedMessages,failedMessages,flowId);
		} catch (Exception e) {
   			e.printStackTrace();
			return ResponseUtil.createFailureResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/failedmessages/{id}",e.getMessage(),"unable to get failed messages of flow " + flowId,flowId);
		}
    }

    @GetMapping(path = "/integration/{integrationId}/flow/alerts/{id}", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> getFlowAlertsLog(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable Long id) throws Exception {

		try {
        	flowId = id.toString();
            integration = integrationResource.getIntegration();

            String log = integration.getFlowAlertsLog(flowId,100);
			return ResponseUtil.createSuccessResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/failedlog/{id}",log,log,flowId);
		} catch (Exception e) {
   			e.printStackTrace();
			return ResponseUtil.createFailureResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/failedmessages/{id}",e.getMessage(),"unable to get failed log of flow" + flowId,flowId);
		}
    }

    @GetMapping(path = "/integration/{integrationId}/flow/numberofalerts/{id}", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> getFlowNumberOfAlerts(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable Long id) throws Exception {

		try {
        	flowId = id.toString();

        	integration = integrationResource.getIntegration();
            String numberOfEntries = integration.getFlowAlertsCount(flowId);

            return ResponseUtil.createSuccessResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/failedlogentries/{id}",numberOfEntries,numberOfEntries,flowId);
		} catch (Exception e) {
   			e.printStackTrace();
			return ResponseUtil.createFailureResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/failedlogentries/{id}",e.getMessage(),"unable to get failed entries of flow log" + flowId,flowId);
		}
    }

    @GetMapping(path = "/integration/{integrationId}/numberofalerts", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> getIntegrationNumberOfAlerts(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId) throws Exception {

		try {
            integration = integrationResource.getIntegration();
            TreeMap<String,String> numberOfEntriesList = integration.getIntegrationAlertsCount();

			return ResponseUtil.createSuccessResponse(integrationId, mediaType,"/integration/{integrationId}/flow/failedlog/{id}",numberOfEntriesList.toString());
		} catch (Exception e) {
   			e.printStackTrace();
			return ResponseUtil.createFailureResponse(integrationId, mediaType,"/integration/{integrationId}/flow/failedmessages/{id}",e.getMessage());
		}
    }

    @GetMapping(path = "/integration/{integrationId}/flow/eventlog/{id}", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> getFlowEventLog(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable Long id) throws Exception {

		try {
        	flowId = id.toString();

            integration = integrationResource.getIntegration();
            String log = integration.getFlowEventsLog(flowId,100);
			return ResponseUtil.createSuccessResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/eventlog/{id}",log,log,flowId);
		} catch (Exception e) {
   			e.printStackTrace();
			return ResponseUtil.createFailureResponseWithHeaders(integrationId, mediaType,"/integration/{integrationId}/flow/eventlog/{id}",e.getMessage(),"unable to get event log of flow " + flowId,flowId);
		}
    }

    @PostMapping(path = "/integration/{integrationId}/maintenance/{time}", consumes = {"application/json"}, produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> setMaintenance(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable Long time, @RequestBody List<Long> ids) throws Exception {

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
                        for(Long id : ids) {
                            flowId = id.toString();
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

                        for(Long id : ids) {

                            flowId = id.toString();
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
