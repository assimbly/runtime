package org.assimbly.connector.routes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.xml.XPathBuilder;
import org.apache.camel.component.file.GenericFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;


public class DefaultRoute extends RouteBuilder{
	
	Map<String, String> props;
	private static Logger logger = LoggerFactory.getLogger("org.assimbly.connector.routes.DefaultRoute");
	
	private int interval;
	private int intervalNrOfMessages = 0;
	private int totalNrOfMessages = 0;
	
	
	public DefaultRoute(final Map<String, String> props){
		this.props = props;
		if (this.props.containsKey("summaryInterval")){
			interval = Integer.parseInt(this.props.get("summaryInterval")) * 1000;
			Timer timer = new Timer();
			timer.scheduleAtFixedRate(new SummaryTask(), interval, interval);			
		}
	}

	@Override
	public void configure() throws Exception {
			
		logger.info("Configuring default route");
		
		if (this.props.containsKey("error.uri")){
			errorHandler(deadLetterChannel(props.get("error.uri"))
					.maximumRedeliveries(3).redeliveryDelay(1000)
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
		
		from(props.get("from.uri")).doTry().process(new Processor() {
			public void process(Exchange exchange) {
				Message in = exchange.getIn();
				intervalNrOfMessages++;
				totalNrOfMessages++;
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
				in.setHeader("Content-Type", props.get("header.contentype"));
								
			}
		})
		.convertBodyTo(String.class, "UTF-8")		
		.to(props.get("to.uri")).routeId(props.get("id")).doCatch(Exception.class).process(new ErrorProcessor());
	
	}
	
	
		
	@SuppressWarnings("unused")
	private String getContent(Object input){
		String text;
		if (input instanceof GenericFile<?>){
			try {
				GenericFile<?> gFile = (GenericFile<?>) input;
				String path = gFile.getAbsoluteFilePath();
				text = new String (Files.readAllBytes(Paths.get(path)));
			} catch (IOException e) {
				text = input.toString();
			}
		}
		else{
			text = input.toString();
		}
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document document = builder.parse(new InputSource(new StringReader(text)));
			Transformer t = TransformerFactory.newInstance().newTransformer();
			t.setOutputProperty(OutputKeys.INDENT, "yes");
			t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			t.transform(new DOMSource(document), new StreamResult(out));
			return new String(out.toByteArray());
		} catch (Exception e) {
			return text;
			
		} 
	}	
	
	public class getNumberOfMessages{
		
		public int run() {
			return totalNrOfMessages;
		}
	}
	
	
	private class SummaryTask extends TimerTask{

		@Override
		public void run() {
			logger.info("Received " + intervalNrOfMessages +  " messages last " + (interval / 1000) + " seconds. Received " + totalNrOfMessages  + " messages in total.");
			intervalNrOfMessages = 0;
		}
		
	}
	
	private class ErrorProcessor implements Processor{
		
		
		@Override
		public void process(Exchange exchange) throws Exception {
			logger.debug("Unrecoverable error occured.");
		}
		
	}

}
