package org.assimbly.integration.processors;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.language.xpath.XPathBuilder;
import org.apache.camel.spi.Language;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.assimbly.util.BaseDirectory;
import org.assimbly.integration.event.FlowEvent;

import javax.xml.xpath.XPathFactory;

public class FailureProcessor implements Processor {
	
    private FlowEvent flowEvent;
    private final String baseDir = BaseDirectory.getInstance().getBaseDirectory();
	private String flowId;
	Map<String, String> props;

	public FailureProcessor(final Map<String, String> props){
		this.props = props;
	}

	public void process(Exchange exchange) throws Exception {

		//first set error headers
		Message in = exchange.getIn();
		String endpointId = props.get("error.header.id");

		if (endpointId != null) {

			Map<String, String> headers = props.entrySet()
					.stream()
					.filter(map -> map.getKey().startsWith("header." + endpointId))
					.collect(Collectors.toMap(map -> map.getKey(), map -> map.getValue()));

			for (Map.Entry<String, String> entry : headers.entrySet()) {

				String language = StringUtils.substringBetween(entry.getKey(), endpointId + ".", ".");
				String key = StringUtils.substringAfterLast(entry.getKey(), language + ".");
				String value = entry.getValue();
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

		  //Write alert to disk
		  Date date = new Date();
		  String today = new SimpleDateFormat("yyyyMMdd").format(date);
		  String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:SS Z").format(date);
		  flowEvent = new FlowEvent(exchange.getFromRouteId(),date,exchange.getException().getMessage());

			if(flowEvent.getFlowId().indexOf("-")!=-1){
				flowId = StringUtils.substringBefore(flowEvent.getFlowId(),"-");
			}else{
				flowId = flowEvent.getFlowId();
			}

		  File file = new File(baseDir + "/alerts/" + flowId  + "/" + today + "_alerts.log");
		  List<String> line = Arrays.asList(timestamp + " : " + flowEvent.getError());
		  FileUtils.writeLines(file, line, true);
		
	  }
	
}
