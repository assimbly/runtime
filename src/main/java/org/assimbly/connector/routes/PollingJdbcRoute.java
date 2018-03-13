package org.assimbly.connector.routes;

import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.xml.XPathBuilder;
import org.apache.camel.component.sjms.jms.JmsMessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

public class PollingJdbcRoute extends RouteBuilder {

	Map<String, String> props;
	CamelContext context;
	
	private static Logger logger = LoggerFactory.getLogger("org.assimbly.connector.routes.PollingJdbcRoute");

	public PollingJdbcRoute(final Map<String, String> props) {
		this.props = props;
	}

	@Override
	public void configure() throws Exception {
			
		logger.info("Configuring Polling Jdbc route");
		
		if (this.props.containsKey("error.uri")){
			errorHandler(deadLetterChannel(props.get("error.uri"))
					.maximumRedeliveries(0).redeliveryDelay(1000)
					.maximumRedeliveryDelay(60000).backOffMultiplier(2)
					.retriesExhaustedLogLevel(LoggingLevel.ERROR)
					.retryAttemptedLogLevel(LoggingLevel.DEBUG)
					.logRetryStackTrace(false).logStackTrace(true)
					.logHandled(false).logExhausted(true)
					.logExhaustedMessageHistory(true));
		}
		else{
			errorHandler(defaultErrorHandler().maximumRedeliveries(0)
					.redeliveryDelay(1000).maximumRedeliveryDelay(60000)
					.backOffMultiplier(2)
					.retriesExhaustedLogLevel(LoggingLevel.ERROR)
					.retryAttemptedLogLevel(LoggingLevel.DEBUG)
					.logRetryStackTrace(false).logStackTrace(true)
					.logHandled(false).logExhausted(true)
					.logExhaustedMessageHistory(true));
		}
		String timerPeriod = props.get("from.timerPeriod");
		if (timerPeriod.isEmpty()){
			timerPeriod = "15m";
		}
		from("timer:foo?period="+ timerPeriod)
				.setBody(
						constant(props.get("from.query")))
				.to("jdbc:" + props.get("from.connection_id")).process(new Processor() {

					@SuppressWarnings("unchecked")
					@Override
					public void process(Exchange exchange) throws Exception {
						Message in = exchange.getIn();
						String sData = arrayList2XMLObject((ArrayList<LinkedHashMap<Object, Object>>) in.getBody());
						in.setBody(sData);
						
						for (Map.Entry<String, String> entry : props.entrySet()) {
							if (entry.getKey().startsWith("to.header.constant")) {
								String key = entry.getKey();
								in.setHeader(key.substring(19),
										entry.getValue());
							} else if (entry.getKey().startsWith(
									"to.header.xpath")) {
								String key = entry.getKey();
								in.setHeader(
										key.substring(16),
										XPathBuilder.xpath(entry.getValue())
												.evaluate(exchange,
														String.class));
							}
						}
						in.setHeader("Content-Type",
								props.get("header.contentype"));
						in.setHeader("JmsMessageType", JmsMessageType.Text);
					}
				}).to(props.get("to.uri")).routeId(props.get("id"));
	}

	private String arrayList2XMLObject(
			ArrayList<LinkedHashMap<Object, Object>> input) throws Exception {

		StringBuffer sb = new StringBuffer();
		sb.append("<Results>");
		for (LinkedHashMap<Object, Object> s : input) {
			sb.append("<Result>");
			for (Object key : s.keySet()) {
				sb.append("<");
				sb.append(key);
				sb.append(">");
				sb.append(s.get(key));
				sb.append("</");
				sb.append(key);
				sb.append(">");
			}
			sb.append("</Result>");
		}
		sb.append("</Results>");
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document document = builder.parse(new InputSource(new StringReader(sb
				.toString())));
		Transformer t = TransformerFactory.newInstance().newTransformer();
		t.setOutputProperty(OutputKeys.INDENT, "yes");
		t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		t.transform(new DOMSource(document), new StreamResult(out));
		return new String(out.toByteArray());
	}
}
