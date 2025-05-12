package org.assimbly.dil.validation;

import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.HttpClientBuilder;
import javax.net.ssl.SSLContext;
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

        if (!httpsUrlString.startsWith("https://")) {
            throw new IllegalArgumentException("Provided url " + httpsUrlString + "is not a HTTPS url");
        }

        if (httpClient == null){
            buildHttpClient();
        }

        httpsUrlString = httpsUrlString.replace(" ", "%20");

        RequestConfig reqConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(15000)
                .setConnectTimeout(15000)
                .setSocketTimeout(15000)
                .build();

        HttpHead req = new HttpHead(httpsUrlString);
        req.setConfig(reqConfig);

        try {
            httpClient.execute(req);
        } catch (SSLException e) {
            return new ValidationResult(ValidationResultStatus.INVALID, getRootCause(e).getMessage());
        } catch (IOException e) {
            return new ValidationResult(ValidationResultStatus.UNKNOWN, getRootCause(e).getMessage());
        }

        return new ValidationResult(ValidationResultStatus.VALID, null);

    }

    private static void buildHttpClient() {

        // This uses the default Java truststore
        SSLContext sslContext = SSLContexts.createSystemDefault();

        // Configure the HttpClient with the default SSL context
        httpClient = HttpClientBuilder.create()
                .setSSLContext(sslContext)
                .build();

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
