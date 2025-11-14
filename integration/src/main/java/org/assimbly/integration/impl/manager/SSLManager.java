package org.assimbly.integration.impl.manager;

import org.apache.camel.CamelContext;
import org.apache.camel.spi.Registry;
import org.apache.camel.support.SimpleRegistry;
import org.apache.camel.support.jsse.SSLContextParameters;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.assimbly.dil.transpiler.ssl.SSLConfiguration;
import org.assimbly.util.BaseDirectory;
import org.assimbly.util.CertificatesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.net.ssl.SSLContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.StringReader;
import java.net.URI;
import java.net.URL;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.util.*;

public class SSLManager {

    protected static final Logger log = LoggerFactory.getLogger(SSLManager.class);

    private final String baseDir = BaseDirectory.getInstance().getBaseDirectory();

    private static final String HTTP_MUTUAL_SSL_PROP = "httpMutualSSL";
    private static final String SEP = "/";
    private static final String SECURITY_PATH = "security";
    private static final String TRUSTSTORE_FILE = "truststore.jks";
    private static final String KEYSTORE_FILE = "keystore.jks";
    private static final String KEYSTORE_PWD = "KEYSTORE_PWD";
    private static final String RESOURCE_PROP = "resource";
    private static final String AUTH_PASSWORD_PROP = "authPassword";

    public void setSSLContext(CamelContext context, SimpleRegistry registry) throws Exception {

        String baseDir2 = FilenameUtils.separatorsToUnix(baseDir);

        File securityPath = new File(baseDir + SEP + SECURITY_PATH + SEP);

        if (!securityPath.exists()) {
            boolean securityPathCreated = securityPath.mkdirs();
            if (!securityPathCreated) {
                throw new Exception("Directory: " + securityPath.getAbsolutePath() + " cannot be create to store keystore files");
            }
        }

        String keyStorePath = baseDir2 + SEP + SECURITY_PATH + SEP + KEYSTORE_FILE;
        String trustStorePath = baseDir2 + SEP + SECURITY_PATH + SEP + TRUSTSTORE_FILE;

        SSLConfiguration sslConfiguration = new SSLConfiguration();

        SSLContextParameters sslContextParameters = sslConfiguration.createSSLContextParameters(keyStorePath, getKeystorePassword(), trustStorePath, getKeystorePassword());

        SSLContextParameters sslContextParametersKeystoreOnly = sslConfiguration.createSSLContextParameters(keyStorePath, getKeystorePassword(), null, null);

        SSLContextParameters sslContextParametersTruststoreOnly = sslConfiguration.createSSLContextParameters(null, null, trustStorePath, getKeystorePassword());

        registry.bind("default", sslContextParameters);
        registry.bind("sslContext", sslContextParameters); // TODO - rename to sslContextParameters
        registry.bind("sslContextObj", sslContextParameters.createSSLContext(context)); // TODO - rename to sslContext
        registry.bind("keystore", sslContextParametersKeystoreOnly);
        registry.bind("truststore", sslContextParametersTruststoreOnly);

        try {
            SSLContext sslContext = sslContextParameters.createSSLContext(context);
            sslContext.createSSLEngine();
        } catch (Exception e) {
            log.error("Can't set SSL context for certificate keystore. TLS/SSL certificates are not available. Reason: {}", e.getMessage());
        }

        String[] sslComponents = {"ftps", "https", "imaps", "jetty", "netty", "smtps"};

        sslConfiguration.setUseGlobalSslContextParameters(context, sslComponents);

    }

