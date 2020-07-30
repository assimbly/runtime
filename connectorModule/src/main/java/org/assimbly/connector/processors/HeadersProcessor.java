package org.assimbly.connector.processors;

import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.xpath.XPathFactory;

import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.language.xpath.XPathBuilder;
import org.apache.camel.spi.Language;
import org.apache.commons.lang3.StringUtils;

//set headers for each endpoint
public class HeadersProcessor implements Processor {
	
	Map<String, String> props;
	
	public HeadersProcessor(final Map<String, String> props){
		this.props = props;
	}

	
	  public void process(Exchange exchange) throws Exception {
		  
			Message in = exchange.getIn();
			Object endpointIdObject = in.getHeader("AssimblyHeaderId");
			
			
			if(endpointIdObject!=null) {
				
				String endpointId = endpointIdObject.toString();
			
			    Map<String, String> headers = props.entrySet()
			    	      .stream()
			    	      .filter(map -> map.getKey().startsWith("header." + endpointId))
			    	      .collect(Collectors.toMap(map -> map.getKey(), map -> map.getValue()));
				
				
				for (Map.Entry<String, String> entry : headers.entrySet()) {

					String key = entry.getKey();

					if (key.startsWith("header." + endpointId + ".constant")) {
						in.setHeader(StringUtils.substringAfterLast(key, "constant."), entry.getValue());
					}else if (key.startsWith("header." + endpointId + ".groovy")) {
						Language groovy = exchange.getContext().resolveLanguage("groovy");
						Expression groovyExpression = groovy.createExpression(entry.getValue());
						String groovyResult = groovyExpression.evaluate(exchange, String.class);
						in.setHeader(StringUtils.substringAfterLast(key, "groovy."), groovyResult);
					}else if (key.startsWith("header." + endpointId + ".jsonpath")) {
						Language jsonPath = exchange.getContext().resolveLanguage("jsonpath");
						Expression jsonExpression = jsonPath.createExpression(entry.getValue());
						String jsonpathResult = jsonExpression.evaluate(exchange, String.class);
						in.setHeader(StringUtils.substringAfterLast(key, "jsonpath."),jsonpathResult);
					}else if (key.startsWith("header." + endpointId + ".simple")) {
						Language simple = exchange.getContext().resolveLanguage("simple");
						in.setHeader(StringUtils.substringAfterLast(key, "simple."), simple.createExpression(entry.getValue()).evaluate(exchange, String.class));
					}else if (key.startsWith("header." + endpointId + ".xpath")) {
						XPathFactory fac = new net.sf.saxon.xpath.XPathFactoryImpl();
						String xpathResult = XPathBuilder.xpath(entry.getValue()).factory(fac).evaluate(exchange, String.class);
						in.setHeader(StringUtils.substringAfterLast(key, "xpath."),xpathResult);						
					}
				}
			}
	  }		  
}