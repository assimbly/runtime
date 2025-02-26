package org.assimbly.dil.validation.https;

import org.apache.camel.component.http.HttpClientConfigurer;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import java.util.ArrayList;
import java.util.List;

public class TrustedClientConfigurer implements HttpClientConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(TrustedClientConfigurer.class);

    private List<FileBasedTrustStore> trustStores = new ArrayList<>();

    public void configureHttpClient(HttpClientBuilder clientBuilder) {
        CompositeTrustManager compositeTrustManager = new CompositeTrustManager();

        try {
            for(FileBasedTrustStore trustStore: trustStores) {
                compositeTrustManager.addTrustManagers(trustStore.loadTrustStore());
            }

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[] { compositeTrustManager }, null);

            SSLConnectionSocketFactory sslConnectionFactory =
                    new SSLConnectionSocketFactory(sslContext, new DefaultHostnameVerifier());

            clientBuilder.setSSLContext(sslContext);
            clientBuilder.setSSLSocketFactory(sslConnectionFactory);

            Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                    .register("https", sslConnectionFactory)
                    .build();

            PoolingHttpClientConnectionManager ccm = new PoolingHttpClientConnectionManager(registry);
            clientBuilder.setConnectionManager(ccm);

            logger.info("Setup SSL context for https4 scheme");
        } catch (Exception e) {
            logger.error("Failed to setup SSL context", e);
            throw new RuntimeException(e);
        }
    }

    public void setTrustStores(List<FileBasedTrustStore> trustStores) {
        this.trustStores = trustStores;
    }

    @Override
    public void configureHttpClient(org.apache.hc.client5.http.impl.classic.HttpClientBuilder clientBuilder) {
        //todo
    }
}
