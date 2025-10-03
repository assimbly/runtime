package org.assimbly.integrationrest;

import groovy.lang.Lazy;
import io.swagger.v3.oas.annotations.Parameter;
import org.assimbly.integration.Integration;
import org.assimbly.integration.impl.CamelIntegration;
import org.assimbly.util.rest.ResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import java.net.URISyntaxException;
import java.util.Properties;
import java.util.TreeMap;


/**
 * Resource to return information about the currently running Spring profiles.
 */
@Component
@RestController
@RequestMapping("/api")
public class IntegrationRuntime {

    protected Logger log = LoggerFactory.getLogger(getClass());

    private final Integration integration = new CamelIntegration(true);

    private boolean plainResponse;

    private boolean integrationIsStarting;

    public IntegrationRuntime() throws Exception {
        // Empty constructor needed because of Exception.
    }

    /**
     * Get  /start : starts integration.
     *
     * @return The ResponseEntity with status 200 (Successful) and status 400 (Bad Request) if the starting integration failed
     */
    @GetMapping(
            path = "/integration/start",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> start(@Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType) {

        try {

            if (integration.isStarted()) {
                return ResponseUtil.createFailureResponse(1L, mediaType, "/integration/start", "Integration already running");
            } else {
                integration.start();
                return ResponseUtil.createSuccessResponse(1L, mediaType, "/integration/start", "Integration started");
            }

        } catch (Exception e) {
            log.error("Start integration failed",e);
            return ResponseUtil.createFailureResponse(1L, mediaType, "/integration/start", e.getMessage());
        }

    }

    /**
     * GET  /stop : stops integration.
     *
     * @return the ResponseEntity with status 200 (Successful) and status 400 (Bad Request) if the stopping integration failed
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @GetMapping(
            path = "/integration/stop",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> stop(@Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType) throws Exception {

        try {
            integration.stop();
            return ResponseUtil.createSuccessResponse(1L, mediaType,"/integration/stop","Integration stopped");
        } catch (Exception e) {
            log.error("Stop integration failed",e);
            return ResponseUtil.createFailureResponse(1L, mediaType,"/integration/stop",e.getMessage());
        }

    }

    /**
     * GET  /info : info of an integration.
     *
     * @return the ResponseEntity with status 200 (Successful) and status 400 (Bad Request) if the stopping integration failed
     */
    @GetMapping(
            path = "/integration/info",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> info(@Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType) {

        try {
            String info = integration.info(mediaType);
            return ResponseUtil.createSuccessResponse(1L, mediaType,"/integration/info",info,true);
        } catch (Exception e) {
            log.error("Retrieving info on integration failed",e);
            return ResponseUtil.createFailureResponse(1L, mediaType,"/integration/info",e.getMessage());
        }

    }

    /**
     * GET  /istarted : checks if integration is started.
     *
     * @return the ResponseEntity with status 200 (Successful) and status 400 (Bad Request) if the stopping integration failed
     */
    @GetMapping(
            path = "/integration/isstarted",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> isStarted(@Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType) {

        try {
            boolean started = integration.isStarted();
            return ResponseUtil.createSuccessResponse(1L, mediaType,"/integration/isstarted", Boolean.toString(started));
        } catch (Exception e) {
            log.error("Check if integration is started failed",e);
            return ResponseUtil.createFailureResponse(1L, mediaType,"/integration/isstarted",e.getMessage());
        }

    }

    @GetMapping(
            path = "/integration/lasterror",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> getLastError(@Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType) {

        try {
            String error = integration.getLastError();
            return ResponseUtil.createSuccessResponse(1L, mediaType,"/integration/lasterror",error,plainResponse);
        } catch (Exception e) {
            log.error("Get last error for integration failed",e);
            return ResponseUtil.createFailureResponse(1L, mediaType,"/integration/lasterror",e.getMessage());
        }

    }

    /**
     * POST  /integration/resolvedependencybyscheme/{scheme} : Resolve the Mave dependency by URI scheme (for example SFTP or FILE).
     *
     * @return the ResponseEntity with status 200 (Successful) and status 400 (Bad Request) if the configuration failed
     */
    @PostMapping(
            path = "/integration/resolvedependencybyscheme/{scheme}",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> resolveDepedencyByScheme(
            @PathVariable(value = "scheme") String scheme,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType
    ) {

        try {
            String result = integration.resolveDependency(scheme);
            log.info("resolveDepedencyByScheme Result: {}", result);
            if(result.contains("Error")){
                log.info("Returns an error");
                return ResponseUtil.createFailureResponse(1L, mediaType,"/integration/resolvedependency/{groupId}/{artifactId}/{version}",result);
            }
            return ResponseUtil.createSuccessResponse(1L, mediaType,"/integration/resolvedependency/{groupId}/{artifactId}/{version}",result);
        } catch (Exception e) {
            log.error("Resolve dependency for scheme={} failed", scheme, e);
            return ResponseUtil.createFailureResponse(1L, mediaType,"/integration/resolvedependency/{groupId}/{artifactId}/{version}",e.getMessage());
        }

    }

    @GetMapping(
            path = "/integration/basedirectory",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> getBaseDirectory(@Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType) {

        plainResponse = true;

        try {
            String directory = integration.getBaseDirectory();
            return ResponseUtil.createSuccessResponse(1L, mediaType,"/integration/basedirectory",directory,plainResponse);
        } catch (Exception e) {
            log.error("Get base directory for Assimbly failed",e);
            return ResponseUtil.createFailureResponse(1L, mediaType,"/integration/basedirectory",e.getMessage());
        }

    }

    @PostMapping(
            path = "/integration/basedirectory",
            consumes = {MediaType.TEXT_PLAIN_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> setBaseDirectory(
            @RequestBody String directory,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType
    ) {

        plainResponse = true;

        try {
            integration.setBaseDirectory(directory);
            return ResponseUtil.createSuccessResponse(1L, mediaType,"/integration/basedirectory","success",plainResponse);
        } catch (Exception e) {
            log.error("Set base directory for Assimbly failed",e);
            return ResponseUtil.createFailureResponse(1L, mediaType,"/integration/basedirectory",e.getMessage());
        }

    }

    @GetMapping(
            path = "/integration/list/flows",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> getListOfFlows(
            @RequestParam(required = false, value = "filterByStatus") String filter,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType
    ) {

        try {
            String flows = integration.getListOfFlows(filter, mediaType);
            return ResponseUtil.createSuccessResponse(1L, mediaType,"/integration/list/flows",flows,true);
        } catch (Exception e) {
            log.error("Get list of flows failed",e);
            return ResponseUtil.createFailureResponse(1L, mediaType,"/integration/list/flows",e.getMessage());
        }

    }

    @GetMapping(
            path = "/integration/list/flows/details",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> getRunningFlowsDetails(
            @RequestParam(required = false, value = "filterByStatus") String filter,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType
    ) {

        try {
            String flowsDetails = integration.getListOfFlowsDetails(filter, mediaType);
            return ResponseUtil.createSuccessResponse(1L, mediaType,"/integration/list/flows",flowsDetails,true);
        } catch (Exception e) {
            log.error("Get list of flows failed",e);
            return ResponseUtil.createFailureResponse(1L, mediaType,"/integration/list/flows/details",e.getMessage());
        }

    }


    @PostMapping(
            path = "/integration/list/soap/action",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> getListOfSoapActions(
            @RequestBody String url,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType
    ) {

        try {
            String flows = integration.getListOfSoapActions(url, mediaType);
            return ResponseUtil.createSuccessResponse(1L, mediaType,"/integration/list/soap/actions",flows,true);
        } catch (Exception e) {
            log.error("Get list of soap actions failed",e);
            return ResponseUtil.createFailureResponse(1L, mediaType,"/integration/list/soap/actions",e.getMessage());
        }

    }

    @GetMapping(
            path = "/integration/count/flows",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> countFlows(
            @RequestParam(required = false, value = "filterByStatus") String filter,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType
    ) {

        try {
            String flowsCount = integration.countFlows(filter, mediaType);
            return ResponseUtil.createSuccessResponse(1L, mediaType,"/integration/count/flows",flowsCount,false);
        } catch (Exception e) {
            log.error("Count running flows failed",e);
            return ResponseUtil.createFailureResponse(1L, mediaType,"/integration/count/flows",e.getMessage());
        }

    }

    @GetMapping(
            path = "/integration/count/steps",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> countSteps(
            @RequestParam(required = false, value = "filterByStatus") String filter,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType
    ) {

        try {
            String stepsCount = integration.countSteps(filter, mediaType);
            return ResponseUtil.createSuccessResponse(1L, mediaType,"/integration/count/steps",stepsCount,false);
        } catch (Exception e) {
            log.error("Count running steps failed",e);
            return ResponseUtil.createFailureResponse(1L, mediaType,"/integration/count/steps",e.getMessage());
        }

    }

    @GetMapping(
            path = "/integration/numberofalerts",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> getIntegrationNumberOfAlerts(@Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType) {

        try {
            TreeMap<String,String> numberOfEntriesList = integration.getIntegrationAlertsCount();

            return ResponseUtil.createSuccessResponse(1L, mediaType,"/integration/flow/failedlog}",numberOfEntriesList.toString());
        } catch (Exception e) {
            log.error("Get number of alerts failed",e);
            return ResponseUtil.createFailureResponse(1L, mediaType,"/integration/flow/failedmessages",e.getMessage());
        }
    }

    @GetMapping(
            path = "/integration/threads",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> getThreads(
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType,
            @RequestHeader(required = false, defaultValue = "", value = "filter") String filter,
            @RequestHeader(required = false, value = "topEntries") Integer topEntries
    ) {

        try {

            if (topEntries == null) {
                topEntries = 0;
            }

            String threads = integration.getThreads(mediaType, filter, topEntries);
            return ResponseUtil.createSuccessResponse(1L, mediaType,"/integration/threads",threads,true);
        } catch (Exception e) {
            log.error("Can't retrieve list of threads",e);
            return ResponseUtil.createFailureResponse(1L, mediaType,"/integration/threads",e.getMessage());
        }

    }

    /*
     * POST  /integration/collectors/add : Set configuration for multiple collectors
     *
     * @param collectorId (CollectorId)
     * @param configuration as JSON or XML
     * @return the ResponseEntity with status 200 (Successful) and status 400 (Bad Request) if setting of the configuration failed
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping(
            path = "/integration/collectors/add",
            consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> addCollectorConfigurations(
            @RequestBody String configuration,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType
    ) {

        log.info("Add collectors. Configuration: \n\n{}\n", configuration);

        try {
            String result = integration.addCollectorsConfiguration(mediaType, configuration);
            if(!result.equalsIgnoreCase("configured")){
                log.error("Add collector failed. Message: {}", result);
                return ResponseUtil.createFailureResponse(1L, mediaType,"/integration/collectors/add",result);
            }

            return ResponseUtil.createSuccessResponse(1L, mediaType,"/integration/collectors/add",result);
        } catch (Exception e) {
            log.error("Add collector failed",e);
            return ResponseUtil.createFailureResponse(1L, mediaType,"/integration/collectors/add",e.getMessage());
        }

    }

    /**
     * POST  /integration/collector/{collectorId}/add : Set the configuraton of a collector
     *
     * @param collectorId (CollectorId)
     * @param configuration as JSON or XML
     * @return the ResponseEntity with status 200 (Successful) and status 400 (Bad Request) if setting of the configuration failed
     */
    @PostMapping(
            path = "/integration/collector/{collectorId}/add",
            consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> addCollectorConfiguration(
            @PathVariable(value = "collectorId") String collectorId,
            @RequestBody String configuration,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType
    ) {

        log.info("Add collector with id={}", collectorId);

        try {
            String result = integration.addCollectorConfiguration(collectorId,mediaType, configuration);
            if(!result.equalsIgnoreCase("configured")){
                log.error("Add collector {} failed. Message: {}", collectorId, result);
                return ResponseUtil.createFailureResponse(1L, mediaType,"/integration/collector/{collectorId}/add",result);
            }

            return ResponseUtil.createSuccessResponse(1L, mediaType,"/integration/collector/{collectorId}/add",result);
        } catch (Exception e) {
            log.error("Add collector {} failed", collectorId, e);
            return ResponseUtil.createFailureResponse(1L, mediaType,"/integration/collector/{collectorId}/add",e.getMessage());
        }

    }

    /**
     * DELETE  /integration/collector/{collectorId}/remove : Remove collector configuration
     *
     * @param collectorId (CollectorId)
     * @return the ResponseEntity with status 200 (Successful) and status 400 (Bad Request) if the remove of configuration failed
     */
    @DeleteMapping(
            path = "/integration/collector/{collectorId}/remove",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> removeCollectorConfiguration(
            @PathVariable(value = "collectorId") String collectorId,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType
    ) {

        log.info("Remove collector with id={}", collectorId);

        try {
            String result = integration.removeCollectorConfiguration(collectorId);
            return ResponseUtil.createSuccessResponse(1L, mediaType,"/integration/collector/{collectorId}/remove", result);
        } catch (Exception e) {
            log.error("Remove collector {} failed", collectorId, e);
            return ResponseUtil.createFailureResponse(1L, mediaType,"/integration/collector/{collectorId}/remove", e.getMessage());
        }

    }

    public Integration getIntegration() {
        return integration;
    }

    public void setIntegration(Properties encryptionProperties) {
        integration.setEncryptionProperties(encryptionProperties);
    }

    public void initIntegration(){

        if(!integration.isStarted() && !integrationIsStarting){
            try {

                //add notifier before starting integration
                integration.start();

            } catch (Exception e) {
                log.error("Init integration failed",e);
            }
        }

    }

}