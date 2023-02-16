package org.assimbly.integrationrest;

import io.swagger.v3.oas.annotations.Parameter;
import org.assimbly.integration.Integration;
import org.assimbly.integration.impl.CamelIntegration;
import org.assimbly.integrationrest.event.FailureCollector;
import org.assimbly.util.rest.ResponseUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.NativeWebRequest;

import javax.servlet.http.HttpServletRequest;
import java.net.URISyntaxException;
import java.util.Properties;
import java.util.TreeMap;


/**
 * Resource to return information about the currently running Spring profiles.
 */
@ControllerAdvice
@RestController
@RequestMapping("/api")
public class IntegrationRuntime {

   	protected Logger log = LoggerFactory.getLogger(getClass());

    private Integration integration = new CamelIntegration(true);

    private boolean plainResponse;

    private boolean integrationIsStarting;

    @Autowired
    private FailureCollector failureCollector;

    public IntegrationRuntime() throws Exception {
    }

    //configure integration


    //Manage integration

    /**
     * Get  /start : starts integration.
     *
     * @param integrationId (by gatewayId)
     * @return The ResponseEntity with status 200 (Successful) and status 400 (Bad Request) if the starting integration failed
     * @throws Exception
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @GetMapping(path = "/integration/{integrationId}/start", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> start(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId) throws Exception {

        try {

            if (integration.isStarted()) {
                return ResponseUtil.createFailureResponse(integrationId, mediaType, "/integration/{integrationId}/start", "Integration already running");
            } else {
            	integration.addEventNotifier(failureCollector);
            	integration.setTracing(false, "default");
            	integration.start();
                return ResponseUtil.createSuccessResponse(integrationId, mediaType, "/integration/{integrationId}/start", "Integration started");
            }

        } catch (Exception e) {
            log.error("Start integration with id=" + integrationId + " failed",e);
            return ResponseUtil.createFailureResponse(integrationId, mediaType, "/integration/{integrationId}/start", e.getMessage());
        }

    }

    /**
     * GET  /stop : stops integration.
     *
     * @param integrationId (by gatewayId)
     * @return the ResponseEntity with status 200 (Successful) and status 400 (Bad Request) if the stopping integration failed
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @GetMapping(path = "/integration/{integrationId}/stop", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> stop(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType,  @PathVariable Long integrationId) throws Exception {

        try {
            integration.stop();
            return ResponseUtil.createSuccessResponse(integrationId, mediaType,"/integration/{integrationId}/stop","Integration stopped");
        } catch (Exception e) {
            log.error("Stop integration with id=" + integrationId + " failed",e);
            return ResponseUtil.createFailureResponse(integrationId, mediaType,"/integration/{integrationId}/stop",e.getMessage());
        }

    }

    /**
     * GET  /info : info of an integration.
     *
     * @param integrationId (by gatewayId)
     * @return the ResponseEntity with status 200 (Successful) and status 400 (Bad Request) if the stopping integration failed
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @GetMapping(path = "/integration/{integrationId}/info", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> info(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType,  @PathVariable Long integrationId) throws Exception {

        try {
            String info = integration.info(mediaType);
            return ResponseUtil.createSuccessResponse(integrationId, mediaType,"/integration/{integrationId}/info",info,true);
        } catch (Exception e) {
            log.error("Retrieving info on integration with id=" + integrationId + " failed",e);
            return ResponseUtil.createFailureResponse(integrationId, mediaType,"/integration/{integrationId}/info",e.getMessage());
        }

    }

    /**
     * GET  /istarted : checks if integration is started.
     *
     * @param integrationId (by GatewaId)
     * @return the ResponseEntity with status 200 (Successful) and status 400 (Bad Request) if the stopping integration failed
     * @throws Exception
     */
    @GetMapping(path = "/integration/{integrationId}/isStarted", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> isStarted(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType,  @PathVariable Long integrationId) throws Exception {

        try {
            Boolean started = integration.isStarted();
            return ResponseUtil.createSuccessResponse(integrationId, mediaType,"/integration/{integrationId}/isStarted",started.toString());
        } catch (Exception e) {
            log.error("Check if integration with id=" + integrationId + " is started failed",e);
            return ResponseUtil.createFailureResponse(integrationId, mediaType,"/integration/{integrationId}/isStarted",e.getMessage());
        }

    }

    @GetMapping(path = "/integration/{integrationId}/lasterror", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> getLastError(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId) throws Exception {

        try {
            String error = integration.getLastError();
            return ResponseUtil.createSuccessResponse(integrationId, mediaType,"/integration/{integrationId}/lasterror",error,plainResponse);
        } catch (Exception e) {
            log.error("Get last error for integration with id=" + integrationId + " failed",e);
            return ResponseUtil.createFailureResponse(integrationId, mediaType,"/integration/{integrationId}/lasterror",e.getMessage());
        }

    }

    /**
     * POST  /integration/{integrationId}/resolvedependencybyscheme/{scheme} : Resolve the Mave dependency by URI scheme (for example SFTP or FILE).
     *
     * @param integrationId (gatewayId)
     * @return the ResponseEntity with status 200 (Successful) and status 400 (Bad Request) if the configuration failed
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping(path = "/integration/{integrationId}/resolvedependencybyscheme/{scheme}", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> resolveDepedencyByScheme(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId,@PathVariable String scheme) throws Exception {

       	try {
       		String result = integration.resolveDependency(scheme);
       		return ResponseUtil.createSuccessResponse(integrationId, mediaType,"/integration/{integrationId}/resolvedependency/{groupId}/{artifactId}/{version}",result);
   		} catch (Exception e) {
            log.error("Resolve dependency for scheme=" + scheme + " failed",e);
            return ResponseUtil.createFailureResponse(integrationId, mediaType,"/integration/{integrationId}/resolvedependency/{groupId}/{artifactId}/{version}",e.getMessage());
   		}

    }

    @GetMapping(path = "/integration/{integrationId}/basedirectory", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> getBaseDirectory(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId) throws Exception {

        plainResponse = true;

        try {
            String directory = integration.getBaseDirectory();
            return ResponseUtil.createSuccessResponse(integrationId, mediaType,"/integration/{integrationId}/basedirectory",directory,plainResponse);
        } catch (Exception e) {
            log.error("Get base directory for Assimbly failed",e);
            return ResponseUtil.createFailureResponse(integrationId, mediaType,"/integration/{integrationId}/basedirectory",e.getMessage());
        }

    }

    @PostMapping(path = "/integration/{integrationId}/basedirectory", consumes = {"text/plain"}, produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> setBaseDirectory(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @RequestBody String directory) throws Exception {

        plainResponse = true;

        try {
			integration.setBaseDirectory(directory);
            return ResponseUtil.createSuccessResponse(integrationId, mediaType,"/integration/{integrationId}/basedirectory","success",plainResponse);
        } catch (Exception e) {
            log.error("Set base directory for Assimbly failed",e);
            return ResponseUtil.createFailureResponse(integrationId, mediaType,"/integration/{integrationId}/basedirectory",e.getMessage());
        }

    }

    @GetMapping(path = "/integration/{integrationId}/list/flows", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> getListOfFlows(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @RequestParam(required=false,value="filterByStatus") String filter, @PathVariable Long integrationId) throws Exception {

        try {
            String flows = integration.getListOfFlows(filter, mediaType);
            return ResponseUtil.createSuccessResponse(integrationId, mediaType,"/integration/{integrationId}/list/flows",flows,true);
        } catch (Exception e) {
            log.error("Get list of flows for integration=" + integrationId + " failed",e);
            return ResponseUtil.createFailureResponse(integrationId, mediaType,"/integration/{integrationId}/list/flows",e.getMessage());
        }

    }

    @GetMapping(path = "/integration/{integrationId}/list/flows/details", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> getRunningFlowsDetails(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @RequestParam(required=false,value="filterByStatus") String filter, @PathVariable Long integrationId) throws Exception {

        try {
            String flowsDetails = integration.getListOfFlowsDetails(filter, mediaType);
            return ResponseUtil.createSuccessResponse(integrationId, mediaType,"/integration/{integrationId}/list/flows",flowsDetails,true);
        } catch (Exception e) {
            log.error("Get list of flows with details for integration=" + integrationId + " failed",e);
            return ResponseUtil.createFailureResponse(integrationId, mediaType,"/integration/{integrationId}/list/flows/details",e.getMessage());
        }

    }


    @PostMapping(path = "/integration/{integrationId}/list/soap/action", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> getListOfSoapActions(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @RequestBody String url) throws Exception {

        try {
            String flows = integration.getListOfSoapActions(url, mediaType);
            return ResponseUtil.createSuccessResponse(integrationId, mediaType,"/integration/{integrationId}/list/soap/actions",flows,true);
        } catch (Exception e) {
            log.error("Get list of soap actions for integration=" + integrationId + " failed",e);
            return ResponseUtil.createFailureResponse(integrationId, mediaType,"/integration/{integrationId}/list/soap/actions",e.getMessage());
        }

    }

    @GetMapping(path = "/integration/{integrationId}/count/flows", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> countFlows(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @RequestParam(required=false,value="filterByStatus") String filter, @PathVariable Long integrationId) throws Exception {

        try {
            String flowsCount = integration.countFlows(filter, mediaType);
            return ResponseUtil.createSuccessResponse(integrationId, mediaType,"/integration/{integrationId}/count/flows",flowsCount,false);
        } catch (Exception e) {
            log.error("Count running flows for integration=" + integrationId + " failed",e);
            return ResponseUtil.createFailureResponse(integrationId, mediaType,"/integration/{integrationId}/count/flows",e.getMessage());
        }

    }

    @GetMapping(path = "/integration/{integrationId}/count/steps", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> countSteps(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @RequestParam(required=false,value="filterByStatus") String filter, @PathVariable Long integrationId) throws Exception {

        try {
            String stepsCount = integration.countSteps(filter, mediaType);
            return ResponseUtil.createSuccessResponse(integrationId, mediaType,"/integration/{integrationId}/count/steps",stepsCount,false);
        } catch (Exception e) {
            log.error("Count running steps for integration=" + integrationId + " failed",e);
            return ResponseUtil.createFailureResponse(integrationId, mediaType,"/integration/{integrationId}/count/steps",e.getMessage());
        }

    }

    @GetMapping(path = "/integration/{integrationId}/numberofalerts", produces = {"application/xml","application/json","text/plain"})
    public ResponseEntity<String> getIntegrationNumberOfAlerts(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId) throws Exception {

        try {
            TreeMap<String,String> numberOfEntriesList = integration.getIntegrationAlertsCount();

            return ResponseUtil.createSuccessResponse(integrationId, mediaType,"/integration/{integrationId}/flow/failedlog}",numberOfEntriesList.toString());
        } catch (Exception e) {
            log.error("Get number of alerts for integration " + integrationId + " failed",e);
            return ResponseUtil.createFailureResponse(integrationId, mediaType,"/integration/{integrationId}/flow/failedmessages",e.getMessage());
        }
    }

    /**
     * POST  /integration/{integrationId}/collector/{collectorId}/add : Set collector configuration
     *
     * @param integrationId (integrationId)
     * @param collectorId (CollectorId)
     * @param configuration as JSON or XML
     * @return the ResponseEntity with status 200 (Successful) and status 400 (Bad Request) if setting of the configuration failed
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping(path = "/integration/{integrationId}/collector/{collectorId}/add", consumes =  {"application/json", "application/xml", "text/plain"}, produces = {"application/json","application/xml","text/plain"})
    public ResponseEntity<String> addCollectorConfiguration(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable String collectorId, @RequestBody String configuration) throws Exception {

        log.info("Add collector with id=" + collectorId);

        try {
            String result = integration.addCollectorConfiguration(collectorId,mediaType, configuration);
            if(!result.equalsIgnoreCase("configured")){
                log.error("Add collector " + collectorId + " failed. Message: " + result);
                return ResponseUtil.createFailureResponse(integrationId, mediaType,"/integration/{integrationId}/collector/{collectorId}/add",result);
            }

            return ResponseUtil.createSuccessResponse(integrationId, mediaType,"/integration/{integrationId}/collector/{collectorId}/add",result);
        } catch (Exception e) {
            log.error("Add collector " + collectorId + " failed",e);
            return ResponseUtil.createFailureResponse(integrationId, mediaType,"/integration/{integrationId}/collector/{collectorId}/add",e.getMessage());
        }

    }

    /**
     * DELETE  /integration/{integrationId}/collector/{collectorId}/remove : Remove collector configuration
     *
     * @param integrationId (integrationId)
     * @param collectorId (CollectorId)
     * @return the ResponseEntity with status 200 (Successful) and status 400 (Bad Request) if the remove of configuration failed
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @DeleteMapping(path = "/integration/{integrationId}/collector/{collectorId}/remove", produces = {"application/json","application/xml","text/plain"})
    public ResponseEntity<String> removeCollectorConfiguration(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable String collectorId) throws Exception {

        log.info("Remove collector with id=" + collectorId);

        try {
            String result = integration.removeCollectorConfiguration(collectorId);
            return ResponseUtil.createSuccessResponse(integrationId, mediaType,"/integration/{integrationId}/collector/{collectorId}/remove",result);
        } catch (Exception e) {
            log.error("Remove collector " + collectorId + " failed",e);
            return ResponseUtil.createFailureResponse(integrationId, mediaType,"/integration/{integrationId}/collector/{collectorId}/remove",e.getMessage());
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

    public Integration getIntegration() {
        return integration;
    }

    public void setIntegration(Properties encryptionProperties) throws Exception {
        integration.setEncryptionProperties(encryptionProperties);
    }

    public void initIntegration(){

        if(!integration.isStarted() && !integrationIsStarting){
            try {

                //add notifier before starting integration
                integration.addEventNotifier(failureCollector);
                integration.start();
                integrationIsStarting = true;

                int count = 1;

                while (!integration.isStarted() && count < 300) {
                    Thread.sleep(100);
                    count++;
                }

                integrationIsStarting = false;

            } catch (Exception e) {
                log.error("Init integration failed",e);
            }
        }

    }

}
