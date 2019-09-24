package org.assimbly.connector.service;

import java.io.File;

import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BrokerArtemis {

	private static Logger logger = LoggerFactory.getLogger("org.assimbly.connector.service.BrokerArtemis");

	EmbeddedActiveMQ broker = new EmbeddedActiveMQ();
	
	public void start() throws Exception {

		File brokerFile = new File("./broker.xml");
		
		if(brokerFile.exists()) {
			String fileConfig = "file:///" + brokerFile.getAbsolutePath();
			logger.info("Using config file 'broker.xml'. Loaded from " + brokerFile.getAbsolutePath());
			broker.setConfigResourcePath(fileConfig);
		}else {
			logger.warn("No config file 'broker.xml' found. Start broker in local mode on url: tcp://127.0.0.1:61616");
			logger.info("The 'broker.xml' should be store in following directory: " + brokerFile.getAbsolutePath());			
			logger.info("broker.xml documentation reference: https://activemq.apache.org/components/artemis/documentation/latest/configuration-index.html");
			Configuration config = new ConfigurationImpl();

			config.addAcceptorConfiguration("in-vm", "vm://0");
			config.addAcceptorConfiguration("tcp", "tcp://127.0.0.1:61616");
			config.setSecurityEnabled(false);
			broker.setConfiguration(config);
		}
		
		
		broker.start();

	}

	public void stop() throws Exception {
		ActiveMQServer activeBroker = broker.getActiveMQServer();
		if(activeBroker!=null) {
			broker.stop();
			activeBroker.stop();
		}	
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
}
