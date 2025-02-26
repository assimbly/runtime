package org.assimbly.dil.blocks.connections.broker;

import com.ibm.mq.jms.MQConnectionFactory;
import com.ibm.msg.client.wmq.WMQConstants;
import jakarta.jms.ConnectionFactory;
import org.apache.camel.CamelContext;
import org.apache.camel.component.jms.JmsComponent;
import org.jasypt.properties.EncryptableProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class IBMMQConnection {

    protected Logger log = LoggerFactory.getLogger(getClass());

    private final CamelContext context;
    private final EncryptableProperties properties;
    private final String componentName;
    private final String connectionId;

    private String url;
    private String username;
    private String password;
    private String queueManager;
    private String channel;
    private String channelReceiveExit;
    private String channelReceiveExitUserData;
    private String channelSendExit;
    private String channelSendExitUserData;
    private String channelSecurityExit;
    private String channelSecurityExitUserData;
    private String clientId;
    private String appName;
    private String clientReconnectTimeOutAsString;
    private String clientReconnectOptionsAsString;
    private String transportTypeAsString;
    private String pollingIntervalAsString;
    private String maxBufferSizeAsString;
    private String clientUserAuthenticationMQCSP;



    public IBMMQConnection(CamelContext context, EncryptableProperties properties, String connectionId, String componentName) {
        this.context = context;
        this.properties = properties;
        this.connectionId = connectionId;
        this.componentName = componentName;
    }


    public void start(String stepType, Object stepId) throws Exception {

        log.info("Setting up jms client connection for ActiveMQ.");

        setFields();

        if (context.hasComponent(componentName) == null) {
            if (url != null) {
                setConnection();
            }
        } else {
            log.error("ActiveMQ connection parameters are invalid.");
            throw new Exception("ActiveMQ connection parameters are invalid.\n");
        }
    }

    private void setFields(){


        //required properties
        url = properties.getProperty("connection." + connectionId + ".url");
        username = properties.getProperty("connection." + connectionId + ".username");
        password = properties.getProperty("connection." + connectionId + ".password");
        queueManager = properties.getProperty("connection." + connectionId + ".queuemanager");
        channel = properties.getProperty("connection." + connectionId + ".channel");

        //optional properties
        channelReceiveExit = properties.getProperty("connection." + connectionId + ".channelreceiveexit");
        channelReceiveExitUserData = properties.getProperty("connection." + connectionId + ".channelreceiveexituserdata");
        channelSendExit = properties.getProperty("connection." + connectionId + ".channelsendexit");
        channelSendExitUserData = properties.getProperty("connection." + connectionId + ".channelsendexituserdata");
        channelSecurityExit = properties.getProperty("connection." + connectionId + ".channelsecurityexit");
        channelSecurityExitUserData = properties.getProperty("connection." + connectionId + ".channelsecurityexituserdata");
        clientId = properties.getProperty("connection." + connectionId + ".appname");
        appName = properties.getProperty("connection." + connectionId + ".clientid");
        clientReconnectTimeOutAsString = properties.getProperty("connection." + connectionId + ".reconnecttimeout");
        clientReconnectOptionsAsString = properties.getProperty("connection." + connectionId + ".reconnectoptions");
        transportTypeAsString = properties.getProperty("connection." + connectionId + ".transporttype");
        pollingIntervalAsString = properties.getProperty("connection." + connectionId + ".pollinginterval");
        maxBufferSizeAsString = properties.getProperty("connection." + connectionId + ".maxbuffersize");
        clientUserAuthenticationMQCSP = properties.getProperty("connection." + connectionId + ".userauthenticationmqcp");

    }


    private void setConnection() throws Exception {

        log.info("Setting up IBM MQ connection factory.");

        MQConnectionFactory cf = setupIBMMQConnectionFactory();

        log.info("Setting up IBM MQ client connection.");
        if (context.hasComponent(componentName) == null) {
            JmsComponent jmsComponent = new JmsComponent();
            jmsComponent.setConnectionFactory((ConnectionFactory) cf);
            context.addComponent(componentName, jmsComponent);
        } else {
            context.removeComponent(componentName);
            JmsComponent jmsComponent = new JmsComponent();
            jmsComponent.setConnectionFactory((ConnectionFactory) cf);
            context.addComponent(componentName, jmsComponent);
        }

    }


    private MQConnectionFactory setupIBMMQConnectionFactory() throws Exception {

        MQConnectionFactory cf = new MQConnectionFactory();

        //Required parameters
        if (url == null) {
            log.error("IBMMQ connection required parameter 'url' isn't set");
            throw new Exception("IBMMQ connection parameters are invalid or missing.\n");
        }
        if (channel == null) {
            log.error("IBMMQ connection required parameter 'channel' isn't set");
            throw new Exception("IBMMQ connection parameters are invalid or missing.\n");
        }
        if (queueManager == null) {
            log.error("IBMMQ connection required parameter 'queuemanager' isn't set");
            throw new Exception("IBMMQ connection parameters are invalid or missing.\n");
        }

        cf.setConnectionNameList(url);
        cf.setChannel(channel);//communications link
        cf.setQueueManager(queueManager);//service provider

        //Optional parameters

        //public static final int 	WMQ_CM_BINDINGS 				0
        //public static final int 	WMQ_CM_CLIENT 					1
        //public static final int   WMQ_CLIENT_NONJMS_MQ            1
        //public static final int 	WMQ_CM_DIRECT_TCPIP 			2
        //public static final int 	WMQ_CM_DIRECT_HTTP 				4
        //public static final int 	WMQ_CM_BINDINGS_THEN_CLIENT 	8
        if (transportTypeAsString != null) {
            cf.setTransportType(Integer.parseInt(transportTypeAsString));
        }else{
            cf.setTransportType(WMQConstants.WMQ_CM_CLIENT);
        }

        if(clientReconnectTimeOutAsString!=null){
            cf.setClientReconnectTimeout(Integer.parseInt(clientReconnectTimeOutAsString));
        }else{
            cf.setClientReconnectTimeout(2);
        }

        if(clientReconnectTimeOutAsString!=null){
            cf.setClientReconnectOptions(Integer.parseInt(clientReconnectOptionsAsString));
        }else{
            cf.setClientReconnectOptions(0);
        }

        cf.setBooleanProperty(WMQConstants.USER_AUTHENTICATION_MQCSP, clientUserAuthenticationMQCSP == null || !clientUserAuthenticationMQCSP.equalsIgnoreCase("false"));

        cf.setBooleanProperty(WMQConstants.WMQ_MQMD_WRITE_ENABLED, true);

        if(channelReceiveExit!=null){cf.setReceiveExit(channelReceiveExit);}
        if(channelReceiveExitUserData!=null){cf.setReceiveExitInit(channelReceiveExitUserData);}
        if(channelSendExit!=null){cf.setSendExit(channelSendExit);}
        if(channelSendExitUserData!=null){cf.setSendExitInit(channelSendExitUserData);}
        if(channelSecurityExit!=null){cf.setSecurityExit(channelSecurityExit);}
        if(channelSecurityExitUserData!=null){cf.setSecurityExitInit(channelSecurityExitUserData);}
        if(appName!=null){cf.setAppName(appName);}
        if(clientId!=null){cf.setClientID(clientId);}
        if(pollingIntervalAsString!=null){cf.setPollingInterval(Integer.parseInt(pollingIntervalAsString));}
        if(maxBufferSizeAsString!=null){cf.setMaxBufferSize(Integer.parseInt(maxBufferSizeAsString));}

        if(username != null && !username.isEmpty() && password != null && !password.isEmpty()) {
            cf.createConnection(username, password);
        }else if(username != null && !username.isEmpty()) {
            cf.setStringProperty(WMQConstants.USERID, username);
            cf.createConnection();
        }else{
            cf.createConnection();
        }

        return cf;

    }

}
