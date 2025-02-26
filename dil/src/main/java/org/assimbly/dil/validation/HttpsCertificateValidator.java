package org.assimbly.dil.validation;

import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.HttpClientBuilder;
import org.assimbly.dil.validation.https.FileBasedTrustStore;
import org.assimbly.dil.validation.https.TrustedClientConfigurer;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HttpsCertificateValidator implements CertificateRetriever {

    private List<FileBasedTrustStore> trustStores = new ArrayList<>();
    private FileBasedTrustStore customTrustStore;
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

    public void addHttpsCertificatesToTrustStore(List<String> urls) throws Exception {
        for (int i = 0; i < urls.size(); i++) {
            String url = urls.get(i);
            ValidationResult result = validate(url);
            if (result.getValidationResultStatus().equals(ValidationResultStatus.INVALID)) {
                customTrustStore.addCertificateForHttpsUrl(url);
                buildHttpClient();
            }
        }
    }

    public void setCustomTrustStore(FileBasedTrustStore customTrustStore) {
        this.customTrustStore = customTrustStore;
    }

    public void setTrustStores(List<FileBasedTrustStore> trustStores) {
        this.trustStores = trustStores;

        // When the truststores are updated our http client should be reconfigured as well
        // to use the new truststores.
        buildHttpClient();
    }

    private void buildHttpClient() {
        TrustedClientConfigurer trustedClientConfigurer = new TrustedClientConfigurer();
        trustedClientConfigurer.setTrustStores(trustStores);

        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
        trustedClientConfigurer.configureHttpClient(httpClientBuilder);

        httpClient = httpClientBuilder.build();
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

    public static class ValidationResult {
        private final ValidationResultStatus validationResultStatus;
        private final String message;

        public ValidationResult(ValidationResultStatus validationResultStatus, String message) {
            this.validationResultStatus = validationResultStatus;
            this.message = message;
        }

        public ValidationResultStatus getValidationResultStatus() {
            return validationResultStatus;
        }

        public String getMessage() {
            return message;
        }
    }
}
