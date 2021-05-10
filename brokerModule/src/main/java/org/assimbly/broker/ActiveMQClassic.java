package org.assimbly.broker;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Map;

import org.apache.activemq.broker.BrokerFactory;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.broker.TransportConnector;
import org.apache.activemq.broker.jmx.BrokerView;
import org.apache.activemq.broker.jmx.ManagementContext;
import org.apache.activemq.broker.region.Destination;
import org.apache.activemq.command.ActiveMQDestination;
import org.apache.commons.io.FileUtils;
import org.assimbly.broker.Broker;
import org.assimbly.util.BaseDirectory;
import org.assimbly.util.ConnectorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

public class ActiveMQClassic implements Broker {

	private static Logger logger = LoggerFactory.getLogger("org.assimbly.broker.Broker");

    private final String baseDir = BaseDirectory.getInstance().getBaseDirectory();

	File brokerFile = new File(baseDir + "/broker/activemq.xml");

	BrokerService broker;
	private ManagementContext brokerManagement;

	public void setBaseDirectory(String baseDirectory) {
		BaseDirectory.getInstance().setBaseDirectory(baseDirectory);
	}
	
	public String start() throws Exception {

		broker = new BrokerService();

		if(brokerFile.exists()) {
			logger.info("Using config file 'activemq.xml'. Loaded from " + brokerFile.getCanonicalPath());
			URI urlConfig = new URI("xbean:" + URLEncoder.encode(brokerFile.getCanonicalPath(), "UTF-8"));
			broker = BrokerFactory.createBroker(urlConfig);
		}else {
			this.setFileConfiguration("");
			logger.warn("No config file 'activemq.xml' found.");
			logger.info("Created default 'activemq.xml' stored in following directory: " + brokerFile.getAbsolutePath());			
			logger.info("broker.xml documentation reference: https://activemq.apache.org/components/artemis/documentation/latest/configuration-index.html");
			logger.info("");
			logger.info("Start broker in local mode on url: tcp://127.0.0.1:61616");
			
			URI urlConfig = new URI("xbean:" + URLEncoder.encode(brokerFile.getCanonicalPath(), "UTF-8"));
			broker = BrokerFactory.createBroker(urlConfig);
		}		
		
		if(!broker.isStarted()) {
			broker.start();
		}
		
		return status();

	}


	public String startEmbedded() throws Exception {

			broker = new BrokerService();

			logger.warn("Start broker in local mode on url: tcp://127.0.0.1:61616");
			
			TransportConnector connector = new TransportConnector();
			connector.setUri(new URI("tcp://127.0.0.1:61616"));
			
			broker.addConnector(connector);		
		
			if(!broker.isStarted()) {
				broker.start();
			}
			
			return status();

	}	

	public String stop() throws Exception {
		broker.stop();
		
		return status();
	}

	
	public String restart() throws Exception {
		this.stop();
		this.start();
		
		return status();
	}

	public String restartEmbedded() throws Exception {
		this.stop();
		this.startEmbedded();
		
		return status();
	}
	
	public String status() throws Exception {

		if(broker==null) {
			broker = new BrokerService();
		}
		
		if(broker.isStarted()){
			return "started";
		}	
		else {
			return "stopped";
		}
	}
	
