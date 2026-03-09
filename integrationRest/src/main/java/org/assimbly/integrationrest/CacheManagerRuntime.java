package org.assimbly.integrationrest;

import io.swagger.v3.oas.annotations.Parameter;
import org.assimbly.integration.Integration;
import org.assimbly.util.rest.ResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


/**
 * REST controller for managing Security.
 */
@RestController
@RequestMapping("/api")
public class CacheManagerRuntime {

    private final Logger log = LoggerFactory.getLogger(CacheManagerRuntime.class);

    private final Integration integration;

    public CacheManagerRuntime(IntegrationRuntime integrationRuntime) {
        this.integration = integrationRuntime.getIntegration();
    }

    /**
     * DELETE  /cache/{flowId} : Delete cache entry
     *
     * @return the ResponseEntity with status 200 (Successful) and status 400 (Bad Request) if delete failed
     */
    @DeleteMapping(
            path = "/cache/{flowId}",
            consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> delCacheEntry(
            @PathVariable(value = "flowId") String flowId,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType
    ) {

        log.debug("REST request to delete flowId {} from cache", flowId);

        try {
            integration.deleteCacheEntry(flowId);
            return ResponseUtil.createSuccessResponse(1L, mediaType,"/cache/{flowId}","OK");
        } catch (Exception e) {
            return ResponseUtil.createFailureResponse(1L, mediaType,"/cache/{flowId}",e.getMessage());
        }

    }
}
