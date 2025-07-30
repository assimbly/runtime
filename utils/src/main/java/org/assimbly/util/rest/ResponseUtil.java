package org.assimbly.util.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Utility class for HTTP body creation.
 */
public final class ResponseUtil {

	private static final Logger log = LoggerFactory.getLogger("org.assimbly.util.rest.ResponseUtil");
	
    private static ResponseEntity<String> response;

	private ResponseUtil() {
    }

    public static ResponseEntity<String> createSuccessResponse(long connectorId, String mediaType, String path, String message) {

        log.debug("REST request with path {} for gateway with id {}", path, connectorId);
    	
    	switch (mediaType.toLowerCase()) {    	
	        case "application/json":
	        	response = ResponseEntity.ok()
	        		.body(BodyUtil.createSuccessJSONResponse(connectorId, path, message));
	            break;
	        case "application/xml":
	        	response = ResponseEntity.ok()
	        		.body(BodyUtil.createSuccessXMLResponse(connectorId, path, message));
	            break;
	        default: 
	        	response = ResponseEntity.ok()
	        		.body(BodyUtil.createSuccessTEXTResponse(message));
	            break;
    	}
    	
   		return response;    	
    }

    public static ResponseEntity<String> createSuccessResponse(long connectorId, String mediaType, String path, String message, boolean plainResponse) {

        log.debug("REST request with path {} for gateway with id {}", path, connectorId);
    
    	if(plainResponse) {
        	response = ResponseEntity.ok()
	        		.body(message);
    	}else {

        	switch (mediaType.toLowerCase()) {    	
		        case "application/json":
		        	response = ResponseEntity.ok()
		        		.body(BodyUtil.createSuccessJSONResponse(connectorId, path, message));
		            break;
		        case "application/xml":
		        	response = ResponseEntity.ok()
		        		.body(BodyUtil.createSuccessXMLResponse(connectorId, path, message));
		            break;
		        default: 
		        	response = ResponseEntity.ok()
		        		.body(BodyUtil.createSuccessTEXTResponse(message));
		            break;
        	}
    	}
    	
    	return response;	
   	}

    public static ResponseEntity<String> createSuccessResponseWithHeaders(long connectorId, String mediaType, String path, String message, String headerMessage, String headerParam) {

        log.debug("REST request with path {} for gateway with id {}", path, connectorId);
    	
    	switch (mediaType.toLowerCase()) {    	
	        case "application/json":
	        	response = ResponseEntity.ok().headers(HeaderUtil.createAlert(headerMessage,headerParam))
	        		.body(BodyUtil.createSuccessJSONResponse(connectorId, path, message));
	            break;
	        case "application/xml":
	        	response = ResponseEntity.ok().headers(HeaderUtil.createAlert(headerMessage,headerParam))
	        		.body(BodyUtil.createSuccessXMLResponse(connectorId, path, message));
	            break;
	        default: 
	        	response = ResponseEntity.ok().headers(HeaderUtil.createAlert(headerMessage,headerParam))
	        		.body(BodyUtil.createSuccessTEXTResponse(message));
	            break;
    	}
    	
   		return response;    	
    }    
    
    public static ResponseEntity<String> createFailureResponse(long connectorId, String mediaType, String path, String message) {

        log.error("REST request with path {} for gateway with id {} failed.", path, connectorId);

    	switch (mediaType.toLowerCase()) {    	
	        case "application/json":
	        	response = ResponseEntity.badRequest()
	        		.body(BodyUtil.createFailureJSONResponse(connectorId, path, message));
	            break;
	        case "application/xml":
	        	response = ResponseEntity.badRequest()
	        		.body(BodyUtil.createFailureXMLResponse(connectorId, path, message));
	
	            break;
	        default: 
	        	response = ResponseEntity.badRequest()
	        		.body(BodyUtil.createFailureTEXTResponse(message));
	            break;
    	}
		
		return response;
	}

	public static ResponseEntity<String> createFailureResponse(long connectorId, String mediaType, String path, String message, boolean plainResponse) {

        log.error("REST request with path {} for gateway with id {} failed.", path, connectorId);

		if(plainResponse) {
			response = ResponseEntity.badRequest()
					.body(message);
		}else {
			switch (mediaType.toLowerCase()) {
				case "application/json":
					response = ResponseEntity.badRequest()
							.body(BodyUtil.createFailureJSONResponse(connectorId, path, message));
					break;
				case "application/xml":
					response = ResponseEntity.badRequest()
							.body(BodyUtil.createFailureXMLResponse(connectorId, path, message));

					break;
				default:
					response = ResponseEntity.badRequest()
							.body(BodyUtil.createFailureTEXTResponse(message));
					break;
			}
		}

		return response;
	}

	public static ResponseEntity<String> createFailureResponseWithHeaders(long connectorId, String mediaType, String path, String message, String headerMessage, String headerParam) {

        log.error("REST request with path {} for gateway with id {} failed.", path, connectorId);

    	switch (mediaType.toLowerCase()) {    	
	        case "application/json":
	        	response = ResponseEntity.status(400).headers(HeaderUtil.createAlert(headerMessage,headerParam))
	        		.body(BodyUtil.createFailureJSONResponse(connectorId, path, message));
	            break;
	        case "application/xml":
	        	response = ResponseEntity.badRequest().headers(HeaderUtil.createAlert(headerMessage,headerParam))
	        		.body(BodyUtil.createFailureXMLResponse(connectorId, path, message));
	
	            break;
	        default: 
	        	response = ResponseEntity.badRequest().headers(HeaderUtil.createAlert(headerMessage,headerParam))
	        		.body(BodyUtil.createFailureTEXTResponse(message));
	            break;
    	}
		
		return response;
	}


	public static ResponseEntity<String> createNoContentResponse(long connectorId, String path) {

        log.debug("REST request with path {} for gateway with id {}", path, connectorId);

		response = ResponseEntity.noContent().build();
		return response;
	}

	public static ResponseEntity<String> createNotModifiedResponse(long connectorId, String path) {

        log.debug("REST request with path {} for gateway with id {}", path, connectorId);

		response = ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
		return response;
	}
    
}
