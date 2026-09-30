package org.assimbly.dil.blocks.connections.auth;

import org.apache.camel.CamelContext;
import org.apache.camel.support.jsse.SSLContextParameters;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.assimbly.dil.transpiler.ssl.SSLConfiguration;
import org.assimbly.util.BaseDirectory;
import org.eclipse.jetty.server.Handler;
import org.jasypt.properties.EncryptableProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;
import java.util.List;

public class MutualSSL {

    protected Logger log = LoggerFactory.getLogger(getClass());

    private static final String SEP = "/";
    private static final String SECURITY_PATH = "security";
    private static final String OUTBOUND_TRUSTSTORE_FILE = "outbound-truststore.p12";
    private static final String KEYSTORE_PWD = "KEYSTORE_PWD";
    private static final String SOURCE_STEP_TYPE = "source";
    private final String baseDir = BaseDirectory.getInstance().getBaseDirectory();

    private final CamelContext context;
    private final EncryptableProperties properties;
    private final String connectionId;
    private final String stepType;
    private final String stepId;

    private String certificate;
    private String password;

    public MutualSSL(CamelContext context, EncryptableProperties properties, String connectionId, String stepType, String stepId) {
        this.context = context;
        this.properties = properties;
        this.connectionId = connectionId;
        this.stepType = stepType;
        this.stepId = stepId;
    }

    public void start() throws Exception {

        log.info("Setting Mutual SSL for connection={}",connectionId);

        setFields();

        if (SOURCE_STEP_TYPE.equalsIgnoreCase(stepType)) {
            if (certificate != null) {
                addInboundHandlerToRegistry();
            } else {
                throw new Exception("Mutual SSL: Certificate is required");
            }
        } else if (certificate != null && password != null) {
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

        String baseDirToUnix = FilenameUtils.separatorsToUnix(baseDir);
        String truststorePath = baseDirToUnix + SEP + SECURITY_PATH + SEP + OUTBOUND_TRUSTSTORE_FILE;

        //This creates an in-memory keystore from the client certificate (.p12/pfx) and an in-memory truststore
        //that combines the JDK cacerts, the outbound truststore and the CA certificates of the client certificate
        SSLConfiguration sslConfiguration = new SSLConfiguration();
        SSLContextParameters sslContextParameters = sslConfiguration.createRuntimeSSLContext(
                certificate, password, truststorePath, getKeystorePassword()
        );

        context.getRegistry().bind(connectionId, sslContextParameters);

    }

    //Inbound (HTTPS source): only clients with a certificate issued by (a CA in) the uploaded certificate are allowed
    private void addInboundHandlerToRegistry() throws Exception {

        String uri = properties.getProperty(stepType + "." + stepId + ".uri");
        if (uri == null) {
            throw new Exception("Mutual SSL: No uri found for " + stepType + " step " + stepId);
        }

        String path = getPath(uri);
        boolean matchOnUriPrefix = getOption(uri, "matchOnUriPrefix");

        SSLConfiguration sslConfiguration = new SSLConfiguration();
        List<X509Certificate> certificates = sslConfiguration.loadCertificates(certificate, password);
        if (certificates.isEmpty()) {
            throw new Exception("Mutual SSL: No certificates found for connection " + connectionId);
        }
        X509TrustManager trustManager = sslConfiguration.createTrustManager(certificates);

        MutualTlsHandler handler = MutualTlsHandler.getInstance(connectionId);
        handler.configure(path, matchOnUriPrefix, trustManager);

        context.getRegistry().bind(connectionId, Handler.class, handler);

        for (X509Certificate trustedCertificate : certificates) {
            log.info("Mutual SSL for connection {}: path {} trusts client certificate (and certificates issued by) subject={}",
                    connectionId, path, trustedCertificate.getSubjectX500Principal());
        }

    }

    // Returns the path of an uri like https://0.0.0.0:9001/path/?options
    private String getPath(String uri) {

        String path = StringUtils.substringBefore(uri, "?");

        if (path.contains("://")) {
            path = StringUtils.substringAfter(path, "://");
            path = path.contains("/") ? path.substring(path.indexOf("/")) : "/";
        }

        return path;

    }

    private boolean getOption(String uri, String option) {

        String query = StringUtils.substringAfter(uri, "?");

        for (String parameter : query.split("&")) {
            if (parameter.equalsIgnoreCase(option + "=true")) {
                return true;
            }
        }

        return false;

    }

    private String getKeystorePassword() {
        String keystorePwd = System.getenv(KEYSTORE_PWD);
        if(StringUtils.isEmpty(keystorePwd)) {
            return "supersecret";
        }

        return keystorePwd;
    }

}
