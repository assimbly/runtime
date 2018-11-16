package org.assimbly.connector.service;

import java.net.URI;

import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.broker.TransportConnector;

public class Broker {

	BrokerService broker = new BrokerService();

	public void start() throws Exception {
		
		TransportConnector connector = new TransportConnector();
		connector.setUri(new URI("tcp://localhost:61616"));
		broker.addConnector(connector);
		if(!broker.isStarted()) {
			broker.start();
		}
	}

	public void stop() throws Exception {
		if(broker.isStarted()) {
			broker.stop();
		}
	}
	
	public String status() throws Exception {
		if(broker.isStarted())
			return "started";
		else {
			return "stopped";
		}
	}
}
