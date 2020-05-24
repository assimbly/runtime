package org.assimbly.connector.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.apache.commons.io.FileUtils;
import org.assimbly.connector.Broker;
import org.assimbly.connector.connect.util.BaseDirectory;
import org.assimbly.connector.connect.util.ConnectorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActiveMQArtemis implements Broker {

	private static Logger logger = LoggerFactory.getLogger("org.assimbly.connector.service.BrokerArtemis");

	EmbeddedActiveMQ broker;
    private final String baseDir = BaseDirectory.getInstance().getBaseDirectory();

	File brokerFile = new File(baseDir + "/broker/broker.xml");

	public void setBaseDirectory(String baseDirectory) {
		BaseDirectory.getInstance().setBaseDirectory(baseDirectory);
	}
	
	public String start() throws Exception {

		broker = new EmbeddedActiveMQ();

		if(brokerFile.exists()) {
			String fileConfig = "file:///" + brokerFile.getAbsolutePath();
			logger.info("Using config file 'broker.xml'. Loaded from " + brokerFile.getAbsolutePath());
			logger.info("broker.xml documentation reference: https://activemq.apache.org/components/artemis/documentation/latest/configuration-index.html");
			broker.setConfigResourcePath(fileConfig);
		}else {
			
			this.setFileConfiguration("");
			logger.warn("No config file 'broker.xml' found.");
			logger.info("Created default 'broker.xml' stored in following directory: " + baseDir + "/broker");			
			logger.info("broker.xml documentation reference: https://activemq.apache.org/components/artemis/documentation/latest/configuration-index.html");
			logger.info("");
			logger.info("Start broker in local mode on url: tcp://127.0.0.1:61616");
			
			String fileConfig = "file:///" + brokerFile.getAbsolutePath();
			broker.setConfigResourcePath(fileConfig);
		}		
		
		broker.start();		

		return status();
	}


	public String startEmbedded() throws Exception {

			logger.warn("Start embedded broker in local mode on url: tcp://127.0.0.1:61616");

			Configuration config = new ConfigurationImpl();
			config.addAcceptorConfiguration("in-vm", "vm://0");
			config.addAcceptorConfiguration("tcp", "tcp://127.0.0.1:61616");
			config.setSecurityEnabled(false);

			broker = new EmbeddedActiveMQ();
			broker.setConfiguration(config);
			broker.start();

			return status();
	}

	
	public String stop() throws Exception {
		ActiveMQServer activeBroker = broker.getActiveMQServer();
		
		if(activeBroker!=null) {
			SimpleString nodeID= activeBroker.getNodeID();
			logger.info("Broker with nodeId '" + nodeID + "' is stopping. Uptime=" + activeBroker.getUptime());
			broker.stop();
			logger.info("Broker with nodeId '" + nodeID + "' is stopped.");
		}
		
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
		String status = "stopped";
		if(broker==null) {
			broker = new EmbeddedActiveMQ();
		}
		ActiveMQServer activeBroker = broker.getActiveMQServer();
		if(activeBroker!=null) {
			if(activeBroker.isActive()) {
				status = "started";	
			}		
		}		
		return status;
	}

	public String info() throws Exception {
		
		if(status().equals("started")) {
			ActiveMQServer activeBroker = broker.getActiveMQServer();
			String info = "uptime="+ activeBroker.getUptime() 
					 + ",totalConnections=" + activeBroker.getTotalConnectionCount()
					 + ",totalConsumers=" + activeBroker.getTotalConsumerCount()
					 + ",totalMessages=" + activeBroker.getTotalMessageCount()
					 + ",nodeId=" + activeBroker.getNodeID()
					 + ",state=" + activeBroker.getState()
					 + ",version=" + activeBroker.getVersion().getFullVersion()
					 + ",type=ActiveMQ Artemis";
			return info;
		}else {
			return "no info. broker not running";
		}
		
		
	}
	
	
	public String getFileConfiguration() throws IOException {

		if(!brokerFile.exists()) {
			ClassLoader classloader = Thread.currentThread().getContextClassLoader();
			
    		try {
    			FileUtils.touch(brokerFile);
    			InputStream is = classloader.getResourceAsStream("broker.xml");
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

			URL schemaFile = classloader.getResource("broker.xsd");
			String xmlValidation = ConnectorUtil.isValidXML(schemaFile, brokerConfiguration);
			if(!xmlValidation.equals("xml is valid")) {
				return xmlValidation;
			} 
			FileUtils.writeStringToFile(brokerFile, brokerConfiguration,StandardCharsets.UTF_8);
		}else {
			FileUtils.touch(brokerFile);
			InputStream is = classloader.getResourceAsStream("broker.xml");
			Files.copy(is, brokerFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
			is.close();
		}

		return "configuration set";
	}


	@Override
	public Object getBroker() throws Exception {
		return broker;
	}
}
