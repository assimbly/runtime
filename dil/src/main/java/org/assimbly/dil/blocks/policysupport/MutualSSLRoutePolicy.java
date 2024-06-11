package org.assimbly.dil.blocks.policysupport;

import org.apache.camel.Route;
import org.apache.camel.support.RoutePolicySupport;
import org.assimbly.util.BaseDirectory;
import org.assimbly.util.CertificatesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class MutualSSLRoutePolicy extends RoutePolicySupport {

    protected Logger log = LoggerFactory.getLogger(getClass());

    private final String baseDir = BaseDirectory.getInstance().getBaseDirectory();
    private final String keystoreFileName = "keystore.jks";

    private String resource;
    private String authPassword;

    public MutualSSLRoutePolicy(String resource, String authPassword) {
        super();
        this.resource = resource;
        this.authPassword = authPassword;
    }

    @Override
    public void onInit(Route route) {
        super.onInit(route);

        System.out.println(" > authPassword: "+authPassword);
        System.out.println(" > resource: "+resource);

        try {
            URL url = new URL(resource);
            String resourceContent = null;
            try (InputStream inputStream = url.openStream()) {
                resourceContent = new String(inputStream.readAllBytes(), StandardCharsets.ISO_8859_1);
            }
            System.out.println(" > resourceContent: "+resourceContent);

            CertificatesUtil util = new CertificatesUtil();
            String keystorePath = baseDir + "/security/" + keystoreFileName;
            util.importP12Certificate(keystorePath, "supersecret", resourceContent, authPassword);

        } catch (Exception e) {
            log.error("Error to add certificate", e);
        }
    }

}
