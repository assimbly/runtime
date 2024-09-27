package org.assimbly.dil.blocks.connections.broker;

import org.apache.camel.CamelContext;
import org.apache.camel.component.amqp.AMQPComponent;
import org.apache.commons.lang3.StringUtils;
import org.assimbly.util.BaseDirectory;
import org.jasypt.properties.EncryptableProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.jms.JMSException;
import java.util.Arrays;


public class AMQPConnection {

    protected Logger log = LoggerFactory.getLogger(getClass());

    private final String KEYSTORE_PWD = "KEYSTORE_PWD";

    private CamelContext context;
    private EncryptableProperties properties;
    private String componentName;
    private String connectionId;

    private final String baseDir = BaseDirectory.getInstance().getBaseDirectory();

    private String url;
    private String username;
    private String password;

    public AMQPConnection(CamelContext context, EncryptableProperties properties, String connectionId, String componentName) {
        this.context = context;
        this.properties = properties;
        this.connectionId = connectionId;
        this.componentName = componentName;
    }


    public void start(boolean sslEnabled) throws Exception {

        log.info("Setting AMQP client connection.");

        setFields();

        if (url != null) {
            setConnection(sslEnabled);
        } else {
            throw new Exception("Unknown url. Broker url is required");
        }
    }

    private void setFields(){

        url = properties.getProperty("connection." + connectionId + ".url");
        username = properties.getProperty("connection." + connectionId + ".username");
        password = properties.getProperty("connection." + connectionId + ".password");

    }



    private void setConnection(boolean sslEnabled) throws JMSException {

        AMQPComponent amqpComponent;

        if (sslEnabled) {
            url = createSSLEnabledUrl(url);
        }

        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            amqpComponent = AMQPComponent.amqpComponent(url);
        } else {
            amqpComponent = AMQPComponent.amqpComponent(url, username, password);
        }

        if (context.hasComponent(componentName) == null) {
            context.addComponent(componentName, amqpComponent);
        } else {
            context.removeComponent(componentName);
            context.addComponent(componentName, amqpComponent);
        }

    }


    private String createSSLEnabledUrl(String url) {

        String modifiedUrl = "";
        String multipleUrls;

        if (url.indexOf(',') == -1) {
            log.info("SSLEnabled Normal Url: ");
            modifiedUrl = addSSLParameterToUrl(url);
        }else{
            log.info("SSLEnabled Failover Url: ");

            if (url.indexOf('(') != -1) {
                multipleUrls = StringUtils.substringBetween(url,"(",")");
            }else{
                multipleUrls = url;
            }

            String[] failoverUrlSplitted = multipleUrls.split(",");

            Integer j = Integer.valueOf(0);
            for (Integer i = 0; i < failoverUrlSplitted.length; i++) {
                if(i.intValue() == j.intValue()){
                    modifiedUrl = addSSLParameterToUrl(failoverUrlSplitted[i]);
                }else{
                    modifiedUrl = modifiedUrl + "," + addSSLParameterToUrl(failoverUrlSplitted[i]);
                }
            }

            if (url.indexOf('(') != -1) {
                modifiedUrl = "failover:(" + modifiedUrl + ")";
            }

        }

        if(modifiedUrl.isEmpty()){
            log.info("SSLEnabled Url: " + url);
            return url;
        }else{
            log.info("SSLEnabled Url: " + modifiedUrl);
            return modifiedUrl;
        }

    }

    private String addSSLParameterToUrl(String url){

        String baseDirURI = baseDir.replace("\\", "/");

        String sslUrl = url;
        if (url.indexOf('?') == -1) {
            sslUrl = url + "?transport.verifyHost=false&transport.trustAll=true&transport.trustStoreLocation=" + baseDirURI + "/security/truststore.jks" + "&transport.trustStorePassword=" + getKeystorePassword();
        } else {
            String[] urlSplitted = url.split("/?");
            String[] optionsSplitted = urlSplitted[1].split("&");

            if (!Arrays.stream(optionsSplitted).anyMatch("transport.verifyHost"::startsWith)) {
                sslUrl = url + "&transport.verifyHost=false";
            }

            if (!Arrays.stream(optionsSplitted).anyMatch("transport.trustStoreLocation"::startsWith)) {
                sslUrl = url + "&transport.trustStoreLocation=" + baseDirURI + "/security/truststore.jks";
            }

            if (!Arrays.stream(optionsSplitted).anyMatch("transport.trustStorePassword"::startsWith)) {
                sslUrl = url + "&transport.trustStorePassword=" + getKeystorePassword();
            }

        }

        return sslUrl;

    }

    private String getKeystorePassword() {
        String keystorePwd = System.getenv(KEYSTORE_PWD);
        if(StringUtils.isEmpty(keystorePwd)) {
            return "supersecret";
        }

        return keystorePwd;
    }

}
