package org.assimbly.dil.blocks.processors;

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
import org.assimbly.dil.event.domain.FlowEvent;
import org.assimbly.util.BaseDirectory;

import javax.xml.xpath.XPathFactory;

public class FailureProcessor implements Processor {
	
    private final String baseDir = BaseDirectory.getInstance().getBaseDirectory();
	private String flowId;
	private Map<String, String> props;

	public FailureProcessor(final Map<String, String> props){
		this.props = props;
	}

	public void process(Exchange exchange) throws Exception {

		//first set error headers
		Message in = exchange.getIn();

		String stepId = props.get("error.message.id");

		if (stepId != null) {

			Map<String, String> headers = props.entrySet()
					.stream()
					.filter(map -> map.getKey().startsWith("message." + stepId))
					.collect(Collectors.toMap(map -> map.getKey(), map -> map.getValue()));

			XPathFactory fac = new net.sf.saxon.xpath.XPathFactoryImpl();

			for (Map.Entry<String, String> entry : headers.entrySet()) {

				String language = StringUtils.substringBetween(entry.getKey(), stepId + ".", ".");
				String key = StringUtils.substringAfterLast(entry.getKey(), language + ".");
				String value = entry.getValue();
				String result;

				if (language == null) {
					continue;
				} else if (language.equalsIgnoreCase("constant")) {
					result = value;
				} else if (language.equalsIgnoreCase("xpath")) {
					result = XPathBuilder.xpath(value).factory(fac).evaluate(exchange, String.class);
				} else {
					Language resolvedLanguage = exchange.getContext().resolveLanguage(language);
					Expression expression = resolvedLanguage.createExpression(value);
					result = expression.evaluate(exchange, String.class);
				}

				in.setHeader(key, result);

			}

		}

		  //Write alert to disk
		  Date date = new Date();
		  String today = new SimpleDateFormat("yyyyMMdd").format(date);
		  String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:SS Z").format(date);
		FlowEvent flowEvent = new FlowEvent(exchange.getFromRouteId(), date, exchange.getException().getMessage());

			if(flowEvent.getFlowId().indexOf("-") == -1){
				flowId = flowEvent.getFlowId();
			}else{
				flowId = StringUtils.substringBefore(flowEvent.getFlowId(),"-");
			}

		  File file = new File(baseDir + "/alerts/" + flowId  + "/" + today + "_alerts.log");
		  List<String> line = Arrays.asList(timestamp + " : " + flowEvent.getError());
		  FileUtils.writeLines(file, line, true);
		
	  }
	
}
