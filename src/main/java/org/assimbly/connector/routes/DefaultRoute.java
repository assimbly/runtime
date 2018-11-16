package org.assimbly.connector.routes;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

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
import org.apache.camel.builder.DefaultErrorHandlerBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.xml.XPathBuilder;
import org.apache.camel.component.file.GenericFile;
import org.apache.commons.io.FileUtils;
import org.assimbly.connector.event.FlowEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;


public class DefaultRoute extends RouteBuilder {
	
	Map<String, String> props;
	private DefaultErrorHandlerBuilder routeErrorHandler;
	private static Logger logger = LoggerFactory.getLogger("org.assimbly.connector.routes.DefaultRoute");
	
	public DefaultRoute(final Map<String, String> props){
		this.props = props;
	}

	public DefaultRoute() {
		// TODO Auto-generated constructor stub
	}

	public interface FailureProcessorListener {
		 public void onFailure();
  	}
	
	@Override
	public void configure() throws Exception {
			
		logger.info("Configuring default route");
		
		Processor setHeaders = new SetHeaders();
		Processor failureProcessor = new FailureProcessor();
		
		if (this.props.containsKey("error.uri")){
			routeErrorHandler = deadLetterChannel(props.get("error.uri"))
			.maximumRedeliveries(1)
			.redeliveryDelay(1000)
			.maximumRedeliveryDelay(60000)
			.backOffMultiplier(2)
			.retriesExhaustedLogLevel(LoggingLevel.ERROR)
			.retryAttemptedLogLevel(LoggingLevel.DEBUG)
			.onExceptionOccurred(failureProcessor)
			.log(log)
			.logRetryStackTrace(false)
			.logStackTrace(true)
			.logHandled(true)
			.logExhausted(true)
			.logExhaustedMessageHistory(true);					
		}
		else{
			routeErrorHandler = defaultErrorHandler()
			.maximumRedeliveries(1)
			.redeliveryDelay(1000)
			.maximumRedeliveryDelay(60000)
			.backOffMultiplier(2)
			.retriesExhaustedLogLevel(LoggingLevel.ERROR)
			.retryAttemptedLogLevel(LoggingLevel.DEBUG)
			.onExceptionOccurred(failureProcessor)
			.logRetryStackTrace(false)
			.logStackTrace(true)
			.logHandled(true)
			.logExhausted(true)
			.logExhaustedMessageHistory(true)
			.log(logger);
		}
		
		
		//the default Camel route
		from(props.get("from.uri"))			
	        .errorHandler(routeErrorHandler)
			.process(setHeaders)
			.convertBodyTo(String.class, "UTF-8")			
			.multicast()
			.shareUnitOfWork()
			.parallelProcessing()
				.to(getToUriList())
				.routeId(props.get("id"));
				
	}
	
	//create a string array for all consumers
	private String[] getToUriList() {
		
		String toUri = props.get("to.uri");
		
		if (this.props.containsKey("wiretap.uri") && this.props.containsKey("offloading")){
			if(props.get("offloading").equals("true")) {
				toUri = toUri + ",\"" + props.get("wiretap.uri") + "\"";
			}
		}
		
		toUri = toUri.substring(1, toUri.length()-1);
		
		String[] toUriArray = toUri.split("\",\"");
		
		return toUriArray;
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
	
	public class SetHeaders implements Processor {
		
		  public void process(Exchange exchange) throws Exception {
				Message in = exchange.getIn();
				for (Map.Entry<String, String> entry : props.entrySet()) {
					if (entry.getKey().startsWith("from.header.constant") || entry.getKey().startsWith("to.header.constant")) {
						String key = entry.getKey();
						in.setHeader(key.substring(key.lastIndexOf("constant") + 9),	entry.getValue());
					} else if (entry.getKey().startsWith("from.header.xpath") || entry.getKey().startsWith("to.header.xpath")) {
						String key = entry.getKey();
						in.setHeader(
								key.substring(key.lastIndexOf("xpath") + 6),
								XPathBuilder.xpath(entry.getValue())
										.evaluate(exchange,
												String.class));
					}
				}
				in.setHeader("Content-Type", props.get("header.contentype"));
				in.setHeader("FlowID", props.get("id"));
				in.setHeader("Source", props.get("from.uri"));
		  }		  
	}
	
	public class FailureProcessor implements Processor {
		
  	    private final String userHomeDir = System.getProperty("user.home");
  	    private FlowEvent flowEvent;
  	  
		public void process(Exchange exchange) throws Exception {
			  
			  Date date = new Date();
			  String today = new SimpleDateFormat("yyyyMMdd").format(date);
			  String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:SS Z").format(date);
			  flowEvent = new FlowEvent(exchange.getFromRouteId(),date,exchange.getException().getMessage());
			  			  
			  File file = new File(userHomeDir + "/assimbly/logs/alerts/" + flowEvent.getFlowId() + "/" + today + "_alerts.log");
			  List<String> line = Arrays.asList(timestamp + " : " + flowEvent.getError());
			  FileUtils.writeLines(file, line, true);
			
		  }
		
	}

}
