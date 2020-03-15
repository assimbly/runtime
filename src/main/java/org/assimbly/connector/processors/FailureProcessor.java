package org.assimbly.connector.processors;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.commons.io.FileUtils;
import org.assimbly.connector.connect.util.BaseDirectory;
import org.assimbly.connector.event.FlowEvent;

public class FailureProcessor implements Processor {
	
    private FlowEvent flowEvent;
    private final String baseDir = BaseDirectory.getInstance().getBaseDirectory();

	  
	public void process(Exchange exchange) throws Exception {
		  
		  Date date = new Date();
		  String today = new SimpleDateFormat("yyyyMMdd").format(date);
		  String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:SS Z").format(date);
		  flowEvent = new FlowEvent(exchange.getFromRouteId(),date,exchange.getException().getMessage());
		  			  
		  File file = new File(baseDir + "/alerts/" + flowEvent.getFlowId() + "/" + today + "_alerts.log");
		  List<String> line = Arrays.asList(timestamp + " : " + flowEvent.getError());
		  FileUtils.writeLines(file, line, true);
		
	  }
	
}
