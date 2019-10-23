package org.assimbly.connector.service;

import java.util.List;
import java.util.Random;
import java.util.TreeMap;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
//import org.apache.activemq.ActiveMQConnection;
//import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.artemis.jms.client.ActiveMQJMSConnectionFactory;
import org.apache.activemq.camel.component.ActiveMQComponent;
import org.apache.activemq.camel.component.ActiveMQConfiguration;
import org.apache.activemq.jms.pool.PooledConnectionFactory;
//import org.apache.activemq.camel.component.ActiveMQComponent;
//import org.apache.activemq.camel.component.ActiveMQConfiguration;
//import org.apache.activemq.pool.PooledConnectionFactory;
import org.apache.camel.CamelContext;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.component.sjms.SjmsComponent;
import org.apache.camel.impl.PropertyPlaceholderDelegateRegistry;
import org.apache.camel.impl.SimpleRegistry;
import org.apache.camel.spi.Registry;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import progress.message.jclient.ConnectionFactory;

public class Connection {

	private String uri;
	private String connectionId;
	private String flowId;
	private TreeMap<String, String> properties;
	private String key;
	private CamelContext context;
	private String connectId;
	private boolean faultTolerant;
	private Object endpointId;
	private String serviceId;
	
	private static Logger logger = LoggerFactory.getLogger("org.assimbly.connector.service.Connection");
	
	public Connection(CamelContext context, TreeMap<String, String> properties, String key) {
		this.context = context;
		this.properties = properties;
		this.key = key;
	}
	
	public TreeMap<String, String> start() throws Exception{
		
		serviceId = properties.get(key);
		System.out.println("komt hier start");
		
		if(key.startsWith("from")){
			uri = properties.get("from.uri");
			startConnection(uri,"from");
		}
		if(key.startsWith("to")){
			endpointId = StringUtils.substringBetween(key, "to.", ".service.id");
			System.out.println("enpointId="+endpointId);
			uri = properties.get("to." + endpointId + ".uri");
			startConnection(uri,"to");
		}
		
		if(key.startsWith("to")){
			uri = properties.get("error.uri");
			startConnection(uri, "error");
		}
		
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
		
		if(properties.get("error.service.id")!=null){
			uri = properties.get("error.uri");
			//stopConnection(uri, "error");
		}		
		
		return properties; 
		
    }
	
	
	private void startConnection(String uri, String type) throws Exception{
		
		if(type.equals("to")) {
			connectionId = properties.get("to." + endpointId + ".service.id");
		}else {
			connectionId = properties.get(type + ".service.id");
		}
		flowId = properties.get("id");
		
        System.out.println("connectionId="+connectionId);
		
		
		if(uri!=null){
			
			if(connectionId!=null){
	
				System.out.println("komt hier uri" + uri);
				
				String[] uriSplitted = uri.split(":",2);
				String component = uriSplitted[0];
				
				System.out.println("component hier uri" + component);
				
				String options[] = {"activemq", "sonicmq", "sjms","sql"};
				int i;
				for (i = 0; i < options.length; i++) {
					if (component != null && component.contains(options[i])) {
						break;
					}
				}
				
				switch (i) {
					case 0:
						setupActiveMQConnection(properties, type);
						break;
			        case 1:	
			        	
			        	System.out.println("komt hier12");
			        	
						connectId = type + connectionId + new Random().nextInt(1000000);
			        	setupSonicMQConnection(properties, type, connectId);				            
			        	System.out.println("komt hier13");
			        	uri = uri.replace("sonicmq:", "sonicmq." + flowId + connectId + ":");
			        	
				        System.out.println("type="+type);
				        System.out.println("uri="+uri);
				        
				        if(type.equals("to")) {
				        	System.out.println(type + "." + endpointId + ".uri");
							properties.put(type + "." + endpointId + ".uri", uri);
				        }else {
							properties.put(type + ".uri", uri);						
				        }
			            break;
					case 2:
						setupJMSConnection(properties, type);
						break;
			        case 3:
				        setupJDBCConnection(properties, type);
				        break;			            
			        default:
			        	logger.error("Connection parameters for component " + component + " are not implemented");
			            throw new Exception("Connection parameters for component " + component + " are not implemented");
				}
		
	
			}
		}
		
	}

