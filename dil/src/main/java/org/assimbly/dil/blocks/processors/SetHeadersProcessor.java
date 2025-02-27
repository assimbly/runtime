package org.assimbly.dil.blocks.processors;

import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.language.xpath.XPathBuilder;
import org.apache.camel.spi.Language;
import org.assimbly.util.IntegrationUtil;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPathFactory;

//set headers for each step
public class SetHeadersProcessor implements Processor {

	public void process(Exchange exchange) throws Exception {

	  Message in = exchange.getIn();

	  String headers  = exchange.getProperty("assimbly.headers",String.class);

	  if(headers.startsWith("<headers")){

		  NodeList nodeList = IntegrationUtil.getNodeList(headers, "headers").item(0).getChildNodes();

		  for (int i = 0; i < nodeList.getLength(); i++) {

			  Node header = nodeList.item(i);

			  if (header.getNodeType() == Node.ELEMENT_NODE) {

				  Element headerElem = (Element) header.getChildNodes();

				  String headerName = getElementValue(headerElem, "name");
				  String headerValue = getElementValue(headerElem, "value");
				  String headerLanguage = getElementValue(headerElem, "language");
				  String headerType = getElementValue(headerElem, "type");

				  String result = evaluateExpression(exchange, headerValue, headerLanguage);

				  if(headerType.equalsIgnoreCase("property")){
					  exchange.setProperty(headerName, result);
				  }else if(headerType.equalsIgnoreCase("variable")){
					  exchange.setVariable(headerName, result);
				  }else{
					  in.setHeader(headerName, result);
				  }

			  }

			  exchange.removeProperty("assimbly.headers");

		  }

	  }

	}

	private String evaluateExpression(Exchange exchange, String headerValue, String headerLanguage){
		String result;

		if (headerLanguage.equalsIgnoreCase("constant")) {
			result = headerValue;
		} else if (headerLanguage.equalsIgnoreCase("xpath")) {
			XPathFactory fac = new net.sf.saxon.xpath.XPathFactoryImpl();
			result = XPathBuilder.xpath(headerValue).factory(fac).evaluate(exchange, String.class);
		} else {
			Language resolvedLanguage = exchange.getContext().resolveLanguage(headerLanguage);
			Expression expression = resolvedLanguage.createExpression(headerValue);
			result = expression.evaluate(exchange, String.class);
		}

		return result;
	}

	private String getElementValue(Element element, String name){

		String value = "";
		NodeList nodeList = element.getElementsByTagName(name);

		if(nodeList.getLength()>0){
			value = nodeList.item(0).getTextContent();
		}

		return value;

	}


}