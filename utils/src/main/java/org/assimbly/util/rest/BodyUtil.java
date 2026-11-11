package org.assimbly.util.rest;

import org.json.JSONObject;
import org.json.XML;

import java.sql.Timestamp;

/**
 * Utility class for HTTP body creation.
 */
public final class BodyUtil {

    private static final String SUCCESS_DETAILS = "successful";
    private static final String FAILURE_DETAILS = "failed. See the log for a complete stack trace";

    private BodyUtil() {
    }

    public static String createSuccessJSONResponse(Long id, String path, String message) {
        return responseBody(id, 200, SUCCESS_DETAILS, path, message).toString();
    }

    public static String createFailureJSONResponse(Long id, String path, String message) {
        return responseBody(id, 400, FAILURE_DETAILS, path, message).toString();
    }

    public static String createSuccessXMLResponse(Long id, String path, String message) {
        return toXml(responseBody(id, 200, SUCCESS_DETAILS, path, message));
    }

    public static String createFailureXMLResponse(Long id, String path, String message) {
        return toXml(responseBody(id, 400, FAILURE_DETAILS, path, message));
    }

    public static String createSuccessTEXTResponse(String message) {
        return message;
    }

    public static String createFailureTEXTResponse(String message) {
        return message;
    }

    private static JSONObject responseBody(Long id, int status, String details, String path, String message) {
        JSONObject responseBody = new JSONObject();
        responseBody.put("id", id.toString());
        responseBody.put("timestamp", new Timestamp(System.currentTimeMillis()));
        responseBody.put("status", status);
        responseBody.put("details", details);
        responseBody.put("path", path);
        responseBody.put("message", message);
        return responseBody;
    }

    private static String toXml(JSONObject responseBody) {
        return "<response>" + System.lineSeparator() + XML.toString(responseBody) + System.lineSeparator() + "</response>";
    }

}
