package org.assimbly.brokerrest;

import org.assimbly.util.rest.ResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.concurrent.Callable;

/// Turns the result of a broker call into the response of a broker endpoint:
/// 200 with the result, or 400 with the reason when the call fails.
final class BrokerResponse {

    private static final Logger log = LoggerFactory.getLogger(BrokerResponse.class);

    private static final long ID = 0L;

    private BrokerResponse() {
    }

    /// Wraps the result in the media type the client asked for.
    ///
    /// @param parameters the request values to log when the call fails
    static ResponseEntity<String> respond(String mediaType, String path, Callable<String> brokerCall, Object... parameters) {
        return respond(mediaType, mediaType, path, brokerCall, parameters);
    }

    /// Returns the result as is, for calls that already deliver it in the media type the client asked for.
    ///
    /// @param parameters the request values to log when the call fails
    static ResponseEntity<String> respondConverted(String mediaType, String path, Callable<String> brokerCall, Object... parameters) {
        return respond("text", mediaType, path, brokerCall, parameters);
    }

    private static ResponseEntity<String> respond(String successMediaType, String failureMediaType, String path, Callable<String> brokerCall, Object[] parameters) {
        try {
            return ResponseUtil.createSuccessResponse(ID, successMediaType, path, brokerCall.call());
        } catch (Exception e) {
            log.error("event=brokerRequest path={} parameters={} reason={}", path, Arrays.toString(parameters), e.getMessage(), e);
            return ResponseUtil.createFailureResponse(ID, failureMediaType, path, e.getMessage());
        }
    }

}
