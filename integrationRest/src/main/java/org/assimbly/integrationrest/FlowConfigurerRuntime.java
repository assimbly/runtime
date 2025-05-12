package org.assimbly.integrationrest;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.servlet.http.HttpServletRequest;
import org.assimbly.integration.Integration;
import org.assimbly.util.rest.ResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.NativeWebRequest;

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

    private Integration integration;

    /**
     * POST  /integration/flow/{flowId}/configure : Set configuration.
     *
     * @param flowId (FlowId)
     * @param configuration as JSON or XML
     * @return the ResponseEntity with status 200 (Successful) and status 400 (Bad Request) if the configuration failed
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping(
			path = "/integration/flow/{flowId}/configure",
			consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE},
			produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
	)
    public ResponseEntity<String> setFlowConfiguration(
			@PathVariable(value = "flowId") String flowId,
			@RequestBody String configuration,
			@Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType
	) throws Exception {

       	try {
            integration = integrationRuntime.getIntegration();
            integration.setFlowConfiguration(flowId, mediaType, configuration);
       		return ResponseUtil.createSuccessResponse(1L, mediaType,"/integration/flow/{flowId}/configure","Flow configuration set");
   		} catch (Exception e) {
			log.error("Set flow configuration failed",e);
			return ResponseUtil.createFailureResponse(1L, mediaType,"/integration/flow/{flowId}/configure",e.getMessage());
   		}
    }

    /**
     * Get  /integration/flow/{flowId}/configure : get XML configuration for integration.
     *
     * @param flowId (flowId)
     * @return the ResponseEntity with status 200 (Successful) and status 400 (Bad Request) if the configuration failed
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @GetMapping(
			path = "/integration/flow/{flowId}/configure",
			produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
	)
    public ResponseEntity<String> getFlowConfiguration(
			@PathVariable(value = "flowId") String flowId,
			@Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType
	) throws Exception {

    	plainResponse = true;

    	try {
            integration = integrationRuntime.getIntegration();
			String flowConfiguration = integration.getFlowConfiguration(flowId, mediaType);
			if(flowConfiguration.startsWith("Error")||flowConfiguration.startsWith("Warning")) {
				return ResponseUtil.createFailureResponse(1L, mediaType,"/integration/flow/{flowId}/configure",flowConfiguration);
			}
			return ResponseUtil.createSuccessResponse(1L, mediaType,"/integration/flow/{flowId}/configure",flowConfiguration,plainResponse);
   		} catch (Exception e) {
			log.error("Get flow configuration failed",e);
			return ResponseUtil.createFailureResponse(1L, mediaType,"/integration/flow/{flowId}/configure",e.getMessage());
   		}
    }

	@GetMapping(
			path = "/integration/flow/{flowId}/isconfigured",
			produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
	)
	public ResponseEntity<String> hasFlow(
			@PathVariable(value = "flowId") String flowId,
			@Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType
	) throws Exception {

		try {
			integration = integrationRuntime.getIntegration();
			boolean hasFlow = integration.hasFlow(flowId);
			return ResponseUtil.createSuccessResponse(1L, mediaType,"/integration/flow/{flowId}/hasflow", Boolean.toString(hasFlow));
		} catch (Exception e) {
			log.error("Check if integration has flow with id=" + flowId + " failed",e);
			return ResponseUtil.createFailureResponse(1L, mediaType,"/integration/flow/{flowId}/hasflow",e.getMessage());
		}

	}

    @GetMapping(
			path = "/integration/flow/documentation/version",
			produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
	)
    public ResponseEntity<String> getDocumentationVersion(@Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType) throws Exception {

		try {
            integration = integrationRuntime.getIntegration();
            String documentation = integration.getDocumentationVersion();
			return ResponseUtil.createSuccessResponse(1L, mediaType,"/integration/flow/documentation/version",documentation,plainResponse);
		} catch (Exception e) {
			log.error("Get documentation version failed",e);
			return ResponseUtil.createFailureResponse(1L, mediaType,"/integration/flow/documentation/version",e.getMessage());
		}
    }

    @GetMapping(
			path = "/integration/flow/documentation/{componenttype}",
			produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
	)
    public ResponseEntity<String> getDocumentation(
			@PathVariable(value = "componenttype") String componenttype,
			@Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType
	) throws Exception {

    	plainResponse = true;

		try {
            integration = integrationRuntime.getIntegration();
            String documentation = integration.getDocumentation(componenttype, mediaType);
    		if(documentation.startsWith("Unknown")) {
				return ResponseUtil.createFailureResponse(1L, mediaType,"/integration/flow/documentation/{componenttype}",documentation);
			}
			return ResponseUtil.createSuccessResponse(1L, mediaType,"/integration/flow/documentation/{componenttype}",documentation,plainResponse);
		} catch (Exception e) {
			log.error("Get documentation failed",e);
			return ResponseUtil.createFailureResponse(1L, mediaType,"/integration/flow/documentation/{componenttype}",e.getMessage());
		}
  }


	@GetMapping(
			path = "/integration/flow/components",
			produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
	)
	public ResponseEntity<String> getComponents(
			@Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType,
			@RequestHeader(value = "IncludeCustomComponents") boolean includeCustomComponents
	) throws Exception {

		log.info("Get components");

		plainResponse = true;

		try {
			integration = integrationRuntime.getIntegration();
			String components = integration.getComponents(includeCustomComponents, mediaType);

			if(components.startsWith("Unknown")) {
				return ResponseUtil.createFailureResponse(1L, mediaType,"/integration/flow/components",components);
			}
			return ResponseUtil.createSuccessResponse(1L, mediaType,"/integration/flow/components",components,plainResponse);
		} catch (Exception e) {
			log.error("Get components failed",e);
			return ResponseUtil.createFailureResponse(1L, mediaType,"/integration/flow/components",e.getMessage());
		}
	}

    @GetMapping(
			path = "/integration/flow/schema/{componenttype}",
			produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
	)
    public ResponseEntity<String> getComponentSchema(
			@PathVariable(value = "componenttype") String componenttype,
			@Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType
	) throws Exception {

    	plainResponse = true;

		try {
            integration = integrationRuntime.getIntegration();
            String documentation = integration.getComponentSchema(componenttype, mediaType);
    		if(documentation.startsWith("Unknown")) {
				//return empty default if unknown
				documentation = "{\"component\": {\"kind\": \"block\"},\"properties\": {    \"\": { \"kind\": \"\", \"displayName\": \"\", \"group\": \"\", \"label\": \"\", \"required\": false, \"type\": \"string\", \"javaType\": \"\", \"deprecated\": false, \"deprecationNote\": \"\", \"autowired\": false, \"secret\": false, \"description\": \"\" }}}";
				return ResponseUtil.createSuccessResponse(1L, mediaType,"/integration/flow/schema/{componenttype}",documentation,plainResponse);
			}
			return ResponseUtil.createSuccessResponse(1L, mediaType,"/integration/flow/schema/{componenttype}",documentation,plainResponse);
		} catch (Exception e) {
			log.error("Get component schema failed",e);
			return ResponseUtil.createFailureResponse(1L, mediaType,"/integration/flow/schema/{componenttype}",e.getMessage());
		}
    }

    @GetMapping(
			path = "/integration/flow/options/{componenttype}",
			produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
	)
    public ResponseEntity<String> getComponentOptions(
			@PathVariable(value = "componenttype") String componenttype,
			@Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType
	) throws Exception {

    	plainResponse = true;

		try {
            integration = integrationRuntime.getIntegration();
            String documentation = integration.getComponentParameters(componenttype, mediaType);
    		if(documentation.startsWith("Unknown")) {
				return ResponseUtil.createFailureResponse(1L, mediaType,"/integration/flow/options/{componenttype}",documentation);
			}
			return ResponseUtil.createSuccessResponse(1L, mediaType,"/integration/flow/options/{componenttype}",documentation,plainResponse);
		} catch (Exception e) {
			log.error("Get components options failed",e);
			return ResponseUtil.createFailureResponse(1L, mediaType,"/integration/flow/options/{componenttype}",e.getMessage());
		}
    }

    @GetMapping(
			path = "/integration/flow/{flowId}/route",
			produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE}
	)
    public ResponseEntity<String> getCamelRoute(
			@PathVariable(value = "flowId") String flowId,
			@Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType
	) throws Exception {

		try {
            integration = integrationRuntime.getIntegration();
            String camelRoute = integration.getCamelRouteConfiguration(flowId, mediaType);
			return ResponseUtil.createSuccessResponse(1L, mediaType,"/integration/flow/{flowId}/route",camelRoute,true);
		} catch (Exception e) {
			log.error("Get Camel route failed",e);
			return ResponseUtil.createFailureResponse(1L, mediaType,"/integration/flow/{flowId}/route",e.getMessage());
		}
    }

	@GetMapping(
			path = "/integration/flow/step/{templatename}",
			produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
	)
	public ResponseEntity<String> getStepTemplate(
			@PathVariable(value = "templatename") String templatename,
			@Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType
	) throws Exception {

		try {
			integration = integrationRuntime.getIntegration();
			String stepTemplate = integration.getStepTemplate(mediaType, templatename);
			return ResponseUtil.createSuccessResponse(1L, mediaType,"/integration/flow/step/{templatename}",stepTemplate,true);
		} catch (Exception e) {
			log.error("Get step template failed",e);
			return ResponseUtil.createFailureResponse(1L, mediaType,"/integration/flow/step/{templatename}",e.getMessage());
		}

	}

	@GetMapping(
			path = "/integration/flow/list/steps",
			produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE}
	)
	public ResponseEntity<String> getListOfStepTemplates(@Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType) throws Exception {

		try {
			integration = integrationRuntime.getIntegration();
			String stepTemplates = integration.getListOfStepTemplates();
			return ResponseUtil.createSuccessResponse(1L, mediaType,"/integration/flow/list/steps",stepTemplates,true);
		} catch (Exception e) {
			log.error("Get all step templates failed",e);
			return ResponseUtil.createFailureResponse(1L, mediaType,"/integration/flow/list/steps",e.getMessage());
		}

	}

	@DeleteMapping(
			path = "/integration/flow/{flowId}/remove",
			produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
	)
    public ResponseEntity<String> removeFlow(
			@PathVariable(value = "flowId") String flowId,
			@Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType
	) throws Exception {

        try {
            integration = integrationRuntime.getIntegration();
            boolean removedFlow = integration.removeFlow(flowId);
            return ResponseUtil.createSuccessResponse(1L, mediaType,"/integration/flow/{flowId}/remove", Boolean.toString(removedFlow));
        } catch (Exception e) {
			log.error("Remove flow " + flowId +" failed",e);
			return ResponseUtil.createFailureResponse(1L, mediaType,"/integration/flow/{flowId}/remove",e.getMessage());
        }

    }

    // Generates a generic error response (exceptions outside try catch):
    @ExceptionHandler({Exception.class})
    public ResponseEntity<String> integrationErrorHandler(Exception error, NativeWebRequest request) throws Exception {

		HttpServletRequest httpRequest = request.getNativeRequest(HttpServletRequest.class);

		String mediaType = httpRequest != null ? httpRequest.getHeader("ACCEPT") : MediaType.APPLICATION_JSON_VALUE;
		String path = httpRequest != null ? httpRequest.getRequestURI() : "/";
		String message = error.getMessage();

    	return ResponseUtil.createFailureResponse(1L, mediaType,path,message);
    }

}
