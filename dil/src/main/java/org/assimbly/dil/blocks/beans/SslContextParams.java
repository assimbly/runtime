package org.assimbly.dil.blocks.beans;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.http.HttpComponent;
import org.apache.camel.support.jsse.KeyManagersParameters;
import org.apache.camel.support.jsse.KeyStoreParameters;
import org.apache.camel.support.jsse.SSLContextParameters;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;


public class SslContextParams implements Processor {

    protected Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public void process(Exchange exchange) throws Exception {

        try {
            String resource = exchange.getProperty("resource", String.class);
            String authPassword = exchange.getProperty("authPassword", String.class);

            KeyStoreParameters ksp = new KeyStoreParameters();
            ksp.setResource(resource);
            ksp.setPassword(authPassword);

            KeyManagersParameters kmp = new KeyManagersParameters();
            kmp.setKeyStore(ksp);
            kmp.setKeyPassword(authPassword);

            SSLContextParameters scp = new SSLContextParameters();
            scp.setKeyManagers(kmp);

            Set<String> componentNamesSet = exchange.getContext().getComponentNames();

            setSSLContextParametersOnHttpComponent(exchange, scp, componentNamesSet.contains("https4") ? "https4" : "https");
        } catch (Exception e) {
            System.out.println(" >> Error on process ");
        }
    }

    private static void setSSLContextParametersOnHttpComponent(Exchange exchange, SSLContextParameters scp, String componentName) {
        // Setting SSLContextParameters to HttpComponent
        HttpComponent httpComponent = exchange.getContext().getComponent(componentName, HttpComponent.class);
        httpComponent.setSslContextParameters(scp);
        httpComponent.setX509HostnameVerifier(new AllowAllHostnameVerifier());
        httpComponent.setUseGlobalSslContextParameters(false);
    }

}
