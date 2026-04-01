package org.assimbly.dil.validation;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpHead;
import org.apache.hc.core5.util.Timeout;

import javax.net.ssl.SSLException;
import java.io.IOException;


public class HttpsCertificateValidator {

    private static HttpClient httpClient;

    /**
     * Checks if the certificate of the given HTTPS url is valid or not.
     * <p/>
     * Valid means the certificate is trusted by the default system truststore or the custom trust store
     * Invalid means anything else, from expired certificate to self signed etc.
     *
     * @return ValidationResult. If a certificate is found invalid or it cannot be determined a message will be included.
     */
    public ValidationResult validate(String httpsUrlString) {
        // 1. URL cleanup (HttpClient 5 still likes clean URIs)
        httpsUrlString = httpsUrlString.replace(" ", "%20");

        // 2. Define Request-specific config (Response timeout is the new "Socket Timeout")
        RequestConfig reqConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofMilliseconds(15000))
                .setResponseTimeout(Timeout.ofMilliseconds(15000))
                .build();

        // 3. Use the ResponseHandler pattern for automatic resource management
        HttpHead request = new HttpHead(httpsUrlString);
        request.setConfig(reqConfig);

        try {
            // execute(request, responseHandler) handles the connection release automatically
            return httpClient.execute(request, response -> {
                // Check status code (e.g., 200 OK)
                if (response.getCode() >= 200 && response.getCode() < 300) {
                    return new ValidationResult(ValidationResultStatus.VALID, null);
                }
                return new ValidationResult(ValidationResultStatus.INVALID, "Status: " + response.getCode());
            });
        } catch (SSLException e) {
            return new ValidationResult(ValidationResultStatus.INVALID, e.getMessage());
        } catch (IOException e) {
            return new ValidationResult(ValidationResultStatus.UNKNOWN, e.getMessage());
        }
    }

    private Throwable getRootCause(Throwable throwable) {
        if (throwable.getCause() != null) {
            return getRootCause(throwable.getCause());
        }
        return throwable;
    }

    public enum ValidationResultStatus {
        VALID, INVALID, UNKNOWN
    }

    public record ValidationResult(ValidationResultStatus validationResultStatus, String message) {
    }

}
