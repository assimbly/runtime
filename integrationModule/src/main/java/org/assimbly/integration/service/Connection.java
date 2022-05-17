package org.assimbly.integration.service;

import com.ibm.mq.jms.MQConnectionFactory;
import com.ibm.msg.client.wmq.WMQConstants;
import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.jms.pool.PooledConnectionFactory;
import org.apache.camel.component.activemq.ActiveMQComponent;
import org.apache.camel.component.activemq.ActiveMQConfiguration;
import org.apache.camel.CamelContext;
import org.apache.camel.component.amqp.AMQPComponent;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.component.properties.PropertiesComponent;
import org.apache.camel.component.sjms.SjmsComponent;
import org.apache.camel.spi.Registry;
import org.apache.commons.lang3.StringUtils;
import org.assimbly.util.BaseDirectory;
import org.jasypt.properties.EncryptableProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import progress.message.jclient.ConnectionFactory;

import java.util.*;

public class Connection {

	protected Logger log = LoggerFactory.getLogger(getClass());
	
	private String uri;
	private String connectionId;
	private String flowId;
	private TreeMap<String, String> properties;
	private String key;
	private CamelContext context;
	private String connectId;
	private boolean faultTolerant;
	private String endpointType;
    private Object endpointId;
	private String serviceId;
	private ActiveMQConnectionFactory activeMQConnectionFactory;
	private SjmsComponent sjmsComponent;

	private final String baseDir = BaseDirectory.getInstance().getBaseDirectory();

	public Connection(CamelContext context, TreeMap<String, String> properties, String key) {
		this.context = context;
		this.properties = properties;
		this.key = key;
	}

	public TreeMap<String, String> start() throws Exception{

        serviceId = properties.get(key);
        endpointType = key.split("\\.")[0]; 
        endpointId = key.split("\\.")[1]; 

        if(key.startsWith("route")){
            
            String route = properties.get("route." + endpointId + ".route");
            String options[] = {"activemq", "amazonmq", "sonicmq", "sjms", "amqps", "amqp", "ibmmq", "sql"};

            int i;
            for (i = 0; i < options.length; i++) {
                if (route!= null && route.contains(options[i])) {
                    uri = options[i];
                }
            }
		}else{
            uri = properties.get(endpointType + "." + endpointId + ".uri");
        }
        
        startConnection(uri, endpointType);        

		return properties;

	}

	//unused (future purposes)
	public TreeMap<String, String> stop() throws Exception{
		if(properties.get("from.service.id")!=null){
			uri = properties.get("from.uri");
			//stopConnection(uri,"from");
		}
		if(properties.get("to.service.id")!=null){
			String toUris = properties.get("to.uri");
			for(@SuppressWarnings("unused") String toUri : toUris.split(",")) {
				//stopConnection(toUri,"to");
			}
		}
		if(properties.get("response.service.id")!=null){
			uri = properties.get("response.uri");
			//stopConnection(toUri,"to");
		}
		if(properties.get("error.service.id")!=null){
			uri = properties.get("error.uri");
			//stopConnection(uri, "error");
		}

		return properties;

	}


	private void startConnection(String uri, String type) throws Exception{

        connectionId = properties.get(type + "." + endpointId + ".service.id");

        flowId = properties.get("id");

		if(uri!=null){

			if(connectionId!=null) {

				String[] uriSplitted = uri.split(":", 2);
				String component = uriSplitted[0];

                String options[] = {"activemq", "amazonmq", "sonicmq", "sjms", "amqps", "amqp", "ibmmq", "sql"};
				int i;
				for (i = 0; i < options.length; i++) {
					if (component != null && component.contains(options[i])) {
						break;
					}
				}

				switch (i) {
					case 0:
						setupActiveMQConnection(properties, "activemq");
						break;
					case 1:
						setupActiveMQConnection(properties, "amazonmq");
						break;
					case 2:
						connectId = type + connectionId + new Random().nextInt(1000000);
						setupSonicMQConnection(properties, type, connectId);
						uri = uri.replace("sonicmq:", "sonicmq." + flowId + connectId + ":");
						properties.put(type + "." + endpointId + ".uri", uri);						
						break;
					case 3:
						setupSJMSConnection(properties, "sjms", type);
						break;
					case 4:
                        setupAMQPConnection(properties, "amqps", true);
						break;
					case 5:
                        setupAMQPConnection(properties, "amqp", false);
						break;
					case 6:
						setupIBMMQConnection(properties, "ibmmq", type);
						break;
					case 7:
						setupJDBCConnection(properties, type);
						break;
					default:
						log.error("Connection parameters for component " + component + " are not implemented");
						throw new Exception("Connection parameters for component " + component + " are not implemented");
				}
			}
		}
	}


