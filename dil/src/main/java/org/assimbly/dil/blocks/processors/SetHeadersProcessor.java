package org.assimbly.dil.blocks.processors;

import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.language.xpath.XPathBuilder;
import org.apache.camel.spi.Language;
import org.assimbly.util.IntegrationUtil;
import org.w3c.dom.*;
import javax.xml.xpath.XPathFactory;

//set headers for each step
public class SetHeadersProcessor implements Processor {

	public void process(Exchange exchange) throws Exception {

	  Message in = exchange.getIn();

	  String headers  = exchange.getProperty("assimbly.headers",String.class);

	  if(headers.startsWith("<headers")){

		  NodeList nodeList = IntegrationUtil.getNodeList(headers, "headers").item(0).getChildNodes();

		  for (int i = 0; i < nodeList.getLength(); i++) {

			  Node node = nodeList.item(i);

			  String language;
			  String type;
			  XPathFactory fac = new net.sf.saxon.xpath.XPathFactoryImpl();
			  
			  if (node.getNodeType() == Node.ELEMENT_NODE) {
				  String headerKey = node.getNodeName();
				  String headerValue = node.getTextContent();

				  Element elem = (Element) node;
				  language = elem.getAttribute("language");
				  type = elem.getAttribute("type");

				  String result;

				  if (language.equalsIgnoreCase("constant")) {
					  result = headerValue;
				  } else if (language.equalsIgnoreCase("xpath")) {
					  result = XPathBuilder.xpath(headerValue).factory(fac).evaluate(exchange, String.class);
				  } else {
					  Language resolvedLanguage = exchange.getContext().resolveLanguage(language);
					  Expression expression = resolvedLanguage.createExpression(headerValue);
					  result = expression.evaluate(exchange, String.class);
				  }

				  if(type.equalsIgnoreCase("property")){
					  exchange.setProperty(headerKey, result);
				  }else{
					  in.setHeader(headerKey, result);
				  }


			  }

			  exchange.removeProperty("assimbly.headers");

		  }

	  }

	}

}