	public String getFileConfiguration() throws IOException {

		if(!brokerFile.exists()) {
			ClassLoader classloader = Thread.currentThread().getContextClassLoader();
			
    		try {
    			FileUtils.touch(brokerFile);
    			InputStream is = classloader.getResourceAsStream("activemq.xml");
    			Files.copy(is, brokerFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        		is.close();
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
    	
		return FileUtils.readFileToString(brokerFile, StandardCharsets.UTF_8);
					
	}
	
	public String setFileConfiguration(String brokerConfiguration) throws IOException {

		ClassLoader classloader = Thread.currentThread().getContextClassLoader();

		if(brokerFile.exists() || !brokerConfiguration.isEmpty()) {
			
			URL schemaFile = classloader.getResource("spring-beans.xsd");
			String xmlValidation = ConnectorUtil.isValidXML(schemaFile, brokerConfiguration);
			if(!xmlValidation.equals("xml is valid")) {
				return xmlValidation;
			} 
			
			FileUtils.writeStringToFile(brokerFile, brokerConfiguration,StandardCharsets.UTF_8);
		}else {
			FileUtils.touch(brokerFile);
			InputStream is = classloader.getResourceAsStream("activemq.xml");
			Files.copy(is, brokerFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
			is.close();
		}	
		
		return "configuration set";
	}


	@Override
	public String info() throws Exception {
		if(status().equals("started")) {

			BrokerView adminView = broker.getAdminView();
			
			String info = "uptime="+ broker.getUptime()
					 + 	",totalConnections=" + broker.getTotalConnections()
					 + ",totalConsumers=" + adminView.getTotalConsumerCount()
					 + ",totalMessages=" + adminView.getTotalMessageCount()
					 + ",nodeId=" + adminView.getBrokerId()
					 + ",state=" + broker.isStarted()
					 + ",version=" + adminView.getBrokerVersion()
					 + ",type=ActiveMQ Classic";
			return info;
		}else {
			return "no info. broker not running";
		}

	}

	@Override
	public String createQueue(String queueName) throws Exception {

		//ActiveMQDestination activeMQDestinationMQ =  ActiveMQDestination.createDestination(queueName,  ActiveMQDestination.QUEUE_TYPE );
		//activeMQDestinationMQ.setPhysicalName(queueName);

		// setDestination(ActiveMQDestination.createDestination(topic, ActiveMQDestination.TOPIC_TYPE));

		JMXServiceURL url = new JMXServiceURL("service:jmx:rmi://localhost:1098/jndi/rmi://localhost:1099/jmxrmi");
		JMXConnector jmxc = JMXConnectorFactory.connect(url);
		MBeanServerConnection conn = jmxc.getMBeanServerConnection();

		String operationName="addQueue";
		String parameter="MyNewQueue";
		ObjectName activeMQ = new ObjectName("org.apache.activemq:type=Broker,brokerName=localhost"); // new ObjectName("org.apache.activemq:BrokerName=localhost,Type=Broker");

		if(parameter != null) {
			Object[] params = {parameter};
			String[] sig = {"java.lang.String"};
			conn.invoke(activeMQ, operationName, params, sig);
		} else {
			conn.invoke(activeMQ, operationName,null,null);
		}



		return "succes";
	}

	@Override
	public String deleteQueue(String queueName) throws Exception {



		return null;
	}

	//https://dzone.com/articles/managing-activemq-jmx-apis

	@Override
	public String getQueue(String queueName) throws Exception {

		return "success";
	}

	@Override
	public String getQueues() throws Exception {
		return null;
	}

	@Override
	public String clearQueue(String queueName) throws Exception {
		return null;
	}

	@Override
	public String clearQueues() throws Exception {
		return null;
	}

	@Override
	public String createTopic(String topicName) throws Exception {
		return null;
	}

	@Override
	public String deleteTopic(String topicName) throws Exception {
		return null;
	}

	@Override
	public String getTopic(String topicName) throws Exception {
		return null;
	}

	@Override
	public String getTopics() throws Exception {
		return null;
	}

	@Override
	public String listMessages(String queueName, String filter) throws Exception {
		return null;
	}

	@Override
	public String removeMessage(String queueName, int message) throws Exception {
		return null;
	}

	@Override
	public String moveMessages(String sourceQueueName, String targetQueueName) throws Exception {
		return null;
	}

	@Override
	public String browseMessage(String queueName, String message) throws Exception {
		return null;
	}

	@Override
	public String browseMessages(String endpointName, Integer page, Integer numberOfMessages) throws Exception {
		return null;
	}

	@Override
	public String removeMessages(String queueName) throws Exception {
		return null;
	}

	@Override
	public String moveMessage(String sourceQueueName, String targetQueueName, String message) throws Exception {
		return null;
	}

	@Override
	public String sendMessage(String queueName, Map<String,String> messageHeaders, String messageBody, String userName, String password) throws Exception {
		return null;
	}

	@Override
	public String getConsumers() throws Exception {
		return null;
	}

	@Override
	public String getConnections() throws Exception {
		//int x = broker.getCurrentConnections();


		return null;
	}

	@Override
	public Object getBroker() throws Exception {
		return broker;
	}
	
	
}
