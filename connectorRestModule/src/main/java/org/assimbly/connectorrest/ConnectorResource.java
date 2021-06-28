package org.assimbly.connectorrest;

import io.swagger.annotations.ApiParam;
import org.assimbly.connector.Connector;
import org.assimbly.connector.impl.CamelConnector;
import org.assimbly.util.rest.ResponseUtil;
import org.assimbly.connectorrest.event.FailureListener;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.NativeWebRequest;

import javax.servlet.http.HttpServletRequest;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.Properties;


/**
 * Resource to return information about the currently running Spring profiles.
 */
@ControllerAdvice
@RestController
@RequestMapping("/api")
public class ConnectorResource {

    private final Logger log = LoggerFactory.getLogger(ConnectorResource.class);

    private Connector connector = new CamelConnector();

    private boolean plainResponse;

    private String gatewayConfiguration;

    private boolean connectorIsStarting = false;

    private String type;

    //@Autowired
    //EncryptionProperties encryptionProperties;

    @Autowired
    FailureListener failureListener;

    //configure connector


    //Manage connector

    /**
     * Get  /start : starts connector.
     *
     * @param connectorId (by gatewayId)
     * @return The ResponseEntity with status 200 (Successful) and status 400 (Bad Request) if the starting connector failed
     * @throws Exception
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @GetMapping(path = "/connector/{connectorId}/start", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> start(@ApiParam(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long connectorId) throws Exception {
        try {

            if (connector.isStarted()) {
                return ResponseUtil.createFailureResponse(connectorId, mediaType, "/connector/{connectorId}/start", "Connector already running");
            } else {
                connector.addEventNotifier(failureListener);
                connector.setTracing(false);
                connector.start();
                return ResponseUtil.createSuccessResponse(connectorId, mediaType, "/connector/{connectorId}/start", "Connector started");
            }

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseUtil.createFailureResponse(connectorId, mediaType, "/connector/{connectorId}/start", e.getMessage());
        }
    }

    /**
     * GET  /stop : stops connector.
     *
     * @param connectorId (by gatewayId)
     * @return the ResponseEntity with status 200 (Successful) and status 400 (Bad Request) if the stopping connector failed
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @GetMapping(path = "/connector/{connectorId}/stop", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> stop(@ApiParam(hidden = true) @RequestHeader("Accept") String mediaType,  @PathVariable Long connectorId) throws Exception {

        try {
            String config = connector.getFlowConfigurations(connectorId.toString(), mediaType);
            connector.stop();
            return ResponseUtil.createSuccessResponse(connectorId, mediaType,"/connector/{connectorId}/stop","Connector stopped");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseUtil.createFailureResponse(connectorId, mediaType,"/connector/{connectorId}/stop",e.getMessage());
        }
    }


    /**
     * GET  /istarted : checks if connector is started.
     *
     * @param connectorId (by GatewaId)
     * @return the ResponseEntity with status 200 (Successful) and status 400 (Bad Request) if the stopping connector failed
     * @throws Exception
     */
    @GetMapping(path = "/connector/{connectorId}/isStarted", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> isStarted(@ApiParam(hidden = true) @RequestHeader("Accept") String mediaType,  @PathVariable Long connectorId) throws Exception {

        try {
            Boolean started = connector.isStarted();
            return ResponseUtil.createSuccessResponse(connectorId, mediaType,"/connector/{connectorId}/isStarted",started.toString());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseUtil.createFailureResponse(connectorId, mediaType,"/connector/{connectorId}/isStarted",e.getMessage());
        }

    }

    @GetMapping(path = "/connector/{connectorId}/testconnection/{host}/{port}/{timeout}", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> testConnection(@ApiParam(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long connectorId, @PathVariable String host,@PathVariable int port, @PathVariable int timeout) throws Exception {

		try {
    		String testConnectionResult = connector.testConnection(host, port, timeout);
			return ResponseUtil.createSuccessResponse(connectorId, mediaType,"/connector/{connectorId}/testconnection/{host}/{port}/{timeout}",testConnectionResult);
		} catch (Exception e) {
   			e.printStackTrace();
			return ResponseUtil.createFailureResponse(connectorId, mediaType,"/connector/{connectorId}/testconnection/{host}/{port}/{timeout}",e.getMessage());
		}

    }

    @GetMapping(path = "/connector/{connectorId}/lasterror", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> getLastError(@ApiParam(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long connectorId) throws Exception {

        try {
            String error = connector.getLastError();
            return ResponseUtil.createSuccessResponse(connectorId, mediaType,"/connector/{connectorId}/lasterror",error,plainResponse);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseUtil.createFailureResponse(connectorId, mediaType,"/connector/{connectorId}/lasterror",e.getMessage());
        }

    }

    /**
     * POST  /connector/{connectorId}/resolvedependencybyscheme/{scheme} : Resolve the Mave dependency by URI scheme (for example SFTP or FILE).
     *
     * @param connectorId (gatewayId)
     * @return the ResponseEntity with status 200 (Successful) and status 400 (Bad Request) if the configuration failed
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping(path = "/connector/{connectorId}/resolvedependencybyscheme/{scheme}", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> resolveDepedencyByScheme(@ApiParam(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long connectorId,@PathVariable String scheme) throws Exception {

       	try {
       		String result = connector.resolveDependency(scheme);
       		return ResponseUtil.createSuccessResponse(connectorId, mediaType,"/connector/{connectorId}/resolvedependency/{groupId}/{artifactId}/{version}",result);
   		} catch (Exception e) {
   			e.printStackTrace();
   			return ResponseUtil.createFailureResponse(connectorId, mediaType,"/connector/{connectorId}/resolvedependency/{groupId}/{artifactId}/{version}",e.getMessage());
   		}
    }


    /**
     * POST  /connector/{connectorId}/resolvedependency/{groupId}/{artifactId}/{version}
     *
     * @param connectorId (gatewayId)
     * @return the ResponseEntity with status 200 (Successful) and status 400 (Bad Request) if the configuration failed
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping(path = "/connector/{connectorId}/resolvedependency/{groupId}/{artifactId}/{version}", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> resolveDepedency(@ApiParam(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long connectorId,@PathVariable String groupId,@PathVariable String artifactId,@PathVariable String version) throws Exception {
       	try {
       		String result = connector.resolveDependency(groupId, artifactId, version);
       		return ResponseUtil.createSuccessResponse(connectorId, mediaType,"/connector/{connectorId}/resolvedependency/{groupId}/{artifactId}/{version}",result);
   		} catch (Exception e) {
   			e.printStackTrace();
   			return ResponseUtil.createFailureResponse(connectorId, mediaType,"/connector/{connectorId}/resolvedependency/{groupId}/{artifactId}/{version}",e.getMessage());
   		}
    }

    /**
     * POST  /connector/{connectorId}/setcertificates : Sets TLS certificates.
     *
     * @param connectorId (gatewayId)
     * @return the ResponseEntity with status 200 (Successful) and status 400 (Bad Request) if the configuration failed
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping(path = "/connector/{connectorId}/setcertificates", consumes =  {"text/plain","application/xml","application/json"}, produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> setCertificates(@ApiParam(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long connectorId, @RequestHeader String keystoreName, @RequestHeader String keystorePassword, @RequestBody String url) throws Exception {

       	try {
       		connector.setCertificatesInKeystore(keystoreName, keystorePassword, url);
       		return ResponseUtil.createSuccessResponse(connectorId, mediaType,"/connector/{connectorId}/setcertificates/{id}","Connector certificates set");
   		} catch (Exception e) {
   			e.printStackTrace();
   			return ResponseUtil.createFailureResponse(connectorId, mediaType,"/connector/{connectorId}/setcertificates/{id}",e.getMessage());
   		}
    }


    // Generates a generic error response (exceptions outside try catch):
    @ExceptionHandler({Exception.class})
    public ResponseEntity<String> connectorErrorHandler(Exception error, NativeWebRequest request) throws Exception {

    	Long connectorId = 0L; // set connectorid to 0, as we may get a string value
    	String mediaType = request.getNativeRequest(HttpServletRequest.class).getHeader("ACCEPT");
    	String path = request.getNativeRequest(HttpServletRequest.class).getRequestURI();
    	String message = error.getMessage();

    	return ResponseUtil.createFailureResponse(connectorId, mediaType,path,message);
    }

    @GetMapping(path = "/connector/{connectorId}/stats", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> getStats(@ApiParam(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long connectorId, @PathVariable Optional<String> statsType) throws Exception {

        plainResponse = true;

        try {

            if(statsType.isPresent()){
                type=statsType.get();
            }else {
                type="default";
            }
            String stats = connector.getStats(type, mediaType);
            if(stats.startsWith("Error")||stats.startsWith("Warning")) {plainResponse = false;}
            return ResponseUtil.createSuccessResponse(connectorId, mediaType,"/connector/{connectorId}/stats",stats,plainResponse);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseUtil.createFailureResponse(connectorId, mediaType,"/connector/{connectorId}/stats",e.getMessage());
        }
    }


    public Connector getConnector() {
        return connector;
    }

    public void setConnector(Properties encryptionProperties) throws Exception {

        connector.setEncryptionProperties(encryptionProperties);

    }


    public void initConnector(){

        if(!connector.isStarted() && !connectorIsStarting){
            try {

                //add notifier before starting connector
                connector.addEventNotifier(failureListener);
                connector.start();
                connectorIsStarting = true;

                int count = 1;

                while (!connector.isStarted() && count < 300) {
                    Thread.sleep(100);
                    count++;
                }

                connectorIsStarting = false;

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

}
