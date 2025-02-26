package org.assimbly.dil.blocks.processors;

import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Processor;

//set headers for each step
public class SetPatternProcessor implements Processor {

	public void process(Exchange exchange) throws Exception {

		  String pattern = exchange.getProperty("pattern",String.class);

		  if(pattern != null){
			  if(pattern.equalsIgnoreCase("InOnly") || pattern.equalsIgnoreCase("OneWay") || pattern.equalsIgnoreCase("Event") || pattern.equalsIgnoreCase("FireAndForget")){
				  exchange.setPattern(ExchangePattern.InOnly);
			  }else if(pattern.equalsIgnoreCase("Inout") || pattern.equalsIgnoreCase("RequestReply")){
				  exchange.setPattern(ExchangePattern.InOut);
			  }
		  }

		  exchange.removeProperty("pattern");

	}

}