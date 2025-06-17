package org.assimbly.dil.blocks.processors;

import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.language.xpath.XPathBuilder;
import org.apache.camel.spi.Language;

import javax.xml.xpath.XPathFactory;

//set headers for each step
public class SetBodyProcessor implements Processor {

	public void process(Exchange exchange) throws Exception {

		  Message in = exchange.getIn();

		  String body = exchange.getProperty("assimbly.body",String.class);
		  String language = exchange.getProperty("assimbly.language",String.class);

		System.out.println("body: " + body);
		System.out.println("language: " + language);

		  if(language == null || language.isEmpty() || language.equalsIgnoreCase("constant")){
			  in.setBody(body);
		  }else{
			  if (language.equalsIgnoreCase("xpath")) {
				  XPathFactory fac = new net.sf.saxon.xpath.XPathFactoryImpl();
				  body = XPathBuilder.xpath(body).factory(fac).evaluate(exchange, String.class);
			  } else {
				  Language resolvedLanguage = exchange.getContext().resolveLanguage(language);
				  Expression expression = resolvedLanguage.createExpression(body);
				  body = expression.evaluate(exchange, String.class);
			  }
			  in.setBody(body);
		  }

		  exchange.removeProperty("assimbly.body");
		  exchange.removeProperty("assimbly.language");
	}

}