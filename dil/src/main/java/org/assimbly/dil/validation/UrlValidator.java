package org.assimbly.dil.validation;

import org.assimbly.util.error.ValidationErrorMessage;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UrlValidator {

    private static final int TIMEOUT = 5000;
    private static final ValidationErrorMessage UNREACHABLE_ERROR = new ValidationErrorMessage("Url is not reachable from the server!");
    private static final ValidationErrorMessage INVALID_URL_ERROR = new ValidationErrorMessage("Url is not valid!");

    public ValidationErrorMessage validate(String url) {
        final HttpURLConnection httpUrlConn;

        try {
            String decodedUrl = URLDecoder.decode(url, "UTF-8");

            if(!validateURL(decodedUrl))
                return INVALID_URL_ERROR;

            httpUrlConn = (HttpURLConnection) new URL(decodedUrl).openConnection();

            httpUrlConn.setRequestMethod("HEAD");

            // Set timeouts in milliseconds
            httpUrlConn.setConnectTimeout(TIMEOUT);
            httpUrlConn.connect();
            //need to trigger exception when no connection can be established
            httpUrlConn.getResponseMessage();
        } catch (UnknownHostException e) {
            return UNREACHABLE_ERROR;
        } catch (Exception e) {
            return new ValidationErrorMessage(e.getMessage());
        }

        return null;
    }

    public static final boolean validateURL(String url) {
        Pattern regex = Pattern.compile("^(https?):\\/\\/[-a-zA-Z0-9+&@#\\/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#\\/%=~_|]");
        Matcher matcher = regex.matcher(url);
        return matcher.find();
    }
}
