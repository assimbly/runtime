package org.assimbly.dil.blocks.connections.broker;

import org.apache.camel.CamelContext;
import org.apache.camel.component.amqp.AMQPComponent;
import org.apache.commons.lang3.StringUtils;
import org.assimbly.util.BaseDirectory;
import org.jasypt.properties.EncryptableProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;


public class AMQPConnection {

    protected Logger log = LoggerFactory.getLogger(getClass());

    private static final String KEYSTORE_PWD = "KEYSTORE_PWD";

    private final CamelContext context;
    private final EncryptableProperties properties;
    private final String componentName;
    private final String connectionId;

    private final String baseDir = BaseDirectory.getInstance().getBaseDirectory();

    private String url;
    private String username;
    private String password;

    private boolean sslEnabled = false;

    public AMQPConnection(CamelContext context, EncryptableProperties properties, String connectionId, String componentName) {
        this.context = context;
        this.properties = properties;
        this.connectionId = connectionId;
        this.componentName = componentName;
    }


    public void start() throws Exception {

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

        if(componentName.equals("amqps")){
            sslEnabled = true;
        }

    }



    private void setConnection(boolean sslEnabled) {

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

        StringBuilder modifiedUrl = new StringBuilder();
        String multipleUrls;

        if (url.indexOf(',') == -1) {
            log.info("SSLEnabled Normal Url: ");
            modifiedUrl = new StringBuilder(addSSLParameterToUrl(url));
        }else{
            log.info("SSLEnabled Failover Url: ");

            if (url.indexOf('(') != -1) {
                multipleUrls = StringUtils.substringBetween(url,"(",")");
            }else{
                multipleUrls = url;
            }

            String[] failoverUrlSplitted = multipleUrls.split(",");

            for (int i = 0; i < failoverUrlSplitted.length; i++) {
                if(i == 0){
                    modifiedUrl = new StringBuilder(addSSLParameterToUrl(failoverUrlSplitted[i]));
                }else{
                    modifiedUrl.append(",").append(addSSLParameterToUrl(failoverUrlSplitted[i]));
                }
            }

            if (url.indexOf('(') != -1) {
                modifiedUrl = new StringBuilder("failover:(" + modifiedUrl + ")");
            }

        }

        if(modifiedUrl.length() == 0){
            log.info("SSLEnabled Url: " + url);
            return url;
        }else{
            log.info("SSLEnabled Url: " + modifiedUrl);
            return modifiedUrl.toString();
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

            if (!Arrays.stream(optionsSplitted).noneMatch("transport.verifyHost"::startsWith)) {
                sslUrl = url + "&transport.verifyHost=false";
            }

            if (!Arrays.stream(optionsSplitted).noneMatch("transport.trustStoreLocation"::startsWith)) {
                sslUrl = url + "&transport.trustStoreLocation=" + baseDirURI + "/security/truststore.jks";
            }

            if (!Arrays.stream(optionsSplitted).noneMatch("transport.trustStorePassword"::startsWith)) {
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
