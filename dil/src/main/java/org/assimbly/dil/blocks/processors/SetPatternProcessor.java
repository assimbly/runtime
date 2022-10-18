package org.assimbly.dil.blocks.processors;

import org.apache.camel.*;
import org.apache.camel.language.xpath.XPathBuilder;
import org.apache.camel.spi.Language;

import javax.xml.xpath.XPathFactory;

//set headers for each step
public class SetPatternProcessor implements Processor {

	public void process(Exchange exchange) throws Exception {

		  String pattern = exchange.getProperty("pattern",String.class);

		  if(pattern != null){
			  if(pattern.equalsIgnoreCase("InOnly") || pattern.equalsIgnoreCase("OneWay") || pattern.equalsIgnoreCase("Event") || pattern.equalsIgnoreCase("FireAndForget")){
				  exchange.setPattern(ExchangePattern.InOnly);
			  }else if(pattern.equalsIgnoreCase("Inout") || pattern.equalsIgnoreCase("RequestReply")){
				  exchange.setPattern(ExchangePattern.InOut);
			  }else if(pattern.equalsIgnoreCase("InOptionalOut")){
				  exchange.setPattern(ExchangePattern.InOptionalOut);
			  }
		  }

		  exchange.removeProperty("pattern");

	}

}