    // add certificate from url on the keystore
    public void addCertificateFromUrl(String url, String authPassword) {
        try {
            byte[] fileContent;

            URL urlObject = URI.create(url).toURL();
            try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(IOUtils.toByteArray(urlObject))) {
                fileContent = IOUtils.toByteArray(byteArrayInputStream);
            }
            String encodedResourceContent = Base64.getEncoder().encodeToString(fileContent);

            CertificatesUtil util = new CertificatesUtil();
            String keystorePath = baseDir + SEP + SECURITY_PATH + SEP + KEYSTORE_FILE;
            util.importP12Certificate(keystorePath, getKeystorePassword(), encodedResourceContent, authPassword);

        } catch (Exception e) {
            log.error("Error to add certificate", e);
        }
    }

    // get mutual ssl info from props
    public Map<String, String> getMutualSSLInfoFromProps(TreeMap<String, String> props) {
        for (Map.Entry<String, String> entry : props.entrySet()) {
            String xml = entry.getValue();
            if (!xml.startsWith("<")) {
                // skip prop if it's not an xml
                continue;
            }
            return getMutualSSLInfoFromXml(xml);
        }
        return Collections.emptyMap();
    }

    // get mutual ssl info from xml
    private HashMap<String, String> getMutualSSLInfoFromXml(String xml) {
        HashMap<String, String> map = new HashMap<>();

        String httpMutualSSL = getPropertyValue(xml, HTTP_MUTUAL_SSL_PROP);
        if (httpMutualSSL != null && httpMutualSSL.equals("true")) {
            String authPassword = getPropertyValue(xml, AUTH_PASSWORD_PROP);
            String resource = getPropertyValue(xml, RESOURCE_PROP);

            map.put(AUTH_PASSWORD_PROP, authPassword);
            map.put(RESOURCE_PROP, resource);

        }

        return map;
    }

    // get property value by property name
    private String getPropertyValue(String xml, String propName) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        XPathFactory xpathFactory = XPathFactory.newInstance();
        XPath xpath = xpathFactory.newXPath();
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xml)));
            String expression = String.format("//setProperty[@name='%s']/constant/text()", propName);
            return xpath.evaluate(expression, doc);
        } catch (Exception e) {
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

        Certificate[] certificates = new Certificate[0];

        try {
            CertificatesUtil util = new CertificatesUtil();
            certificates = util.downloadCertificates(url);
        } catch (Exception e) {
            log.error("Start certificates for url {} failed.", url, e);
        }
        return certificates;
    }

    public Certificate getCertificateFromKeystore(String keystoreName, String keystorePassword, String certificateName) {
        String keystorePath = baseDir + SEP + SECURITY_PATH + SEP + keystoreName;
        CertificatesUtil util = new CertificatesUtil();
        return util.getCertificate(keystorePath, keystorePassword, certificateName);
    }

    public void setCertificatesInKeystore(String keystoreName, String keystorePassword, String url) {

        try {
            CertificatesUtil util = new CertificatesUtil();
            Certificate[] certificates = util.downloadCertificates(url);
            String keystorePath = baseDir + SEP + SECURITY_PATH + SEP + keystoreName;
            util.importCertificates(keystorePath, keystorePassword, certificates);
        } catch (Exception e) {
            log.error("Set certificates for url {} failed.", url, e);
        }
    }

    public String importCertificateInKeystore(String keystoreName, String keystorePassword, String certificateName, Certificate certificate) {

        CertificatesUtil util = new CertificatesUtil();

        String keystorePath = baseDir + SEP + SECURITY_PATH + SEP + keystoreName;

        File file = new File(keystorePath);

        String result;

        if (file.exists()) {
            result = util.importCertificate(keystorePath, keystorePassword, certificateName, certificate);
        } else {
            result = "Keystore doesn't exist";
        }

        return result;

    }


    public Map<String, Certificate> importCertificatesInKeystore(String keystoreName, String keystorePassword, Certificate[] certificates) throws Exception {

        CertificatesUtil util = new CertificatesUtil();

        String keystorePath = baseDir + SEP + SECURITY_PATH + SEP + keystoreName;

        File file = new File(keystorePath);

        if (file.exists()) {
            return util.importCertificates(keystorePath, keystorePassword, certificates);
        } else {
            throw new KeyStoreException("Keystore " + keystoreName + "doesn't exist");
        }

    }

    public Map<String, Certificate> importP12CertificateInKeystore(String keystoreName, String keystorePassword, String p12Certificate, String p12Password) throws Exception {

        CertificatesUtil util = new CertificatesUtil();

        String keystorePath = baseDir + SEP + SECURITY_PATH + SEP + keystoreName;
        return util.importP12Certificate(keystorePath, keystorePassword, p12Certificate, p12Password);

    }

    public void deleteCertificateInKeystore(String keystoreName, String keystorePassword) {

        String keystorePath = baseDir + SEP + SECURITY_PATH + SEP + keystoreName;

        CertificatesUtil util = new CertificatesUtil();
        util.deleteCertificate(keystorePath, keystorePassword);
    }

    public void setMutualSsl(String keystoreResource, String keystorePassword, String contextId, Registry registry) throws Exception {

        String baseDir2 = FilenameUtils.separatorsToUnix(baseDir);
        String truststorePath = baseDir2 + SEP + SECURITY_PATH + SEP + TRUSTSTORE_FILE;

        SSLConfiguration sslConfiguration = new SSLConfiguration();
        SSLContextParameters sslContextParameters = sslConfiguration.createRuntimeSSLContext(
                keystoreResource, keystorePassword, truststorePath, getKeystorePassword()
        );

        registry.bind(contextId, sslContextParameters);
    }

}
