package org.assimbly.connector.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.apache.activemq.broker.BrokerFactory;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.broker.TransportConnector;
import org.apache.activemq.broker.jmx.BrokerView;
import org.apache.commons.io.FileUtils;
import org.assimbly.connector.Broker;
import org.assimbly.connector.connect.util.BaseDirectory;
import org.assimbly.connector.connect.util.ConnectorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActiveMQClassic implements Broker {

	private static Logger logger = LoggerFactory.getLogger("org.assimbly.connector.service.Broker");

    private final String baseDir = BaseDirectory.getInstance().getBaseDirectory();

	File brokerFile = new File(baseDir + "/broker/activemq.xml");

	BrokerService broker;

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
	public Object getBroker() throws Exception {
		return broker;
	}
	
	
}
