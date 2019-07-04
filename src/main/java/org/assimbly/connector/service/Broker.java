package org.assimbly.connector.service;

import java.io.File;
import java.net.URI;
import java.net.URLEncoder;

import org.apache.activemq.broker.BrokerFactory;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.broker.TransportConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Broker {

	private static Logger logger = LoggerFactory.getLogger("org.assimbly.connector.service.Broker");
	
	BrokerService broker = new BrokerService();

	public void start() throws Exception {
	
		File brokerFile = new File("./activemq.xml");
		
		if(brokerFile.exists()) {
			logger.info("Using config file 'activemq.xml'. Loaded from " + brokerFile.getCanonicalPath());
			URI urlConfig = new URI("xbean:" + URLEncoder.encode(brokerFile.getCanonicalPath(), "UTF-8"));
			broker = BrokerFactory.createBroker(urlConfig);
		}else {
			logger.warn("No config file 'activemq.xml' found. Start broker in local mode on url: tcp://127.0.0.1:61616");
			
			TransportConnector connector = new TransportConnector();
			connector.setUri(new URI("tcp://127.0.0.1:61616"));
			
			broker.addConnector(connector);		
		}
		
		if(!broker.isStarted()) {
			broker.start();
		}
	}

	public void stop() throws Exception {
		broker.stop();
	}
	
	public String status() throws Exception {
		if(broker.isStarted())
			return "started";
		else {
			return "stopped";
		}
	}
	
}
