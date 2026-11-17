package org.assimbly.util.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Utility class for HTTP response creation.
 */
public final class ResponseUtil {

	private static final Logger log = LoggerFactory.getLogger("org.assimbly.util.rest.ResponseUtil");

	private ResponseUtil() {
    }

    public static ResponseEntity<String> createSuccessResponse(long connectorId, String mediaType, String path, String message) {
        logRequest(path, connectorId);
        return ResponseEntity.ok().body(successBody(connectorId, mediaType, path, message));
    }

    public static ResponseEntity<String> createSuccessResponse(long connectorId, String mediaType, String path, String message, boolean plainResponse) {
        logRequest(path, connectorId);
        return ResponseEntity.ok().body(plainResponse ? message : successBody(connectorId, mediaType, path, message));
    }

    public static ResponseEntity<String> createSuccessResponseWithHeaders(long connectorId, String mediaType, String path, String message, String headerMessage, String headerParam) {
        logRequest(path, connectorId);
        return ResponseEntity.ok().headers(HeaderUtil.createAlert(headerMessage, headerParam))
                .body(successBody(connectorId, mediaType, path, message));
    }

    public static ResponseEntity<String> createFailureResponse(long connectorId, String mediaType, String path, String message) {
        logFailedRequest(path, connectorId);
        return ResponseEntity.badRequest().body(failureBody(connectorId, mediaType, path, message));
    }

    public static ResponseEntity<String> createFailureResponse(long connectorId, String mediaType, String path, String message, boolean plainResponse) {
        logFailedRequest(path, connectorId);
        return ResponseEntity.badRequest().body(plainResponse ? message : failureBody(connectorId, mediaType, path, message));
    }

    public static ResponseEntity<String> createFailureResponseWithHeaders(long connectorId, String mediaType, String path, String message, String headerMessage, String headerParam) {
        logFailedRequest(path, connectorId);
        return ResponseEntity.badRequest().headers(HeaderUtil.createAlert(headerMessage, headerParam))
                .body(failureBody(connectorId, mediaType, path, message));
    }

    public static ResponseEntity<String> createNoContentResponse(long connectorId, String path) {
        logRequest(path, connectorId);
        return ResponseEntity.noContent().build();
    }

    public static ResponseEntity<String> createNotModifiedResponse(long connectorId, String path) {
        logRequest(path, connectorId);
        return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
    }

    /** The body in the format of the media type; any other media type than JSON or XML gets the message as it is. */
    private static String successBody(long connectorId, String mediaType, String path, String message) {
        return switch (mediaType.toLowerCase()) {
            case "application/json" -> BodyUtil.createSuccessJSONResponse(connectorId, path, message);
            case "application/xml" -> BodyUtil.createSuccessXMLResponse(connectorId, path, message);
            default -> BodyUtil.createSuccessTEXTResponse(message);
        };
    }

    private static String failureBody(long connectorId, String mediaType, String path, String message) {
        return switch (mediaType.toLowerCase()) {
            case "application/json" -> BodyUtil.createFailureJSONResponse(connectorId, path, message);
            case "application/xml" -> BodyUtil.createFailureXMLResponse(connectorId, path, message);
            default -> BodyUtil.createFailureTEXTResponse(message);
        };
    }

    private static void logRequest(String path, long connectorId) {
        log.debug("REST request with path {} for gateway with id {}", path, connectorId);
    }

    private static void logFailedRequest(String path, long connectorId) {
        log.error("REST request with path {} for gateway with id {} failed.", path, connectorId);
    }

}
