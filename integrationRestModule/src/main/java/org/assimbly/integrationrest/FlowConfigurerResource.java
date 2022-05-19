package org.assimbly.integrationrest;

import io.swagger.v3.oas.annotations.Parameter;
import org.assimbly.integration.Integration;
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

	protected Logger log = LoggerFactory.getLogger(getClass());
	
    @Autowired
    private IntegrationResource integrationResource;

    private boolean plainResponse;

    private String flowConfiguration;

    private Integration integration;

    /**
     * POST  /integration/{integrationId}/setflowconfiguration/{flowId} : Set configuration from XML.
     *
     * @param integrationId (integrationId)
     * @param id (FlowId)
     * @param configuration as XML
     * @return the ResponseEntity with status 200 (Successful) and status 400 (Bad Request) if the configuration failed
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping(path = "/integration/{integrationId}/setflowconfiguration/{flowId}", consumes =  {"application/xml","application/json","text/plain"}, produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> setFlowConfiguration(@Parameter(hidden = true) @RequestHeader("Content-type") String mediaType, @PathVariable Long integrationId,@PathVariable String flowId,@RequestBody String configuration) throws Exception {

       	try {
            integration = integrationResource.getIntegration();
            integration.setFlowConfiguration(flowId, mediaType, configuration);
       		return ResponseUtil.createSuccessResponse(integrationId, mediaType,"/integration/{integrationId}/setflowconfiguration/{flowId}","Flow configuration set");
   		} catch (Exception e) {
   			e.printStackTrace();
   			return ResponseUtil.createFailureResponse(integrationId, mediaType,"/integration/{integrationId}/setflowconfiguration/{flowId}",e.getMessage());
   		}
    }

    /**
     * Get  /integration/{integrationId}/getflowconfiguration/{flowId} : get XML configuration for integration.
     *
     * @param integrationId (integrationId)
     * @param id (flowId)
     * @return the ResponseEntity with status 200 (Successful) and status 400 (Bad Request) if the configuration failed
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @GetMapping(path = "/integration/{integrationId}/getflowconfiguration/{flowId}", produces = {"application/xml","application/json","text/plain"})
    public ResponseEntity<String> getFlowConfiguration(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable String flowId) throws Exception {

    	plainResponse = true;

    	try {
            integration = integrationResource.getIntegration();
            flowConfiguration = integration.getFlowConfiguration(flowId, mediaType);
			if(flowConfiguration.startsWith("Error")||flowConfiguration.startsWith("Warning")) {
				return ResponseUtil.createFailureResponse(integrationId, mediaType,"/integration/{integrationId}/getconfiguration",flowConfiguration);
			}
			return ResponseUtil.createSuccessResponse(integrationId, mediaType,"/integration/{integrationId}/getflowconfiguration/{flowId}",flowConfiguration,plainResponse);
   		} catch (Exception e) {
   			e.printStackTrace();
   			return ResponseUtil.createFailureResponse(integrationId, mediaType,"/integration/{integrationId}/getflowconfiguration/{flowId}",e.getMessage());
   		}
    }

	/**
	 * POST  /integration/{integrationId}/setconfiguration : Set configuration from XML.
	 *
	 * @param integrationId (gatewayId)
	 * @param configuration as xml
	 * @return the ResponseEntity with status 200 (Successful) and status 400 (Bad Request) if the configuration failed
	 * @throws URISyntaxException if the Location URI syntax is incorrect
	 */
	@PostMapping(path = "/integration/{integrationId}/setflowconfigurations", consumes =  {"application/xml","application/json","text/plain"}, produces = {"text/plain","application/xml","application/json"})
	public ResponseEntity<String> setFlowConfigurations(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @RequestBody String configuration) throws Exception {
		try {
			integration.setFlowConfigurations(integrationId.toString(), mediaType, configuration);
			return ResponseUtil.createSuccessResponse(integrationId, mediaType, "/integration/{integrationId}/setconfiguration", "Integration configuration set");
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseUtil.createFailureResponse(integrationId, mediaType, "/integration/{integrationId}/setconfiguration", e.getMessage());
		}

	}

	/**
	 * Get  /integration/{integrationId}/getconfiguration : get XML configuration for gateway.
	 *
	 * @param integrationId (gatewayId)
	 * @return the ResponseEntity with status 200 (Successful) and status 400 (Bad Request) if the configuration failed
	 * @throws URISyntaxException if the Location URI syntax is incorrect
	 */
	@GetMapping(path = "/integration/{integrationId}/getFlowConfigurations", produces = {"application/xml","application/json","text/plain"})
	public ResponseEntity<String> getFlowConfigurations(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId) throws Exception {

		plainResponse = true;

		try {
			String gatewayConfiguration = integration.getFlowConfigurations(integrationId.toString(), mediaType);
			if (gatewayConfiguration.startsWith("Error") || gatewayConfiguration.startsWith("Warning")) {
				return ResponseUtil.createFailureResponse(integrationId, mediaType, "/integration/{integrationId}/getconfiguration", gatewayConfiguration);
			}
			return ResponseUtil.createSuccessResponse(integrationId, mediaType, "/integration/{integrationId}/getconfiguration", gatewayConfiguration, plainResponse);
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseUtil.createFailureResponse(integrationId, mediaType, "/integration/{integrationId}/getconfiguration", e.getMessage());
		}

	}



    @GetMapping(path = "/integration/{integrationId}/flow/documentation/version", produces = {"application/xml","application/json","text/plain"})
    public ResponseEntity<String> getDocumentationVersion(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId) throws Exception {

		try {
            integration = integrationResource.getIntegration();
            String documentation = integration.getDocumentationVersion();
			return ResponseUtil.createSuccessResponse(integrationId, mediaType,"/integration/{integrationId}/flow/documentation/version",documentation,plainResponse);
		} catch (Exception e) {
   			e.printStackTrace();
			return ResponseUtil.createFailureResponse(integrationId, mediaType,"/integration/{integrationId}/flow/documentation/version",e.getMessage());
		}
    }

    @GetMapping(path = "/integration/{integrationId}/flow/documentation/{componenttype}", produces = {"application/xml","application/json","text/plain"})
    public ResponseEntity<String> getDocumentation(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable String componenttype) throws Exception {

    	plainResponse = true;

		try {
            integration = integrationResource.getIntegration();
            String documentation = integration.getDocumentation(componenttype, mediaType);
    		if(documentation.startsWith("Unknown")) {
				return ResponseUtil.createFailureResponse(integrationId, mediaType,"/integration/{integrationId}/flow/documentation/{componenttype}",documentation);
			}
			return ResponseUtil.createSuccessResponse(integrationId, mediaType,"/integration/{integrationId}/flow/documentation/{componenttype}",documentation,plainResponse);
		} catch (Exception e) {
   			e.printStackTrace();
			return ResponseUtil.createFailureResponse(integrationId, mediaType,"/integration/{integrationId}/flow/documentation/{componenttype}",e.getMessage());
		}
    }

	@GetMapping(path = "/integration/{integrationId}/flow/components", produces = {"application/xml","application/json","text/plain"})
	public ResponseEntity<String> getComponents(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId) throws Exception {

		plainResponse = true;

		try {
			integration = integrationResource.getIntegration();
			String components = integration.getComponents(mediaType);
			if(components.startsWith("Unknown")) {
				return ResponseUtil.createFailureResponse(integrationId, mediaType,"/integration/{integrationId}/flow/schema/{componenttype}",components);
			}
			return ResponseUtil.createSuccessResponse(integrationId, mediaType,"/integration/{integrationId}/flow/schema/{componenttype}",components,plainResponse);
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseUtil.createFailureResponse(integrationId, mediaType,"/integration/{integrationId}/flow/schema/{componenttype}",e.getMessage());
		}
	}

    @GetMapping(path = "/integration/{integrationId}/flow/schema/{componenttype}", produces = {"application/xml","application/json","text/plain"})
    public ResponseEntity<String> getComponentSchema(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable String componenttype) throws Exception {

    	plainResponse = true;

		try {
            integration = integrationResource.getIntegration();
            String documentation = integration.getComponentSchema(componenttype, mediaType);
    		if(documentation.startsWith("Unknown")) {
				return ResponseUtil.createFailureResponse(integrationId, mediaType,"/integration/{integrationId}/flow/schema/{componenttype}",documentation);
			}
			return ResponseUtil.createSuccessResponse(integrationId, mediaType,"/integration/{integrationId}/flow/schema/{componenttype}",documentation,plainResponse);
		} catch (Exception e) {
   			e.printStackTrace();
			return ResponseUtil.createFailureResponse(integrationId, mediaType,"/integration/{integrationId}/flow/schema/{componenttype}",e.getMessage());
		}
    }

    @GetMapping(path = "/integration/{integrationId}/flow/options/{componenttype}", produces = {"application/xml","application/json","text/plain"})
    public ResponseEntity<String> getComponentOptions(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable String componenttype) throws Exception {

    	plainResponse = true;

		try {
            integration = integrationResource.getIntegration();
            String documentation = integration.getComponentParameters(componenttype, mediaType);
    		if(documentation.startsWith("Unknown")) {
				return ResponseUtil.createFailureResponse(integrationId, mediaType,"/integration/{integrationId}/flow/options/{componenttype}",documentation);
			}
			return ResponseUtil.createSuccessResponse(integrationId, mediaType,"/integration/{integrationId}/flow/options/{componenttype}",documentation,plainResponse);
		} catch (Exception e) {
   			e.printStackTrace();
			return ResponseUtil.createFailureResponse(integrationId, mediaType,"/integration/{integrationId}/flow/options/{componenttype}",e.getMessage());
		}
    }

	@GetMapping(path = "/integration/{integrationId}/flow/validateUri", produces = {"application/xml","application/json","text/plain"})
    public ResponseEntity<String> validateFlowUri(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @RequestHeader("Uri") String uri, @PathVariable Long integrationId) throws Exception {
		try {
            integration = integrationResource.getIntegration();
            String flowValidation = integration.validateFlow(uri);
			return ResponseUtil.createSuccessResponse(integrationId, mediaType,"/integration/{integrationId}/flow/validateUri",flowValidation);
		} catch (Exception e) {
			return ResponseUtil.createFailureResponse(integrationId, mediaType,"/integration/{integrationId}/flow/validateUri",e.getMessage());
		}
    }

    @GetMapping(path = "/integration/{integrationId}/flow/route/{flowId}", produces = {"application/xml","application/json"})
    public ResponseEntity<String> getCamelRoute(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable String flowId) throws Exception {

		try {
            integration = integrationResource.getIntegration();
            String camelRoute = integration.getCamelRouteConfiguration(flowId, mediaType);
			return ResponseUtil.createSuccessResponse(integrationId, mediaType,"/integration/{integrationId}/flow/route/{flowId}",camelRoute,true);
		} catch (Exception e) {
   			e.printStackTrace();
			return ResponseUtil.createFailureResponse(integrationId, mediaType,"/integration/{integrationId}/flow/route/{flowId}",e.getMessage());
		}
    }

    @GetMapping(path = "/integration/{integrationId}/flow/routes", produces = {"application/xml","application/json"})
    public ResponseEntity<String> getAllCamelRoutes(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId) throws Exception {

		try {
            integration = integrationResource.getIntegration();
            String camelRoutes = integration.getAllCamelRoutesConfiguration(mediaType);
			return ResponseUtil.createSuccessResponse(integrationId, mediaType,"/integration/{integrationId}/flow/routes",camelRoutes,true);
		} catch (Exception e) {
   			e.printStackTrace();
			return ResponseUtil.createFailureResponse(integrationId, mediaType,"/integration/{integrationId}/flow/routes",e.getMessage());
		}

    }

    @GetMapping(path = "/integration/{integrationId}/removeflow/{flowId}", produces = {"application/xml","application/json","text/plain"})
    public ResponseEntity<String> removeFlow(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable String flowId) throws Exception {

        try {
            integration = integrationResource.getIntegration();
            Boolean removedFlow = integration.removeFlow(flowId);
            return ResponseUtil.createSuccessResponse(integrationId, mediaType,"/integration/{integrationId}/removeflow/{flowId}",removedFlow.toString());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseUtil.createFailureResponse(integrationId, mediaType,"/integration/{integrationId}/removeflow/{flowId}",e.getMessage());
        }

    }

    // Generates a generic error response (exceptions outside try catch):
    @ExceptionHandler({Exception.class})
    public ResponseEntity<String> integrationErrorHandler(Exception error, NativeWebRequest request) throws Exception {

    	Long integrationId = 0L; // set integrationId to 0, as we may get a string value
    	String mediaType = request.getNativeRequest(HttpServletRequest.class).getHeader("ACCEPT");
    	String path = request.getNativeRequest(HttpServletRequest.class).getRequestURI();
    	String message = error.getMessage();

    	return ResponseUtil.createFailureResponse(integrationId, mediaType,path,message);
    }

}