	@SuppressWarnings("unused")
	private void stopConnection(String uri, String type) throws Exception{

		if(type.equals("to")) {
			connectionId = properties.get("to." + endpointId + ".service.id");
		}else {
			connectionId = properties.get(type + ".service.id");
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
		        	logger.error("Connection parameters for component " + component + " are not implemented");
		            throw new Exception("Connection parameters for component " + component + " are not implemented");
			}
		}
	}		
	
	
	
	private void setupActiveMQConnection(TreeMap<String, String> properties, String direction) throws Exception{
		
		String componentName = "activemq";
		
		String url = properties.get("service." + serviceId +".url");
		String conType = properties.get("service." + serviceId +".conType");
		String maxConnections = properties.get("service." + serviceId +"service.maxConnections");
		String concurentConsumers = properties.get("service." + serviceId +"service.concurentConsumers");
		
		if (conType == null){
			logger.info("No connection type specified. Setting up basic connection for activemq.");
			conType = "basic";
		}else if (!conType.equals("basic") && !conType.equals("pooled")){
			logger.info("Invalid connection type specified. Setting up basic connection for activemq.");
			conType = "basic";
		}else if (conType.equals("pooled")){
			
			if (maxConnections == null){
				maxConnections = "10";
			}
			if (concurentConsumers == null){
				concurentConsumers = "10";
			}
			
		}
		
		if(context.hasComponent(componentName) == null){
			if(url!=null){
				
				ActiveMQConnectionFactory activeMQConnectionFactory = new ActiveMQConnectionFactory(url);
				if (conType.equals("basic")){
					ActiveMQConnection connection = (ActiveMQConnection) activeMQConnectionFactory.createConnection();
					connection.start();
					context.addComponent(componentName, JmsComponent.jmsComponentAutoAcknowledge(activeMQConnectionFactory));
					logger.info("Started basic connection for ActiveMQ.");
				}
				else{
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
						logger.info("Started pooled connection for ActiveMQ.");
						logger.info("Maximum connections: " + maxConnections + " - concurentConsumers: " + concurentConsumers);
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
    													
				
				
			}else{
				logger.error("ActiveMQ connection parameters are invalid.");
				throw new Exception("ActiveMQ connection parameters are invalid.\n");
			}
		}		
	}
	

	private void setupJMSConnection(TreeMap<String, String> properties, String direction) throws Exception{
		
		if(direction.equals("to")) {
			direction = direction + "." + endpointId;
		}
		
		String componentName = "sjms";
		String url = properties.get("service." + serviceId + ".url");
		String username = properties.get("service."  + serviceId + ".username");
		String password = properties.get("service."  + serviceId + ".password");
		
		logger.info("Setting up sjms client connection for ActiveMQ Artemis.");
		if(url!=null){
			
			if(context.hasComponent(componentName) == null){
				
				org.apache.activemq.artemis.jms.client.ActiveMQJMSConnectionFactory cf = null;
				
				if(username == null || password == null) {
					cf = new org.apache.activemq.artemis.jms.client.ActiveMQJMSConnectionFactory(url);
					cf.setConnectionTTL(-1);
				}else {
					cf = new org.apache.activemq.artemis.jms.client.ActiveMQJMSConnectionFactory(url, username, password);
					cf.setConnectionTTL(-1);
				}
				
				SjmsComponent component = new SjmsComponent();
				component.setConnectionFactory(cf);
				context.addComponent("sjms", component);
			}
			
		}
	}
	

	private ConnectionFactory createFactory(String string, int i, String string2, String string3, String string4) {
		// TODO Auto-generated method stub
		return null;
	}

	private void setupSonicMQConnection(TreeMap<String, String> properties, String direction, String connectId) throws Exception{

		String flowId = properties.get("id");
		String componentName = "sonicmq." + flowId + connectId;
		String url = properties.get("service." + serviceId + ".url");
		String username = properties.get("service."  + serviceId + ".username");
		String password = properties.get("service." + serviceId + ".password");
			
			if(url!=null || username !=null || password != null){
				
				if(properties.get("service." + serviceId + ".faultTolerant")!=null) {
					try {
						Boolean.parseBoolean(properties.get("service." + serviceId + ".faultTolerant"));
					} catch (Exception e) {
						faultTolerant = true;
					}
				}else {
					faultTolerant = true;
				}
				

				if(context.hasComponent(componentName) == null){
					ConnectionFactory connection = new ConnectionFactory (url,username, password);
					connection.setFaultTolerant(faultTolerant);
					connection.setConnectID("Assimbly/Gateway/" + connectionId + "/Flow/" + flowId + "/" + connectId);
					connection.setPrefetchCount(10);
					
					SjmsComponent jms = new SjmsComponent();
					jms.setConnectionFactory(connection);
					jms.setConnectionClientId("Assimbly/Gateway/" + connectionId + "/Flow/"  + flowId + "/" + connectId);
					jms.setCamelContext(context);
					jms.start();
				
					context.addComponent(componentName, jms);
				}	

				
			}else{
				logger.error("SonicMQ connection parameters are invalid or missing");				
				if(url==null) {
					logger.error("SonicMQ connection required parameter 'url' isn't set");
				}
				if(username==null) {
					logger.error("SonicMQ connection required parameter 'username' isn't set");
				}
				if(password==null) {
					logger.error("SonicMQ connection required parameter 'password' isn't set");
				}
				throw new Exception("SonicMQ connection parameters are invalid or missing.\n");
			}
		
	}

	@SuppressWarnings("unused")
	private void removeSonicMQConnection(TreeMap<String, String> properties, String direction, String connectId) throws Exception{

		String componentNamePrefix = "sonicmq." + connectId;
		String url = properties.get("service. " + serviceId + " .url");
		String username = properties.get("service." + serviceId + ".username");
		String password = properties.get("service." + serviceId + ".password");

		List<String> componentNames = context.getComponentNames();

		for(String componentName : componentNames) {

			if(componentName.startsWith(componentNamePrefix)) {
				if(url!=null || username !=null || password != null){
					
					ConnectionFactory connection = new ConnectionFactory (url,username, password);	
					SjmsComponent jms = new SjmsComponent();
					
					jms.setConnectionFactory(connection);
					jms.setCamelContext(context);
					
					jms.stop();
					context.removeComponent(componentName);
					
				}else{
					logger.error("SonicMQ connection parameters are invalid or missing");				
					if(url==null) {
						logger.error("SonicMQ connection required parameter 'url' isn't set");
					}
					if(username==null) {
						logger.error("SonicMQ connection required parameter 'username' isn't set");
					}
					if(password==null) {
						logger.error("SonicMQ connection required parameter 'password' isn't set");
					}
					throw new Exception("SonicMQ connection parameters are invalid or missing.\n");
				}
			}
		}
	}
	
	private void setupJDBCConnection(TreeMap<String, String> properties, String direction) throws Exception{
		
		String driver = properties.get("service."  + serviceId + ".driver");
		String url = properties.get("service" + serviceId + "url");		
		String username = properties.get("service." + serviceId + ".username");
		String password = properties.get("service." + serviceId + ".password");
		
		DriverManagerDataSource ds = new DriverManagerDataSource();
		ds.setDriverClassName(driver);
		ds.setUrl(url);
		ds.setUsername(username);
		ds.setPassword(password);
	
		Registry registry = context.getRegistry();
		
		if (registry instanceof PropertyPlaceholderDelegateRegistry){
		  registry =((PropertyPlaceholderDelegateRegistry)registry).getRegistry();
		 ((SimpleRegistry)registry).put(connectionId, ds); 
		}		
	}	
	
}