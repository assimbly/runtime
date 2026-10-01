package org.assimbly.integration.impl.manager;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.util.*;

import javax.net.ssl.SSLContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.apache.camel.CamelContext;
import org.apache.camel.support.SimpleRegistry;
import org.apache.camel.support.jsse.SSLContextParameters;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.assimbly.dil.transpiler.ssl.SSLConfiguration;
import org.assimbly.util.BaseDirectory;
import org.assimbly.util.CertificatesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

public class SSLManager {

    protected static final Logger log = LoggerFactory.getLogger(SSLManager.class);

    private final String baseDir = BaseDirectory.getInstance().getBaseDirectory();

    private static final String HTTP_MUTUAL_SSL_PROP = "httpMutualSSL";
    private static final String SEP = "/";
    private static final String SECURITY_PATH = "security";

    private static final String SERVER_IDENTITY_FILE = "server-identity.p12";
    private static final String OUTBOUND_TRUSTSTORE_FILE = "outbound-truststore.p12";

    private static final String KEYSTORE_PWD = "KEYSTORE_PWD";
    private static final String RESOURCE_PROP = "resource";
    private static final String AUTH_PASSWORD_PROP = "authPassword";

    private static final String[] SSL_COMPONENTS = { "ftps", "https", "imaps", "jetty", "netty", "smtps" };


    public void setSSLContext(CamelContext context, SimpleRegistry registry) throws Exception {

        SSLConfiguration sslConfiguration = new SSLConfiguration();

        Path securityPath = prepareSecurityDirectory();

        //generic SSL Context
        registerSSLContext(context, registry, sslConfiguration, securityPath);

        //Register the sslConfiguration globally
        sslConfiguration.setUseGlobalSslContextParameters(context, SSL_COMPONENTS);

    }

    public void setMutualSslContext(String contextId, SimpleRegistry registry, String keystoreResource, String keystorePassword) throws Exception {

        SSLConfiguration sslConfiguration = new SSLConfiguration();

        Path securityPath = prepareSecurityDirectory();

        //specific Mutal SSL Context
        registerMutualSSLContext(contextId, registry, sslConfiguration, securityPath, keystoreResource, keystorePassword);

    }

    private void registerSSLContext(CamelContext context, SimpleRegistry registry, SSLConfiguration sslConfiguration, Path securityPath) throws Exception {

        String serverIdentityPath = securityPath.resolve(SERVER_IDENTITY_FILE).toString();

        String outboundTrustPath = securityPath.resolve(OUTBOUND_TRUSTSTORE_FILE).toString();

        SSLContextParameters sslContextParameters =
                sslConfiguration.createSSLContextParameters(
                        serverIdentityPath,
                        getKeystorePassword(),
                        outboundTrustPath,
                        getKeystorePassword()
                );

        SSLContext sslContext = sslContextParameters.createSSLContext(context);

        registry.bind("sslContext", sslContextParameters);
        registry.bind("sslContextObj", sslContext);

        try {
            sslContext.createSSLEngine();
        } catch (Exception e) {
            log.error(
                    "Can't set SSL context for certificate keystore. " +
                            "TLS/SSL certificates are not available. Reason: {}",
                    e.getMessage()
            );
        }
    }

    private void registerMutualSSLContext(String contextId, SimpleRegistry registry, SSLConfiguration sslConfiguration, Path securityPath, String keystoreResource, String keystorePassword) throws Exception {

        String outboundTrustPath = securityPath.resolve(OUTBOUND_TRUSTSTORE_FILE).toString();

        SSLContextParameters sslContextParameters = sslConfiguration.createRuntimeSSLContext(
                keystoreResource,
                keystorePassword,
                outboundTrustPath,
                getKeystorePassword()
        );

        registry.bind(contextId, sslContextParameters);


    }

    // add certificate from url on the keystore
    public void addCertificateFromUrl(String url, String authPassword) {
        try {
            URL urlObject = URI.create(url).toURL();
            byte[] fileContent = IOUtils.toByteArray(urlObject);
            String encodedResourceContent = Base64.getEncoder().encodeToString(fileContent);

            CertificatesUtil util = new CertificatesUtil();
            String keystorePath = baseDir + SEP + SECURITY_PATH + SEP + SERVER_IDENTITY_FILE;
            util.importP12Certificate(keystorePath, getKeystorePassword(), encodedResourceContent, authPassword);

        } catch (Exception e) {
            log.error("Error adding certificate from URL: {}", url, e);
        }
    }

