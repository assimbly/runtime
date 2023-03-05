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
public class FlowConfigurerRuntime {

	protected Logger log = LoggerFactory.getLogger(getClass());
	
    @Autowired
    private IntegrationRuntime integrationRuntime;

    private boolean plainResponse;

    private String flowConfiguration;

    private Integration integration;

    /**
     * POST  /integration/{integrationId}/flow/{flowId}/configure : Set configuration.
     *
     * @param integrationId (integrationId)
     * @param flowId (FlowId)
     * @param configuration as JSON or XML
     * @return the ResponseEntity with status 200 (Successful) and status 400 (Bad Request) if the configuration failed
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping(path = "/integration/{integrationId}/flow/{flowId}/configure", consumes =  {"application/xml","application/json","text/plain"}, produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> setFlowConfiguration(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId,@PathVariable String flowId,@RequestBody String configuration) throws Exception {

       	try {
            integration = integrationRuntime.getIntegration();
            integration.setFlowConfiguration(flowId, mediaType, configuration);
       		return ResponseUtil.createSuccessResponse(integrationId, mediaType,"/integration/{integrationId}/flow/{flowId}/configure","Flow configuration set");
   		} catch (Exception e) {
			log.error("Set flow configuration failed",e);
			return ResponseUtil.createFailureResponse(integrationId, mediaType,"/integration/{integrationId}/flow/{flowId}/configure",e.getMessage());
   		}
    }

    /**
     * Get  /integration/{integrationId}/flow/{flowId}/configure : get XML configuration for integration.
     *
     * @param integrationId (integrationId)
     * @param flowId (flowId)
     * @return the ResponseEntity with status 200 (Successful) and status 400 (Bad Request) if the configuration failed
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @GetMapping(path = "/integration/{integrationId}/flow/{flowId}/configure", produces = {"application/xml","application/json","text/plain"})
    public ResponseEntity<String> getFlowConfiguration(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable String flowId) throws Exception {

    	plainResponse = true;

    	try {
            integration = integrationRuntime.getIntegration();
            flowConfiguration = integration.getFlowConfiguration(flowId, mediaType);
			if(flowConfiguration.startsWith("Error")||flowConfiguration.startsWith("Warning")) {
				return ResponseUtil.createFailureResponse(integrationId, mediaType,"/integration/{integrationId}/flow/{flowId}/configure",flowConfiguration);
			}
			return ResponseUtil.createSuccessResponse(integrationId, mediaType,"/integration/{integrationId}/flow/{flowId}/configure",flowConfiguration,plainResponse);
   		} catch (Exception e) {
			log.error("Get flow configuration failed",e);
			return ResponseUtil.createFailureResponse(integrationId, mediaType,"/integration/{integrationId}/flow/{flowId}/configure",e.getMessage());
   		}
    }

	@GetMapping(path = "/integration/{integrationId}/flow/{flowId}/isconfigured", produces = {"application/xml","application/json","text/plain"})
	public ResponseEntity<String> hasFlow(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable String flowId) throws Exception {

		try {
			integration = integrationRuntime.getIntegration();
			Boolean hasFlow = integration.hasFlow(flowId);
			return ResponseUtil.createSuccessResponse(integrationId, mediaType,"/integration/{integrationId}/flow/{flowId}/hasflow",hasFlow.toString());
		} catch (Exception e) {
			log.error("Check if integration " + integrationId + " has flow " + flowId + " failed",e);
			return ResponseUtil.createFailureResponse(integrationId, mediaType,"/integration/{integrationId}/flow/{flowId}/hasflow",e.getMessage());
		}

	}

    @GetMapping(path = "/integration/{integrationId}/flow/documentation/version", produces = {"application/xml","application/json","text/plain"})
    public ResponseEntity<String> getDocumentationVersion(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId) throws Exception {

		try {
            integration = integrationRuntime.getIntegration();
            String documentation = integration.getDocumentationVersion();
			return ResponseUtil.createSuccessResponse(integrationId, mediaType,"/integration/{integrationId}/flow/documentation/version",documentation,plainResponse);
		} catch (Exception e) {
			log.error("Get documentation version failed",e);
			return ResponseUtil.createFailureResponse(integrationId, mediaType,"/integration/{integrationId}/flow/documentation/version",e.getMessage());
		}
    }

    @GetMapping(path = "/integration/{integrationId}/flow/documentation/{componenttype}", produces = {"application/xml","application/json","text/plain"})
    public ResponseEntity<String> getDocumentation(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable String componenttype) throws Exception {

    	plainResponse = true;

		try {
            integration = integrationRuntime.getIntegration();
            String documentation = integration.getDocumentation(componenttype, mediaType);
    		if(documentation.startsWith("Unknown")) {
				return ResponseUtil.createFailureResponse(integrationId, mediaType,"/integration/{integrationId}/flow/documentation/{componenttype}",documentation);
			}
			return ResponseUtil.createSuccessResponse(integrationId, mediaType,"/integration/{integrationId}/flow/documentation/{componenttype}",documentation,plainResponse);
		} catch (Exception e) {
			log.error("Get documentation failed",e);
			return ResponseUtil.createFailureResponse(integrationId, mediaType,"/integration/{integrationId}/flow/documentation/{componenttype}",e.getMessage());
		}
    }

	@GetMapping(path = "/integration/{integrationId}/flow/components", produces = {"application/xml","application/json","text/plain"})
	public ResponseEntity<String> getComponents(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @RequestHeader("IncludeCustomComponents") Boolean includeCustomComponents, @PathVariable Long integrationId) throws Exception {

		plainResponse = true;

		try {
			integration = integrationRuntime.getIntegration();
			String components = integration.getComponents(includeCustomComponents, mediaType);
			if(components.startsWith("Unknown")) {
				return ResponseUtil.createFailureResponse(integrationId, mediaType,"/integration/{integrationId}/flow/schema/{componenttype}",components);
			}
			return ResponseUtil.createSuccessResponse(integrationId, mediaType,"/integration/{integrationId}/flow/schema/{componenttype}",components,plainResponse);
		} catch (Exception e) {
			log.error("Get components failed",e);
			return ResponseUtil.createFailureResponse(integrationId, mediaType,"/integration/{integrationId}/flow/schema/{componenttype}",e.getMessage());
		}
	}

    @GetMapping(path = "/integration/{integrationId}/flow/schema/{componenttype}", produces = {"application/xml","application/json","text/plain"})
    public ResponseEntity<String> getComponentSchema(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable String componenttype) throws Exception {

    	plainResponse = true;

		try {
            integration = integrationRuntime.getIntegration();
            String documentation = integration.getComponentSchema(componenttype, mediaType);
    		if(documentation.startsWith("Unknown")) {
				//return empty default if unknown
				documentation = "{\"component\": {\"kind\": \"block\"},\"properties\": {    \"\": { \"kind\": \"\", \"displayName\": \"\", \"group\": \"\", \"label\": \"\", \"required\": false, \"type\": \"string\", \"javaType\": \"\", \"deprecated\": false, \"deprecationNote\": \"\", \"autowired\": false, \"secret\": false, \"description\": \"\" }}}";
				return ResponseUtil.createSuccessResponse(integrationId, mediaType,"/integration/{integrationId}/flow/schema/{componenttype}",documentation,plainResponse);
			}
			return ResponseUtil.createSuccessResponse(integrationId, mediaType,"/integration/{integrationId}/flow/schema/{componenttype}",documentation,plainResponse);
		} catch (Exception e) {
			log.error("Get component schema failed",e);
			return ResponseUtil.createFailureResponse(integrationId, mediaType,"/integration/{integrationId}/flow/schema/{componenttype}",e.getMessage());
		}
    }

    @GetMapping(path = "/integration/{integrationId}/flow/options/{componenttype}", produces = {"application/xml","application/json","text/plain"})
    public ResponseEntity<String> getComponentOptions(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable String componenttype) throws Exception {

    	plainResponse = true;

		try {
            integration = integrationRuntime.getIntegration();
            String documentation = integration.getComponentParameters(componenttype, mediaType);
    		if(documentation.startsWith("Unknown")) {
				return ResponseUtil.createFailureResponse(integrationId, mediaType,"/integration/{integrationId}/flow/options/{componenttype}",documentation);
			}
			return ResponseUtil.createSuccessResponse(integrationId, mediaType,"/integration/{integrationId}/flow/options/{componenttype}",documentation,plainResponse);
		} catch (Exception e) {
			log.error("Get components options failed",e);
			return ResponseUtil.createFailureResponse(integrationId, mediaType,"/integration/{integrationId}/flow/options/{componenttype}",e.getMessage());
		}
    }

    @GetMapping(path = "/integration/{integrationId}/flow/{flowId}/route", produces = {"application/xml","application/json"})
    public ResponseEntity<String> getCamelRoute(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable String flowId) throws Exception {

		try {
            integration = integrationRuntime.getIntegration();
            String camelRoute = integration.getCamelRouteConfiguration(flowId, mediaType);
			return ResponseUtil.createSuccessResponse(integrationId, mediaType,"/integration/{integrationId}/flow/{flowId}/route",camelRoute,true);
		} catch (Exception e) {
			log.error("Get Camel route failed",e);
			return ResponseUtil.createFailureResponse(integrationId, mediaType,"/integration/{integrationId}/flow/{flowId}/route",e.getMessage());
		}
    }

    @GetMapping(path = "/integration/{integrationId}/flow/routes", produces = {"application/xml","application/json"})
    public ResponseEntity<String> getAllCamelRoutes(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId) throws Exception {

		try {
            integration = integrationRuntime.getIntegration();
            String camelRoutes = integration.getAllCamelRoutesConfiguration(mediaType);
			return ResponseUtil.createSuccessResponse(integrationId, mediaType,"/integration/{integrationId}/flow/routes",camelRoutes,true);
		} catch (Exception e) {
			log.error("Get all Camel routes failed",e);
			return ResponseUtil.createFailureResponse(integrationId, mediaType,"/integration/{integrationId}/flow/routes",e.getMessage());
		}

    }

    @DeleteMapping(path = "/integration/{integrationId}/flow/{flowId}/remove", produces = {"application/xml","application/json","text/plain"})
    public ResponseEntity<String> removeFlow(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable String flowId) throws Exception {

        try {
            integration = integrationRuntime.getIntegration();
            Boolean removedFlow = integration.removeFlow(flowId);
            return ResponseUtil.createSuccessResponse(integrationId, mediaType,"/integration/{integrationId}/flow/{flowId}/remove",removedFlow.toString());
        } catch (Exception e) {
			log.error("Remove flow " + flowId +" failed",e);
			return ResponseUtil.createFailureResponse(integrationId, mediaType,"/integration/{integrationId}/flow/{flowId}/remove",e.getMessage());
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
