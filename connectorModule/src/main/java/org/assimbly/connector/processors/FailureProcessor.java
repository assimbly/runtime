package org.assimbly.connector.processors;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.assimbly.util.BaseDirectory;
import org.assimbly.connector.event.FlowEvent;

public class FailureProcessor implements Processor {
	
    private FlowEvent flowEvent;
    private final String baseDir = BaseDirectory.getInstance().getBaseDirectory();
	private String flowId;
	  
	public void process(Exchange exchange) throws Exception {
		  
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
