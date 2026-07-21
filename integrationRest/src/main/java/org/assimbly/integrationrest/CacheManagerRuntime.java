package org.assimbly.integrationrest;

import io.swagger.v3.oas.annotations.Parameter;
import org.assimbly.docconverter.DocConverter;
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
     * GET  /cache/installed-flows : list of installed flows
     */
    @GetMapping(
            path = "/cache/installed-flows",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> getFlowStats(
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType
    ) {

        log.debug("REST get installed flows index");

        try {
            String result = integration.getInstalledFlowsIndex();

            if (mediaType.contains("xml")) {
                result = DocConverter.convertJsonToXml(result);
            }

            boolean plainResponse = !result.startsWith("Error") && !result.startsWith("Warning");

            return ResponseUtil.createSuccessResponse(1L, mediaType,"/cache/installed-flows", result, plainResponse);
        } catch (Exception e) {
            log.error("Get installed flows index failed", e);
            return ResponseUtil.createFailureResponse(1L, mediaType,"/cache/installed-flows",e.getMessage());
        }
    }

    /**
     * DELETE  /cache/{flowId}/invalidate : Invalidate a specific cache entry
     */
    @DeleteMapping(
            path = "/cache/{flowId}/invalidate",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> invalidateCacheEntry(
            @PathVariable(value = "flowId") String flowId,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType
    ) {
        log.debug("REST request to invalidate flowId {} from cache", flowId);
        try {
            integration.deleteCacheEntry(flowId);
            return ResponseUtil.createSuccessResponse(1L, mediaType, "/cache/{flowId}/invalidate", "OK");
        } catch (Exception e) {
            return ResponseUtil.createFailureResponse(1L, mediaType, "/cache/{flowId}/invalidate", e.getMessage());
        }
    }

    /**
     * DELETE  /cache/invalidate : Invalidate all cache entries
     */
    @DeleteMapping(
            path = "/cache/invalidate",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> invalidateAllCache(
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType
    ) {
        log.debug("REST request to invalidate entire cache");
        try {
            integration.clearAllCache();
            return ResponseUtil.createSuccessResponse(1L, mediaType, "/cache/invalidate", "All cache entries invalidated");
        } catch (Exception e) {
            return ResponseUtil.createFailureResponse(1L, mediaType, "/cache/invalidate", e.getMessage());
        }
    }
}
