package org.assimbly.dil.validation;

import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpHead;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import java.io.IOException;
import java.util.concurrent.TimeUnit;


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
                .setConnectionRequestTimeout(15000, TimeUnit.MILLISECONDS)
                .setConnectTimeout(15000, TimeUnit.MILLISECONDS)
                .setResponseTimeout(15000, TimeUnit.MILLISECONDS)
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

        // 1. Create a TLS Strategy instead of a Socket Factory
        // This handles the SSLContext and HostnameVerifier
        DefaultClientTlsStrategy tlsStrategy = new DefaultClientTlsStrategy(
                sslContext,
                NoopHostnameVerifier.INSTANCE
        );

        // 2. Use setTlsSocketStrategy on the Connection Manager
        var connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setTlsSocketStrategy(tlsStrategy)
                .build();

        // 3. Build the client
        HttpClients.custom()
                .setConnectionManager(connectionManager)
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
