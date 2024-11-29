package org.assimbly.dil.blocks.processors;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

public class SetLogProcessor implements Processor {

	public void process(Exchange exchange) throws Exception {

		JsonExchangeFormatter jsonFormatter = new JsonExchangeFormatter();
		jsonFormatter.setShowAll(true);

		String json = jsonFormatter.format(exchange);

		System.out.println(json);

	}

}