	@SuppressWarnings("unused")
	private void stopConnection(String uri, String type) throws Exception{

		if(type.equals("error")) {
			connectionId = properties.get(type + ".service.id");
		}else {
			connectionId = properties.get("to." + endpointId + ".service.id");
		}

		flowId = properties.get("id");

		if(uri!=null && connectionId!=null){

			String[] uriSplitted = uri.split(":",2);
			String component = uriSplitted[0];
			String endpoint = uriSplitted[1];

			String options[] = {"activemq", "sonicmq", "sjms","jdbc"};
			int i;
			for (i = 0; i < options.length; i++) {
				if (component != null && component.contains(options[i])) {
					break;
				}
			}

			switch (i) {
				case 0:
					//removeActiveMQConnection(properties, type);
					break;
				case 1:
					//String connectId = type + connectionId + flowId;
					//removeSonicMQConnection(properties, type, connectId);
					//properties.remove(type + ".uri", "sonicmq." + connectionId + flowId + ":" + endpoint);
					break;
				case 2:
					//removeJDBCConnection(properties, type);
					break;
				default:
					log.error("Connection parameters for component " + component + " are not implemented");
					throw new Exception("Connection parameters for component " + component + " are not implemented");
			}
		}
	}



	private void setupActiveMQConnection(TreeMap<String, String> properties, String componentName) throws Exception{

        log.info("Setting up jms client connection for ActiveMQ.");
        EncryptableProperties decryptedProperties = decryptProperties(properties);
        String url = decryptedProperties.getProperty("service." + serviceId + ".url");
        String username = decryptedProperties.getProperty("service." + serviceId + ".username");
        String password = decryptedProperties.getProperty("service." + serviceId + ".password");//properties.get("service." + serviceId + ".password");

        String conType = decryptedProperties.getProperty("service." + serviceId + ".conType");
        String maxConnections = decryptedProperties.getProperty("service." + serviceId + "service.maxConnections");
        String concurentConsumers = decryptedProperties.getProperty("service." + serviceId + "service.concurentConsumers");

        if (conType == null) {
            log.info("No connection type specified. Setting up basic connection for activemq.");
            conType = "basic";
        } else if (!conType.equals("basic") && !conType.equals("pooled")) {
            log.info("Invalid connection type specified. Setting up basic connection for activemq.");
            conType = "basic";
        } else if (conType.equals("pooled")) {

            if (maxConnections == null) {
                maxConnections = "10";
            }
            if (concurentConsumers == null) {
                concurentConsumers = "10";
            }

        }

        if (context.hasComponent(componentName) == null) {
            if (url != null) {

                if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
                    activeMQConnectionFactory = new ActiveMQConnectionFactory(url);
                } else {
                    activeMQConnectionFactory = new ActiveMQConnectionFactory(username, password, url);
                }


                if (conType.equals("basic")) {
                    ActiveMQConnection connection = (ActiveMQConnection) activeMQConnectionFactory.createConnection();
                    connection.start();
                    context.addComponent(componentName, JmsComponent.jmsComponentAutoAcknowledge(activeMQConnectionFactory));
                    log.info("Started basic connection for ActiveMQ.");
                } else {
                    try {
                        PooledConnectionFactory pooledConnectionFactory = new PooledConnectionFactory();
                        pooledConnectionFactory.setConnectionFactory(activeMQConnectionFactory);
                        pooledConnectionFactory.setMaxConnections(Integer.parseInt(maxConnections));

                        ActiveMQConfiguration configuration = new ActiveMQConfiguration();
                        configuration.setConnectionFactory(pooledConnectionFactory);
                        configuration.setConcurrentConsumers(Integer.parseInt(concurentConsumers));
                        configuration.setUsePooledConnection(true);

                        ActiveMQComponent component = new ActiveMQComponent(configuration);
                        context.addComponent(componentName, component);
                        log.info("Started pooled connection for ActiveMQ.");
                        log.info("Maximum connections: " + maxConnections + " - concurentConsumers: " + concurentConsumers);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }


				/*PooledConnection connection = (PooledConnection) pooledFactory.createConnection();
				ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(url);

				policy.setMaximumRedeliveries(3);
				policy.setInitialRedeliveryDelay(5);
				session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
				consumer = session.createConsumer(queue);
				consumer.setMessageListener(this);
    			connection.start();*/


            } else {
                log.error("ActiveMQ connection parameters are invalid.");
                throw new Exception("ActiveMQ connection parameters are invalid.\n");
            }
        }
    }


