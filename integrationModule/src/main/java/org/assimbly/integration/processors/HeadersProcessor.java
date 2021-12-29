package org.assimbly.integration.processors;

import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.xpath.XPathFactory;

import com.ibm.msg.client.wmq.WMQConstants;
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

		  if (endpointIdObject != null) {

			  String endpointId = endpointIdObject.toString();

			  Map<String, String> headers = props.entrySet()
					  .stream()
					  .filter(map -> map.getKey().startsWith("header." + endpointId))
					  .collect(Collectors.toMap(map -> map.getKey(), map -> map.getValue()));

			  for (Map.Entry<String, String> entry : headers.entrySet()) {

				  String language = StringUtils.substringBetween(entry.getKey(), endpointId + ".", ".");
				  String key = StringUtils.substringAfterLast(entry.getKey(), language + ".");
				  String value = entry.getValue(); //StringUtils.substringAfterLast(entry.getValue(), "=");
				  String result = "";

				  if (language == null) {
					  continue;
				  } else if (language.equalsIgnoreCase("constant")) {
					  result = value;
				  } else if (language.equalsIgnoreCase("xpath")) {
					  XPathFactory fac = new net.sf.saxon.xpath.XPathFactoryImpl();
					  result = XPathBuilder.xpath(value).factory(fac).evaluate(exchange, String.class);
				  } else {
					  Language resolvedLanguage = exchange.getContext().resolveLanguage(language);
					  Expression expression = resolvedLanguage.createExpression(value);
					  result = expression.evaluate(exchange, String.class);
				  }

				  in.setHeader(key, result);

			  }

			  in.removeHeader("AssimblyHeaderId");
		  }

		  Object removeHeadersObject = in.getHeader("RemoveHeaders");
		  if (removeHeadersObject != null) {
			  String removeHeaders = removeHeadersObject.toString();
			  in.removeHeaders(removeHeaders);
		  }

	  }
}