    // get mutual ssl info from props
    public Map<String, String> getMutualSSLInfoFromProps(TreeMap<String, String> props) {
        return props.values().stream()
                .filter(xml -> xml.startsWith("<"))
                .findFirst()
                .map(this::getMutualSSLInfoFromXml)
                .orElseGet(HashMap::new);
    }

    // get mutual ssl info from xml
    private HashMap<String, String> getMutualSSLInfoFromXml(String xml) {
        HashMap<String, String> map = new HashMap<>();

        String httpMutualSSL = getPropertyValue(xml, HTTP_MUTUAL_SSL_PROP);
        if ("true".equalsIgnoreCase(httpMutualSSL)) {
            String authPassword = getPropertyValue(xml, AUTH_PASSWORD_PROP);
            String resource = getPropertyValue(xml, RESOURCE_PROP);

            map.put(AUTH_PASSWORD_PROP, authPassword);
            map.put(RESOURCE_PROP, resource);
        }

        return map;
    }

    // get property value by property name
    private String getPropertyValue(String xml, String propName) {
        try {
            DocumentBuilderFactory xmlFactory = DocumentBuilderFactory.newInstance();
            xmlFactory.setNamespaceAware(true);

            // Disallow DTDs and External Entities for XXE prevention
            xmlFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            xmlFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            xmlFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

            DocumentBuilder builder = xmlFactory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xml)));

            XPath xpath = XPathFactory.newInstance().newXPath();
            String expression = "//setProperty[@name='%s']/constant/text()".formatted(propName);
            return xpath.evaluate(expression, doc);
        } catch (Exception e) {
            log.debug("Could not parse property '{}' from XML: {}", propName, e.getMessage());
            return null;
        }
    }

    private String getKeystorePassword() {
        String keystorePwd = System.getenv(KEYSTORE_PWD);
        if (StringUtils.isEmpty(keystorePwd)) {
            return "supersecret";
        }
        return keystorePwd;
    }

    public Certificate[] getCertificates(String url) {
        try {
            CertificatesUtil util = new CertificatesUtil();
            return util.downloadCertificates(url);
        } catch (Exception e) {
            log.error("Start certificates for url {} failed.", url, e);
            return new Certificate[0];
        }
    }

    public Certificate getCertificateFromKeystore(String keystoreName, String keystorePassword, String certificateName) {
        String keystorePath = baseDir + SEP + SECURITY_PATH + SEP + keystoreName;
        CertificatesUtil util = new CertificatesUtil();
        return util.getCertificate(keystorePath, keystorePassword, certificateName);
    }

    public String importCertificateInKeystore(String keystoreName, String keystorePassword, String certificateName, Certificate certificate) {
        CertificatesUtil util = new CertificatesUtil();
        String keystorePath = baseDir + SEP + SECURITY_PATH + SEP + keystoreName;
        File file = new File(keystorePath);

        if (file.exists()) {
            return util.importCertificate(keystorePath, keystorePassword, certificateName, certificate);
        } else {
            return "Keystore doesn't exist";
        }
    }

    public Map<String, Certificate> downloadCertificatesInKeystore(String keystoreName, String keystorePassword, Certificate[] certificates) throws Exception {
        CertificatesUtil util = new CertificatesUtil();
        String keystorePath = baseDir + SEP + SECURITY_PATH + SEP + keystoreName;
        File file = new File(keystorePath);

        if (file.exists()) {
            return util.storeCertificates(keystorePath, keystorePassword, certificates);
        } else {
            throw new KeyStoreException("Keystore " + keystoreName + " doesn't exist");
        }
    }

    public Map<String, Certificate> importP12CertificateInKeystore(String keystoreName, String keystorePassword, String p12Certificate, String p12Password) throws Exception {
        CertificatesUtil util = new CertificatesUtil();
        String keystorePath = baseDir + SEP + SECURITY_PATH + SEP + keystoreName;
        return util.importP12Certificate(keystorePath, keystorePassword, p12Certificate, p12Password);
    }

    public boolean deleteCertificateInKeystore(String keystoreName, String keystorePassword, String certificateName) throws Exception {
        String keystorePath = baseDir + SEP + SECURITY_PATH + SEP + keystoreName;
        CertificatesUtil util = new CertificatesUtil();
        return util.deleteCertificate(keystorePath, keystorePassword, certificateName);
    }

    private Path prepareSecurityDirectory() throws IOException {
        Path securityPath = Paths.get(baseDir, SECURITY_PATH);
        Files.createDirectories(securityPath);
        return securityPath;
    }

}