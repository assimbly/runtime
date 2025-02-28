package org.assimbly.dil.blocks.processors;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.assimbly.dil.event.domain.FlowEvent;
import org.assimbly.util.BaseDirectory;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class FailureProcessor implements Processor {
	
    private final String baseDir = BaseDirectory.getInstance().getBaseDirectory();

	public void process(Exchange exchange) throws Exception {

		//Write alert to disk
		Date date = new Date();
		String today = new SimpleDateFormat("yyyyMMdd").format(date);
		String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z").format(date);
		FlowEvent flowEvent = new FlowEvent(exchange.getFromRouteId(), date, exchange.getException().getMessage());

		String flowId;
		if(flowEvent.getFlowId().indexOf("-") == -1){
			flowId = flowEvent.getFlowId();
		}else{
			flowId = StringUtils.substringBefore(flowEvent.getFlowId(),"-");
		}

		File file = new File(baseDir + "/alerts/" + flowId  + "/" + today + "_alerts.log");
		List<String> line = List.of(timestamp + " : " + flowEvent.getError());
		FileUtils.writeLines(file, line, true);
		
	}
	
}
