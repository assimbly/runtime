package org.assimbly.connector.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.apache.commons.io.FileUtils;
import org.assimbly.connector.connect.util.ConnectorUtil;
import org.assimbly.docconverter.DocConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BrokerArtemis {

	private static Logger logger = LoggerFactory.getLogger("org.assimbly.connector.service.BrokerArtemis");

	EmbeddedActiveMQ broker = new EmbeddedActiveMQ();

	File brokerFile = new File("./broker.xml");
	
	public void start() throws Exception {

		if(brokerFile.exists()) {
			String fileConfig = "file:///" + brokerFile.getAbsolutePath();
			logger.info("Using config file 'broker.xml'. Loaded from " + brokerFile.getAbsolutePath());

			broker.setConfigResourcePath(fileConfig);
		}else {
			this.setFileConfiguration("");
			logger.warn("No config file 'broker.xml' found.");
			logger.info("Create default 'broker.xml' stored in following directory: " + brokerFile.getAbsolutePath());			
			logger.info("broker.xml documentation reference: https://activemq.apache.org/components/artemis/documentation/latest/configuration-index.html");
			logger.info("");
			logger.info("Start broker in local mode on url: tcp://127.0.0.1:61616");
			
			String fileConfig = "file:///" + brokerFile.getAbsolutePath();
			broker.setConfigResourcePath(fileConfig);
		}		
		
		broker.start();

	}


	public void startEmbedded() throws Exception {

			logger.warn("Start broker in local mode on url: tcp://127.0.0.1:61616");
			Configuration config = new ConfigurationImpl();

			config.addAcceptorConfiguration("in-vm", "vm://0");
			config.addAcceptorConfiguration("tcp", "tcp://127.0.0.1:61616");
			config.setSecurityEnabled(false);
			broker.setConfiguration(config);
			broker.start();

	}

	
	public void stop() throws Exception {
		ActiveMQServer activeBroker = broker.getActiveMQServer();
		if(activeBroker!=null) {
			broker.stop();
			activeBroker.stop();
		}	
	}

	public void restart() throws Exception {
		this.stop();
		this.start();
	}

	
	public void restartEmbedded() throws Exception {
		this.stop();
		this.startEmbedded();
	}	
	
	public String status() throws Exception {
		String status = "stopped";
		ActiveMQServer activeBroker = broker.getActiveMQServer();
		if(activeBroker!=null) {
			if(activeBroker.isActive()) {
				status = "started";	
			}		
		}		
		return status;
	}
	
	public String getFileConfiguration() throws IOException {

		if(!brokerFile.exists()) {
			ClassLoader classloader = Thread.currentThread().getContextClassLoader();
			
    		try {
    			brokerFile.createNewFile();
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
			
			InputStream is = classloader.getResourceAsStream("broker.xml");
			FileUtils.writeStringToFile(brokerFile, brokerConfiguration,StandardCharsets.UTF_8);
		}else {
			
			brokerFile.createNewFile();
			InputStream is = classloader.getResourceAsStream("broker.xml");
			Files.copy(is, brokerFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
			is.close();
		}

		return "configuration set";
	}
}
