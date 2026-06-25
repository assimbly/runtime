package org.assimbly.dil.blocks.connections.auth;

import org.apache.camel.CamelContext;
import org.apache.camel.support.jsse.SSLContextParameters;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.assimbly.dil.transpiler.ssl.SSLConfiguration;
import org.assimbly.util.BaseDirectory;
import org.jasypt.properties.EncryptableProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MutualSSL {

    protected Logger log = LoggerFactory.getLogger(getClass());

    private static final String SEP = "/";
    private static final String SECURITY_PATH = "security";
    private static final String TRUSTSTORE_FILE = "truststore.jks";
    private static final String KEYSTORE_PWD = "KEYSTORE_PWD";
    private final String baseDir = BaseDirectory.getInstance().getBaseDirectory();

    private final CamelContext context;
    private final EncryptableProperties properties;
    private final String connectionId;

    private String certificate;
    private String password;

    public MutualSSL(CamelContext context, EncryptableProperties properties, String connectionId) {
        this.context = context;
        this.properties = properties;
        this.connectionId = connectionId;
    }

    public void start() throws Exception {

        log.info("Setting Mutual SSL for connection={}",connectionId);

        setFields();

        if (certificate != null && password != null) {
            addToRegistry(connectionId);
        } else {
            throw new Exception("Mutual SSL: Certificate & password are required");
        }

    }

    private void setFields(){

        certificate = properties.getProperty("connection." + connectionId + ".certificate");
        password = properties.getProperty("connection." + connectionId + ".password");

    }

    private void addToRegistry(String connectionId) throws Exception {

        String baseDir2 = FilenameUtils.separatorsToUnix(baseDir);
        String truststorePath = baseDir2 + SEP + SECURITY_PATH + SEP + TRUSTSTORE_FILE;

        SSLConfiguration sslConfiguration = new SSLConfiguration();
        SSLContextParameters sslContextParameters = sslConfiguration.createRuntimeSSLContext(
                certificate, password, truststorePath, getKeystorePassword()
        );

        context.getRegistry().bind(connectionId, sslContextParameters);

    }

    private String getKeystorePassword() {
        String keystorePwd = System.getenv(KEYSTORE_PWD);
        if(StringUtils.isEmpty(keystorePwd)) {
            return "supersecret";
        }

        return keystorePwd;
    }

}