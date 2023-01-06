package org.assimbly.integrationrest;

import io.swagger.v3.oas.annotations.Parameter;
import org.assimbly.integration.Integration;
import org.assimbly.integration.impl.CamelIntegration;
import org.assimbly.integrationrest.event.FailureListener;
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


/**
 * Resource to return information about the currently running Spring profiles.
 */
@ControllerAdvice
@RestController
@RequestMapping("/api")
public class IntegrationResource {

   	protected Logger log = LoggerFactory.getLogger(getClass());

    private Integration integration = new CamelIntegration(true);

    private boolean plainResponse;

    private boolean integrationIsStarting;

    @Autowired
    private FailureListener failureListener;

    public IntegrationResource() throws Exception {
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
            	integration.addEventNotifier(failureListener);
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

    @GetMapping(path = "/integration/{integrationId}/testconnection/{host}/{port}/{timeout}", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> testConnection(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable String host,@PathVariable int port, @PathVariable int timeout) throws Exception {

		try {
    		String testConnectionResult = integration.testConnection(host, port, timeout);
			return ResponseUtil.createSuccessResponse(integrationId, mediaType,"/integration/{integrationId}/testconnection/{host}/{port}/{timeout}",testConnectionResult);
		} catch (Exception e) {
            log.error("Test connection failed",e);
			return ResponseUtil.createFailureResponse(integrationId, mediaType,"/integration/{integrationId}/testconnection/{host}/{port}/{timeout}",e.getMessage());
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


    /**
     * POST  /integration/{integrationId}/resolvedependency/{groupId}/{artifactId}/{version}
     *
     * @param integrationId (gatewayId)
     * @return the ResponseEntity with status 200 (Successful) and status 400 (Bad Request) if the configuration failed
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    /*
    @PostMapping(path = "/integration/{integrationId}/resolvedependency/{groupId}/{artifactId}/{version}", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> resolveDepedency(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId,@PathVariable String groupId,@PathVariable String artifactId,@PathVariable String version) throws Exception {
       	try {
       		String result = integration.resolveDependency(groupId, artifactId, version);
       		return ResponseUtil.createSuccessResponse(integrationId, mediaType,"/integration/{integrationId}/resolvedependency/{groupId}/{artifactId}/{version}",result);
   		} catch (Exception e) {
   			e.printStackTrace();
   			return ResponseUtil.createFailureResponse(integrationId, mediaType,"/integration/{integrationId}/resolvedependency/{groupId}/{artifactId}/{version}",e.getMessage());
   		}
    }
    */


    /**
     * POST  /integration/{integrationId}/setcertificates : Sets TLS certificates.
     *
     * @param integrationId (gatewayId)
     * @return the ResponseEntity with status 200 (Successful) and status 400 (Bad Request) if the configuration failed
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping(path = "/integration/{integrationId}/setcertificates", consumes =  {"text/plain","application/xml","application/json"}, produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> setCertificates(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @RequestHeader String keystoreName, @RequestHeader String keystorePassword, @RequestBody String url) throws Exception {

       	try {
       		integration.setCertificatesInKeystore(keystoreName, keystorePassword, url);
       		return ResponseUtil.createSuccessResponse(integrationId, mediaType,"/integration/{integrationId}/setcertificates/{id}","Certificates set");
   		} catch (Exception e) {
            log.error("Set certificates for keystore=" + keystoreName + " for url=" + url + " failed",e);

            return ResponseUtil.createFailureResponse(integrationId, mediaType,"/integration/{integrationId}/setcertificates/{id}",e.getMessage());
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

    @GetMapping(path = "/integration/{integrationId}/list/runningflows", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> getRunningFlows(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId) throws Exception {

        try {
            String runningFlows = integration.getRunningFlows(mediaType);
            return ResponseUtil.createSuccessResponse(integrationId, mediaType,"/integration/{integrationId}/list/runningflows",runningFlows,true);
        } catch (Exception e) {
            log.error("Get list of running flows for integration=" + integrationId + " failed",e);
            return ResponseUtil.createFailureResponse(integrationId, mediaType,"/integration/{integrationId}/list/runningflows",e.getMessage());
        }

    }

    @GetMapping(path = "/integration/{integrationId}/list/details/runningflows", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> getRunningFlowsDetails(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId) throws Exception {

        try {
            String runningFlowsDetails = integration.getRunningFlowsDetails(mediaType);
            return ResponseUtil.createSuccessResponse(integrationId, mediaType,"/integration/{integrationId}/list/details/runningflows",runningFlowsDetails,true);
        } catch (Exception e) {
            log.error("Get list of running flows for integration=" + integrationId + " failed",e);
            return ResponseUtil.createFailureResponse(integrationId, mediaType,"/integration/{integrationId}/list/details/runningflows",e.getMessage());
        }

    }

    @GetMapping(path = "/integration/{integrationId}/count/runningflows", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> getRunningFlowsCount(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId) throws Exception {

        try {
            String runningFlowsCount = integration.getRunningFlowsCount(mediaType);
            return ResponseUtil.createSuccessResponse(integrationId, mediaType,"/integration/{integrationId}/count/runningflows",runningFlowsCount,false);
        } catch (Exception e) {
            log.error("Count running flows for integration=" + integrationId + " failed",e);
            return ResponseUtil.createFailureResponse(integrationId, mediaType,"/integration/{integrationId}/count/runningflows",e.getMessage());
        }

    }

    @GetMapping(path = "/integration/{integrationId}/count/runningsteps", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> getRunningStepsCount(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId) throws Exception {

        try {
            String runningStepsCount = integration.getRunningStepsCount(mediaType);
            return ResponseUtil.createSuccessResponse(integrationId, mediaType,"/integration/{integrationId}/count/runningsteps",runningStepsCount,false);
        } catch (Exception e) {
            log.error("Count running steps for integration=" + integrationId + " failed",e);
            return ResponseUtil.createFailureResponse(integrationId, mediaType,"/integration/{integrationId}/count/runningsteps",e.getMessage());
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
                integration.addEventNotifier(failureListener);
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
