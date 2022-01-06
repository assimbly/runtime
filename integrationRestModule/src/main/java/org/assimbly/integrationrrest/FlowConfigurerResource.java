package org.assimbly.integrationrest;

import io.swagger.annotations.ApiParam;
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

    private final Logger log = LoggerFactory.getLogger(FlowConfigurerResource.class);

    @Autowired
    private IntegrationResource integrationResource;

    private String flowId;

    private boolean plainResponse;

    private String flowConfiguration;

    private Integration integration;

    /**
     * POST  /integration/{integrationId}/setflowconfiguration/{id} : Set configuration from XML.
     *
     * @param integrationId (integrationId)
     * @param id (FlowId)
     * @param configuration as XML
     * @return the ResponseEntity with status 200 (Successful) and status 400 (Bad Request) if the configuration failed
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping(path = "/integration/{integrationId}/setflowconfiguration/{id}", consumes =  {"text/plain","application/xml","application/json"}, produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> setFlowConfiguration(@ApiParam(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId,@PathVariable Long id,@RequestBody String configuration) throws Exception {

       	try {
            integration = integrationResource.getIntegration();
            integration.setFlowConfiguration(id.toString(), mediaType, configuration);
       		return ResponseUtil.createSuccessResponse(integrationId, mediaType,"/integration/{integrationId}/setflowconfiguration/{id}","Flow configuration set");
   		} catch (Exception e) {
   			e.printStackTrace();
   			return ResponseUtil.createFailureResponse(integrationId, mediaType,"/integration/{integrationId}/setflowconfiguration/{id}",e.getMessage());
   		}
    }

    /**
     * Get  /integration/{integrationId}/getflowconfiguration/{id} : get XML configuration for integration.
     *
     * @param integrationId (integrationId)
     * @param id (flowId)
     * @return the ResponseEntity with status 200 (Successful) and status 400 (Bad Request) if the configuration failed
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @GetMapping(path = "/integration/{integrationId}/getflowconfiguration/{id}", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> getFlowConfiguration(@ApiParam(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable Long id) throws Exception {

    	plainResponse = true;

    	try {
            integration = integrationResource.getIntegration();
            flowConfiguration = integration.getFlowConfiguration(id.toString(), mediaType);
			if(flowConfiguration.startsWith("Error")||flowConfiguration.startsWith("Warning")) {
				return ResponseUtil.createFailureResponse(integrationId, mediaType,"/integration/{integrationId}/getconfiguration",flowConfiguration);
			}
			return ResponseUtil.createSuccessResponse(integrationId, mediaType,"/integration/{integrationId}/getflowconfiguration/{id}",flowConfiguration,plainResponse);
   		} catch (Exception e) {
   			e.printStackTrace();
   			return ResponseUtil.createFailureResponse(integrationId, mediaType,"/integration/{integrationId}/getflowconfiguration/{id}",e.getMessage());
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
	@PostMapping(path = "/integration/{integrationId}/setflowconfigurations", consumes =  {"text/plain","application/xml","application/json"}, produces = {"text/plain","application/xml","application/json"})
	public ResponseEntity<String> setFlowConfigurations(@ApiParam(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @RequestBody String configuration) throws Exception {
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
	@GetMapping(path = "/integration/{integrationId}/getFlowConfigurations", produces = {"text/plain","application/xml","application/json"})
	public ResponseEntity<String> getFlowConfigurations(@ApiParam(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId) throws Exception {

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



    @GetMapping(path = "/integration/{integrationId}/flow/documentation/version", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> getDocumentationVersion(@ApiParam(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId) throws Exception {

		try {
            integration = integrationResource.getIntegration();
            String documentation = integration.getDocumentationVersion();
			return ResponseUtil.createSuccessResponse(integrationId, mediaType,"/integration/{integrationId}/flow/documentation/version",documentation,plainResponse);
		} catch (Exception e) {
   			e.printStackTrace();
			return ResponseUtil.createFailureResponse(integrationId, mediaType,"/integration/{integrationId}/flow/documentation/version",e.getMessage());
		}
    }

    @GetMapping(path = "/integration/{integrationId}/flow/documentation/{componenttype}", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> getDocumentation(@ApiParam(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable String componenttype) throws Exception {

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

	@GetMapping(path = "/integration/{integrationId}/flow/components", produces = {"text/plain","application/xml","application/json"})
	public ResponseEntity<String> getComponents(@ApiParam(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId) throws Exception {

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

    @GetMapping(path = "/integration/{integrationId}/flow/schema/{componenttype}", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> getComponentSchema(@ApiParam(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable String componenttype) throws Exception {

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

    @GetMapping(path = "/integration/{integrationId}/flow/options/{componenttype}", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> getComponentOptions(@ApiParam(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable String componenttype) throws Exception {

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

	@GetMapping(path = "/integration/{integrationId}/flow/validateUri", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> validateFlowUri(@ApiParam(hidden = true) @RequestHeader("Accept") String mediaType, @RequestHeader("Uri") String uri, @PathVariable Long integrationId) throws Exception {
		try {
            integration = integrationResource.getIntegration();
            String flowValidation = integration.validateFlow(uri);
			return ResponseUtil.createSuccessResponse(integrationId, mediaType,"/integration/{integrationId}/flow/validateUri",flowValidation);
		} catch (Exception e) {
			return ResponseUtil.createFailureResponse(integrationId, mediaType,"/integration/{integrationId}/flow/validateUri",e.getMessage());
		}
    }

    @GetMapping(path = "/integration/{integrationId}/flow/route/{id}", produces = {"application/xml","application/json"})
    public ResponseEntity<String> getCamelRoute(@ApiParam(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable Long id) throws Exception {

		try {
        	flowId = id.toString();
            integration = integrationResource.getIntegration();
            String camelRoute = integration.getCamelRouteConfiguration(flowId, mediaType);
			return ResponseUtil.createSuccessResponse(integrationId, mediaType,"/integration/{integrationId}/flow/route/{id}",camelRoute,true);
		} catch (Exception e) {
   			e.printStackTrace();
			return ResponseUtil.createFailureResponse(integrationId, mediaType,"/integration/{integrationId}/flow/route/{id}",e.getMessage());
		}
    }

    @GetMapping(path = "/integration/{integrationId}/flow/routes", produces = {"application/xml","application/json"})
    public ResponseEntity<String> getAllCamelRoutes(@ApiParam(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId) throws Exception {

		try {
            integration = integrationResource.getIntegration();
            String camelRoutes = integration.getAllCamelRoutesConfiguration(mediaType);
			return ResponseUtil.createSuccessResponse(integrationId, mediaType,"/integration/{integrationId}/flow/routes",camelRoutes,true);
		} catch (Exception e) {
   			e.printStackTrace();
			return ResponseUtil.createFailureResponse(integrationId, mediaType,"/integration/{integrationId}/flow/routes",e.getMessage());
		}

    }

    @GetMapping(path = "/integration/{integrationId}/removeflow/{id}", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> removeFlow(@ApiParam(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable Long id) throws Exception {

        try {
            integration = integrationResource.getIntegration();
            Boolean removedFlow = integration.removeFlow(id.toString());
            return ResponseUtil.createSuccessResponse(integrationId, mediaType,"/integration/{integrationId}/removeflow/{id}",removedFlow.toString());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseUtil.createFailureResponse(integrationId, mediaType,"/integration/{integrationId}/removeflow/{id}",e.getMessage());
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
