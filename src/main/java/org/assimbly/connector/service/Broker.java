package org.assimbly.connector.service;

import java.net.URI;

import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.broker.TransportConnector;
import org.apache.activemq.command.ActiveMQDestination;

public class Broker {

	public void start() throws Exception {
		
		BrokerService broker = new BrokerService();
		
		
		ActiveMQDestination activeMQDestinationMQ =  ActiveMQDestination.createDestination("order",  (byte) 1 );	
	
		ActiveMQDestination[] destinations = new ActiveMQDestination[]{activeMQDestinationMQ};
		
		
		TransportConnector connector = new TransportConnector();
		connector.setUri(new URI("tcp://localhost:61616"));
		//broker.setBrokerName("embedded");
		broker.setDestinations(destinations);
		broker.addConnector(connector);
		broker.start();
		
	}
	
}
