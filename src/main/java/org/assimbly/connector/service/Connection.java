package org.assimbly.connector.service;

import java.util.List;
import java.util.Random;
import java.util.TreeMap;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.camel.component.ActiveMQComponent;
import org.apache.activemq.camel.component.ActiveMQConfiguration;
import org.apache.activemq.pool.PooledConnectionFactory;
import org.apache.camel.CamelContext;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.component.sjms.SjmsComponent;
import org.apache.camel.impl.PropertyPlaceholderDelegateRegistry;
import org.apache.camel.impl.SimpleRegistry;
import org.apache.camel.spi.Registry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import progress.message.jclient.ConnectionFactory;

public class Connection {

	private String uri;
	private String connectionId;
	private String flowId;
	private TreeMap<String, String> properties;
	private CamelContext context;
	private String connectId;
	private boolean faultTolerant;
	
	private static Logger logger = LoggerFactory.getLogger("org.assimbly.connector.service.Connection");
	
	public Connection(CamelContext context, TreeMap<String, String> properties) {
		this.context = context;
		this.properties = properties;
	}
	
	public TreeMap<String, String> start() throws Exception{
		
		if(properties.get("from.service.id")!=null){
			uri = properties.get("from.uri");
			startConnection(uri,"from");
		}
		if(properties.get("to.service.id")!=null){
			uri = properties.get("to.uri");
			startConnection(uri,"to");
		}
		
		if(properties.get("error.service.id")!=null){
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
		
		connectionId = properties.get(type + ".service.id");
		flowId = properties.get("id");
		
		if(uri!=null){
			
			if(connectionId!=null){
	
				String[] uriSplitted = uri.split(":",2);
				String component = uriSplitted[0];
				
				String options[] = {"activemq", "sonicmq", "sql"};
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
			        	
			        	if(type.equals("to")) {
							String newUri = null;
							for(String toUri : uri.split(",")) {
								connectId = type + connectionId + flowId + new Random().nextInt(1000000);
								setupSonicMQConnection(properties, type, connectId);
								if(newUri==null) {
									newUri = toUri.replace("sonicmq:", "sonicmq." + connectId + ":");
								}else {
									newUri = newUri + "," + toUri.replace("sonicmq:", "sonicmq." + connectId + ":");;
								}
							} 
				            uri = newUri;
			        	}else {
							connectId = type + connectionId + flowId + new Random().nextInt(1000000);
			        		setupSonicMQConnection(properties, type, connectId);				            
				            uri = uri.replace("sonicmq:", "sonicmq." + connectId + ":");
			        	}
						properties.put(type + ".uri", uri);						
			            break;
					case 2:
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

		connectionId = properties.get(type + ".service.id");
		flowId = properties.get("id");
		
		if(uri!=null && connectionId!=null){
	
			String[] uriSplitted = uri.split(":",2);
			String component = uriSplitted[0];
			String endpoint = uriSplitted[1];
			
			String options[] = {"activemq", "sonicmq", "jdbc"};
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
		String url = properties.get(direction + ".service.url");
		String conType = properties.get(direction + ".service.conType");
		String maxConnections = properties.get(direction + ".service.maxConnections");
		String concurentConsumers = properties.get(direction + ".service.concurentConsumers");
		
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
					logger.info("Started basic connection for activemq.");
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
						logger.info("Started pooled connection for activemq.");
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
	
		
	private void setupSonicMQConnection(TreeMap<String, String> properties, String direction, String connectId) throws Exception{

		String flowId = properties.get("id");
		String componentName = "sonicmq." + connectId;
		String url = properties.get(direction + ".service.url");
		String username = properties.get(direction + ".service.username");
		String password = properties.get(direction + ".service.password");
			
			if(url!=null || username !=null || password != null){

				
				if(properties.get(direction + ".service.faultTolerant")!=null) {
					try {
						Boolean.parseBoolean(properties.get(direction + ".service.faultTolerant"));
					} catch (Exception e) {
						faultTolerant = true;
					}
				}else {
					faultTolerant = true;
				}
				

				if(context.hasComponent(componentName) == null){
					ConnectionFactory connection = new ConnectionFactory (url,username, password);
					connection.setFaultTolerant(faultTolerant);
					connection.setConnectID("Assimbly/Gateway/" + connectionId + "/Flow/" + flowId  + "/" + connectId);
					connection.setPrefetchCount(10);
					
					SjmsComponent jms = new SjmsComponent();
					jms.setConnectionFactory(connection);
					jms.setConnectionClientId("Assimbly/Gateway/" + connectionId + "/Flow/" + flowId  + "/" + connectId);
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
		String url = properties.get(direction + ".service.url");
		String username = properties.get(direction + ".service.username");
		String password = properties.get(direction + ".service.password");

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
		
		String driver = properties.get(direction + ".service.driver");
		String url = properties.get(direction + ".service.url");		
		String username = properties.get(direction + ".service.username");
		String password = properties.get(direction + ".service.password");
		
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