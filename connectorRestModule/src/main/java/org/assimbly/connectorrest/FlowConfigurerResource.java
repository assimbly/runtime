package org.assimbly.connectorrest;

import io.swagger.annotations.ApiParam;
import org.assimbly.connector.Connector;
import org.assimbly.util.rest.ResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.NativeWebRequest;
import javax.servlet.http.HttpServletRequest;
import java.net.URISyntaxException;

/**
 * Resource to return information about the currently running Spring profiles.
 */
@ControllerAdvice
@RestController
@RequestMapping("/api")
public class FlowConfigurerResource {

    private final Logger log = LoggerFactory.getLogger(FlowConfigurerResource.class);

    @Autowired
    private ConnectorResource connectorResource;

    private String flowId;

    private boolean plainResponse;

    private String flowConfiguration;

    private Connector connector;

    /**
     * POST  /connector/{connectorId}/setflowconfiguration/{id} : Set configuration from XML.
     *
     * @param connectorId (connectorId)
     * @param id (FlowId)
     * @param configuration as XML
     * @return the ResponseEntity with status 200 (Successful) and status 400 (Bad Request) if the configuration failed
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping(path = "/connector/{connectorId}/setflowconfiguration/{id}", consumes =  {"text/plain","application/xml","application/json"}, produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> setFlowConfiguration(@ApiParam(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long connectorId,@PathVariable Long id,@RequestBody String configuration) throws Exception {

       	try {
            connector = connectorResource.getConnector();
            connector.setFlowConfiguration(id.toString(), mediaType, configuration);
       		return ResponseUtil.createSuccessResponse(connectorId, mediaType,"/connector/{connectorId}/setflowconfiguration/{id}","Flow configuration set");
   		} catch (Exception e) {
   			e.printStackTrace();
   			return ResponseUtil.createFailureResponse(connectorId, mediaType,"/connector/{connectorId}/setflowconfiguration/{id}",e.getMessage());
   		}
    }

    /**
     * Get  /connector/{connectorId}/getflowconfiguration/{id} : get XML configuration for connector.
     *
     * @param connectorId (connectorId)
     * @param id (flowId)
     * @return the ResponseEntity with status 200 (Successful) and status 400 (Bad Request) if the configuration failed
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @GetMapping(path = "/connector/{connectorId}/getflowconfiguration/{id}", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> getFlowConfiguration(@ApiParam(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long connectorId, @PathVariable Long id) throws Exception {

    	plainResponse = true;

    	try {
            connector = connectorResource.getConnector();
            flowConfiguration = connector.getFlowConfiguration(id.toString(), mediaType);
			if(flowConfiguration.startsWith("Error")||flowConfiguration.startsWith("Warning")) {
				return ResponseUtil.createFailureResponse(connectorId, mediaType,"/connector/{connectorId}/getconfiguration",flowConfiguration);
			}
			return ResponseUtil.createSuccessResponse(connectorId, mediaType,"/connector/{connectorId}/getflowconfiguration/{id}",flowConfiguration,plainResponse);
   		} catch (Exception e) {
   			e.printStackTrace();
   			return ResponseUtil.createFailureResponse(connectorId, mediaType,"/connector/{connectorId}/getflowconfiguration/{id}",e.getMessage());
   		}
    }

	/**
	 * POST  /connector/{connectorid}/setconfiguration : Set configuration from XML.
	 *
	 * @param connectorId (gatewayId)
	 * @param configuration as xml
	 * @return the ResponseEntity with status 200 (Successful) and status 400 (Bad Request) if the configuration failed
	 * @throws URISyntaxException if the Location URI syntax is incorrect
	 */
	@PostMapping(path = "/connector/{connectorId}/setflowconfigurations", consumes =  {"text/plain","application/xml","application/json"}, produces = {"text/plain","application/xml","application/json"})
	public ResponseEntity<String> setFlowConfigurations(@ApiParam(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long connectorId, @RequestBody String configuration) throws Exception {
		try {
			connector.setFlowConfigurations(connectorId.toString(), mediaType, configuration);
			return ResponseUtil.createSuccessResponse(connectorId, mediaType, "/connector/{connectorId}/setconfiguration", "Connector configuration set");
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseUtil.createFailureResponse(connectorId, mediaType, "/connector/{connectorId}/setconfiguration", e.getMessage());
		}

	}

	/**
	 * Get  /connector/{connectorId}/getconfiguration : get XML configuration for gateway.
	 *
	 * @param connectorId (gatewayId)
	 * @return the ResponseEntity with status 200 (Successful) and status 400 (Bad Request) if the configuration failed
	 * @throws URISyntaxException if the Location URI syntax is incorrect
	 */
	@GetMapping(path = "/connector/{connectorId}/getFlowConfigurations", produces = {"text/plain","application/xml","application/json"})
	public ResponseEntity<String> getFlowConfigurations(@ApiParam(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long connectorId) throws Exception {

		plainResponse = true;

		try {
			String gatewayConfiguration = connector.getFlowConfigurations(connectorId.toString(), mediaType);
			if (gatewayConfiguration.startsWith("Error") || gatewayConfiguration.startsWith("Warning")) {
				return ResponseUtil.createFailureResponse(connectorId, mediaType, "/connector/{connectorId}/getconfiguration", gatewayConfiguration);
			}
			return ResponseUtil.createSuccessResponse(connectorId, mediaType, "/connector/{connectorId}/getconfiguration", gatewayConfiguration, plainResponse);
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseUtil.createFailureResponse(connectorId, mediaType, "/connector/{connectorId}/getconfiguration", e.getMessage());
		}

	}



    @GetMapping(path = "/connector/{connectorId}/flow/documentation/version", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> getDocumentationVersion(@ApiParam(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long connectorId) throws Exception {

		try {
            connector = connectorResource.getConnector();
            String documentation = connector.getDocumentationVersion();
			return ResponseUtil.createSuccessResponse(connectorId, mediaType,"/connector/{connectorId}/flow/documentation/version",documentation,plainResponse);
		} catch (Exception e) {
   			e.printStackTrace();
			return ResponseUtil.createFailureResponse(connectorId, mediaType,"/connector/{connectorId}/flow/documentation/version",e.getMessage());
		}
    }

    @GetMapping(path = "/connector/{connectorId}/flow/documentation/{componenttype}", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> getDocumentation(@ApiParam(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long connectorId, @PathVariable String componenttype) throws Exception {

    	plainResponse = true;

		try {
            connector = connectorResource.getConnector();
            String documentation = connector.getDocumentation(componenttype, mediaType);
    		if(documentation.startsWith("Unknown")) {
				return ResponseUtil.createFailureResponse(connectorId, mediaType,"/connector/{connectorId}/flow/documentation/{componenttype}",documentation);
			}
			return ResponseUtil.createSuccessResponse(connectorId, mediaType,"/connector/{connectorId}/flow/documentation/{componenttype}",documentation,plainResponse);
		} catch (Exception e) {
   			e.printStackTrace();
			return ResponseUtil.createFailureResponse(connectorId, mediaType,"/connector/{connectorId}/flow/documentation/{componenttype}",e.getMessage());
		}
    }

    @GetMapping(path = "/connector/{connectorId}/flow/schema/{componenttype}", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> getComponentSchema(@ApiParam(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long connectorId, @PathVariable String componenttype) throws Exception {

    	plainResponse = true;

		try {
            connector = connectorResource.getConnector();
            String documentation = connector.getComponentSchema(componenttype, mediaType);
    		if(documentation.startsWith("Unknown")) {
				return ResponseUtil.createFailureResponse(connectorId, mediaType,"/connector/{connectorId}/flow/schema/{componenttype}",documentation);
			}
			return ResponseUtil.createSuccessResponse(connectorId, mediaType,"/connector/{connectorId}/flow/schema/{componenttype}",documentation,plainResponse);
		} catch (Exception e) {
   			e.printStackTrace();
			return ResponseUtil.createFailureResponse(connectorId, mediaType,"/connector/{connectorId}/flow/schema/{componenttype}",e.getMessage());
		}
    }

    @GetMapping(path = "/connector/{connectorId}/flow/options/{componenttype}", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> getComponentOptions(@ApiParam(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long connectorId, @PathVariable String componenttype) throws Exception {

    	plainResponse = true;

		try {
            connector = connectorResource.getConnector();
            String documentation = connector.getComponentParameters(componenttype, mediaType);
    		if(documentation.startsWith("Unknown")) {
				return ResponseUtil.createFailureResponse(connectorId, mediaType,"/connector/{connectorId}/flow/options/{componenttype}",documentation);
			}
			return ResponseUtil.createSuccessResponse(connectorId, mediaType,"/connector/{connectorId}/flow/options/{componenttype}",documentation,plainResponse);
		} catch (Exception e) {
   			e.printStackTrace();
			return ResponseUtil.createFailureResponse(connectorId, mediaType,"/connector/{connectorId}/flow/options/{componenttype}",e.getMessage());
		}
    }

	@GetMapping(path = "/connector/{connectorId}/flow/validateUri", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> validateFlowUri(@ApiParam(hidden = true) @RequestHeader("Accept") String mediaType, @RequestHeader("Uri") String uri, @PathVariable Long connectorId) throws Exception {
		try {
            connector = connectorResource.getConnector();
            String flowValidation = connector.validateFlow(uri);
			return ResponseUtil.createSuccessResponse(connectorId, mediaType,"/connector/{connectorId}/flow/validateUri",flowValidation);
		} catch (Exception e) {
			return ResponseUtil.createFailureResponse(connectorId, mediaType,"/connector/{connectorId}/flow/validateUri",e.getMessage());
		}
    }

    @GetMapping(path = "/connector/{connectorId}/flow/route/{id}", produces = {"application/xml","application/json"})
    public ResponseEntity<String> getCamelRoute(@ApiParam(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long connectorId, @PathVariable Long id) throws Exception {

		try {
        	flowId = id.toString();
            connector = connectorResource.getConnector();
            String camelRoute = connector.getCamelRouteConfiguration(flowId, mediaType);
			return ResponseUtil.createSuccessResponse(connectorId, mediaType,"/connector/{connectorId}/flow/route/{id}",camelRoute,true);
		} catch (Exception e) {
   			e.printStackTrace();
			return ResponseUtil.createFailureResponse(connectorId, mediaType,"/connector/{connectorId}/flow/route/{id}",e.getMessage());
		}
    }

    @GetMapping(path = "/connector/{connectorId}/flow/routes", produces = {"application/xml","application/json"})
    public ResponseEntity<String> getAllCamelRoutes(@ApiParam(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long connectorId) throws Exception {

		try {
            connector = connectorResource.getConnector();
            String camelRoutes = connector.getAllCamelRoutesConfiguration(mediaType);
			return ResponseUtil.createSuccessResponse(connectorId, mediaType,"/connector/{connectorId}/flow/routes",camelRoutes,true);
		} catch (Exception e) {
   			e.printStackTrace();
			return ResponseUtil.createFailureResponse(connectorId, mediaType,"/connector/{connectorId}/flow/routes",e.getMessage());
		}

    }

    @GetMapping(path = "/connector/{connectorId}/removeflow/{id}", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> removeFlow(@ApiParam(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long connectorId, @PathVariable Long id) throws Exception {

        try {
            connector = connectorResource.getConnector();
            Boolean removedFlow = connector.removeFlow(id.toString());
            return ResponseUtil.createSuccessResponse(connectorId, mediaType,"/connector/{connectorId}/removeflow/{id}",removedFlow.toString());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseUtil.createFailureResponse(connectorId, mediaType,"/connector/{connectorId}/removeflow/{id}",e.getMessage());
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

}