    private void setupSJMSConnection(TreeMap<String, String> properties, String componentName, String direction) throws Exception {

        if (direction.equals("to") || direction.equals("from")) {
            direction = direction + "." + endpointId;
        }
        EncryptableProperties decryptedProperties = decryptProperties(properties);
        String url = decryptedProperties.getProperty("service." + serviceId + ".url");
        String jmsProvider = decryptedProperties.getProperty("service." + serviceId + ".jmsprovider");
        String username = decryptedProperties.getProperty("service." + serviceId + ".username");
        String password = decryptedProperties.getProperty("service." + serviceId + ".password");

        log.info("Setting up sjms client connection.");

        if (url != null) {

            if(jmsProvider.equalsIgnoreCase("AMQ") || jmsProvider.equalsIgnoreCase("ActiveMQ Artemis")){

                org.apache.activemq.artemis.jms.client.ActiveMQJMSConnectionFactory cf = null;

                if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
                    cf = new org.apache.activemq.artemis.jms.client.ActiveMQJMSConnectionFactory(url);
                    cf.setConnectionTTL(-1);
                    cf.setReconnectAttempts(-1);
                    cf.setRetryInterval(1000);
                    cf.setRetryIntervalMultiplier(2.0);
                    cf.setMaxRetryInterval(3600000);
                } else {
                    cf = new org.apache.activemq.artemis.jms.client.ActiveMQJMSConnectionFactory(url, username, password);
                    cf.setConnectionTTL(-1);
                    cf.setReconnectAttempts(-1);
                    cf.setRetryInterval(1000);
                    cf.setRetryIntervalMultiplier(2.0);
                    cf.setMaxRetryInterval(3600000);
                }

                if (context.hasComponent(componentName) == null) {
                    sjmsComponent = new SjmsComponent();
                    sjmsComponent.setConnectionFactory(cf);
                    context.addComponent(componentName, sjmsComponent);
                } else {
                    context.removeComponent(componentName);
                    sjmsComponent = new SjmsComponent();
                    sjmsComponent.setConnectionFactory(cf);
                    context.addComponent(componentName, sjmsComponent);
                }


            }else if (jmsProvider.equalsIgnoreCase("ActiveMQ Classic")){
                ActiveMQConnectionFactory cf = null;

                if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
                    cf = new ActiveMQConnectionFactory(url);
                } else {
                    cf = new ActiveMQConnectionFactory(username, password, url);
                }

                if (context.hasComponent(componentName) == null) {
                    sjmsComponent = new SjmsComponent();
                    sjmsComponent.setConnectionFactory(cf);
                    context.addComponent(componentName, sjmsComponent);
                } else {
                    context.removeComponent(componentName);
                    sjmsComponent = new SjmsComponent();
                    sjmsComponent.setConnectionFactory(cf);
                    context.addComponent(componentName, sjmsComponent);
                }

            }else{
                throw new Exception("Unknown jms provider (valid are ActiveMQ Classic, AcitveMQ Artemis, AMQ).\n");
            }
        }
    }

    private void setupAMQPConnection(TreeMap<String, String> properties, String componentName, boolean sslEnabled) throws Exception {

        EncryptableProperties decryptedProperties = decryptProperties(properties);
        String url = decryptedProperties.getProperty("service." + serviceId + ".url");
        String username = decryptedProperties.getProperty("service." + serviceId + ".username");
        String password = decryptedProperties.getProperty("service." + serviceId + ".password");

        log.info("Setting AMQP client connection.");
        if (url != null) {

            AMQPComponent amqpComponent = null;

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

        } else {
            throw new Exception("url parameter is invalid or missing.\n");
        }

    }

    private void setupSonicMQConnection(TreeMap<String, String> properties, String direction, String connectId) throws Exception {

        EncryptableProperties decryptedProperties = decryptProperties(properties);
        String flowId = decryptedProperties.getProperty("id");
        String componentName = "sonicmq." + flowId + connectId;
        String url = decryptedProperties.getProperty("service." + serviceId + ".url");
        String username = decryptedProperties.getProperty("service." + serviceId + ".username");
        String password = decryptedProperties.getProperty("service." + serviceId + ".password");

        if (url != null || username != null || password != null) {

            if (decryptedProperties.getProperty("service." + serviceId + ".faultTolerant") != null) {
                try {
                    Boolean.parseBoolean(decryptedProperties.getProperty("service." + serviceId + ".faultTolerant"));
                } catch (Exception e) {
                    faultTolerant = true;
                }
            } else {
                faultTolerant = true;
            }


            if (context.hasComponent(componentName) == null) {

                ConnectionFactory connection = new ConnectionFactory(url, username, password);
                connection.setConnectID("Assimbly/Gateway/" + connectionId + "/Flow/" + flowId + "/" + connectId);
                connection.setPrefetchCount(10);
                connection.setReconnectInterval(60);
                connection.setFaultTolerant(faultTolerant);
                connection.setFaultTolerantReconnectTimeout(3600);
                connection.setInitialConnectTimeout(15);

                log.info("Connecting to SonicMQ broker (connection time is set to 15 seconds)");

                SjmsComponent jms = new SjmsComponent();
                jms.setConnectionFactory(connection);
                //jms.setConnectionClientId("Assimbly/Gateway/" + connectionId + "/Flow/" + flowId + "/" + connectId);
                jms.setCamelContext(context);
                jms.start();

                context.addComponent(componentName, jms);
            }


        } else {
            log.error("SonicMQ connection parameters are invalid or missing");
            if (url == null) {
                log.error("SonicMQ connection required parameter 'url' isn't set");
            }
            if (username == null) {
                log.error("SonicMQ connection required parameter 'username' isn't set");
            }
            if (password == null) {
                log.error("SonicMQ connection required parameter 'password' isn't set");
            }
            throw new Exception("SonicMQ connection parameters are invalid or missing.\n");
        }

    }

    @SuppressWarnings("unused")
    private void removeSonicMQConnection(TreeMap<String, String> properties, String direction, String connectId) throws Exception {

        EncryptableProperties decryptedProperties = decryptProperties(properties);
        String componentNamePrefix = "sonicmq." + connectId;
        String url = decryptedProperties.getProperty("service. " + serviceId + " .url");
        String username = decryptedProperties.getProperty("service." + serviceId + ".username");
        String password = decryptedProperties.getProperty("service." + serviceId + ".password");

        Set<String> componentNames = context.getComponentNames();

        for (String componentName : componentNames) {

            if (componentName.startsWith(componentNamePrefix)) {
                if (url != null || username != null || password != null) {

                    ConnectionFactory connection = new ConnectionFactory(url, username, password);
                    try (SjmsComponent jms = new SjmsComponent()) {
                        jms.setConnectionFactory(connection);
                        jms.setCamelContext(context);

                        jms.stop();
                    }

                    context.removeComponent(componentName);

                } else {
                    log.error("SonicMQ connection parameters are invalid or missing");
                    if (url == null) {
                        log.error("SonicMQ connection required parameter 'url' isn't set");
                    }
                    if (username == null) {
                        log.error("SonicMQ connection required parameter 'username' isn't set");
                    }
                    if (password == null) {
                        log.error("SonicMQ connection required parameter 'password' isn't set");
                    }
                    throw new Exception("SonicMQ connection parameters are invalid or missing.\n");
                }
            }
        }
    }


    private void setupIBMMQConnection(TreeMap<String, String> properties, String componentName, String direction) throws Exception {

        if (direction.equals("to") || direction.equals("from")) {
            direction = direction + "." + endpointId;
        }

        log.info("Setting up IBM MQ connection factory.");

        MQConnectionFactory cf = setupIBMMQConnectionFactory(properties);

        log.info("Setting up IBM MQ client connection.");
        if (context.hasComponent(componentName) == null) {
            JmsComponent jmsComponent = new JmsComponent();
            jmsComponent.setConnectionFactory(cf);
            context.addComponent(componentName, jmsComponent);
        } else {
            context.removeComponent(componentName);
            JmsComponent jmsComponent = new JmsComponent();
            jmsComponent.setConnectionFactory(cf);
            context.addComponent(componentName, jmsComponent);
        }

    }

	private MQConnectionFactory setupIBMMQConnectionFactory(TreeMap<String, String> properties) throws Exception {

        EncryptableProperties decryptedProperties = decryptProperties(properties);
        //required properties
        String url = decryptedProperties.getProperty("service." + serviceId + ".url");
        String username = decryptedProperties.getProperty("service." + serviceId + ".username");
        String password = decryptedProperties.getProperty("service." + serviceId + ".password");
        String queueManager = decryptedProperties.getProperty("service." + serviceId + ".queuemanager");
        String channel = decryptedProperties.getProperty("service." + serviceId + ".channel");

        //optional properties
        String channelReceiveExit = decryptedProperties.getProperty("service." + serviceId + ".channelreceiveexit");
        String channelReceiveExitUserData = decryptedProperties.getProperty("service." + serviceId + ".channelreceiveexituserdata");
        String channelSendExit = decryptedProperties.getProperty("service." + serviceId + ".channelsendexit");
        String channelSendExitUserData = decryptedProperties.getProperty("service." + serviceId + ".channelsendexituserdata");
        String channelSecurityExit = decryptedProperties.getProperty("service." + serviceId + ".channelsecurityexit");
        String channelSecurityExitUserData = decryptedProperties.getProperty("service." + serviceId + ".channelsecurityexituserdata");
        String clientId = decryptedProperties.getProperty("service." + serviceId + ".appname");
        String appName = decryptedProperties.getProperty("service." + serviceId + ".clientid");
        String clientReconnectTimeOutAsString = decryptedProperties.getProperty("service." + serviceId + ".reconnecttimeout");
        String clientReconnectOptionsAsString = decryptedProperties.getProperty("service." + serviceId + ".reconnectoptions");
        String transportTypeAsString = decryptedProperties.getProperty("service." + serviceId + ".transporttype");
        String pollingIntervalAsString = decryptedProperties.getProperty("service." + serviceId + ".pollinginterval");
        String maxBufferSizeAsString = decryptedProperties.getProperty("service." + serviceId + ".maxbuffersize");
        String clientUserAuthenticationMQCSP = decryptedProperties.getProperty("service." + serviceId + ".userauthenticationmqcp");

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

        if(clientUserAuthenticationMQCSP!=null && clientUserAuthenticationMQCSP.equalsIgnoreCase("false")){
            cf.setBooleanProperty(WMQConstants.USER_AUTHENTICATION_MQCSP, false);
        }else{
            cf.setBooleanProperty(WMQConstants.USER_AUTHENTICATION_MQCSP, true);
        }

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

    private void setupJDBCConnection(TreeMap<String, String> properties, String direction) throws Exception {

        EncryptableProperties decryptedProperties = decryptProperties(properties);

		if(direction.equals("error")) {
			connectionId = decryptedProperties.getProperty(direction + ".service.id");
		}else {
			connectionId = decryptedProperties.getProperty(direction + "." + endpointId + ".service.id");
		}

        //Create datasource
        String driver = decryptedProperties.getProperty("service." + serviceId + ".driver");
        String url = decryptedProperties.getProperty("service." + serviceId + ".url");
        String username = decryptedProperties.getProperty("service." + serviceId + ".username");
        String password = decryptedProperties.getProperty("service." + serviceId + ".password");

        log.info("Create datasource for url: " + url + "(driver=" + driver + ")");

        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName(driver);
        ds.setUrl(url);
        ds.setUsername(username);
        ds.setPassword(password);

        //Add datasource to registry
        Registry registry = context.getRegistry();
        registry.bind(connectionId, ds);

        log.info("Datasource has been created");

    }

    private String createSSLEnabledUrl(String url) {

        String modifiedUrl = "";
        String multipleUrls = "";

        if (url.indexOf(",") != -1) {
            log.info("SSLEnabled Failover Url: ");

            if (url.indexOf("(") != -1) {
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

            if (url.indexOf("(") != -1) {
                modifiedUrl = "failover:(" + modifiedUrl + ")";
            }

        }else{
            log.info("SSLEnabled Normal Url: ");
            modifiedUrl = addSSLParameterToUrl(url);
        }

        if(!modifiedUrl.isEmpty()){
            url = modifiedUrl;
        }

        log.info("SSLEnabled Url: " + url);

        return url;

    }

    private String addSSLParameterToUrl(String url){

        String baseDirURI = baseDir.replace("\\", "/");

        if (url.indexOf("?") != -1) {

            String[] urlSplitted = url.split("/?");
            String[] optionsSplitted = urlSplitted[1].split("&");

            if (!Arrays.stream(optionsSplitted).anyMatch("transport.verifyHost"::startsWith)) {
                url = url + "&transport.verifyHost=false";
            }

            /*
            if (!Arrays.stream(optionsSplitted).anyMatch("transport.keyStoreLocation"::startsWith)) {
                url = url + "&transport.keyStoreLocation=" + baseDirURI + "/security/keystore.jks";
            }

            if (!Arrays.stream(optionsSplitted).anyMatch("transport.keyStorePassword"::startsWith)) {
                url = url + "&transport.keyStorePassword=supersecret";
            }*/

            if (!Arrays.stream(optionsSplitted).anyMatch("transport.trustStoreLocation"::startsWith)) {
                url = url + "&transport.trustStoreLocation=" + baseDirURI + "/security/truststore.jks";
            }

            if (!Arrays.stream(optionsSplitted).anyMatch("transport.trustStorePassword"::startsWith)) {
                url = url + "&transport.trustStorePassword=supersecret";
            }

        } else {
            url = url + "?transport.verifyHost=false&transport.trustAll=true&transport.trustStoreLocation=" + baseDirURI + "/security/truststore.jks" + "&transport.trustStorePassword=supersecret";
        }

        return url;

    }

    private EncryptableProperties decryptProperties(TreeMap<String, String> properties) {
        EncryptableProperties decryptedProperties = (EncryptableProperties) ((PropertiesComponent) context.getPropertiesComponent()).getInitialProperties();
        decryptedProperties.putAll(properties);
        return decryptedProperties;
    }

}