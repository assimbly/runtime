package org.assimbly.dil.blocks.processors;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.commons.lang3.StringUtils;
import org.assimbly.dil.event.domain.FlowEvent;

import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

public class LastFailureProcessor implements Processor {

    public static final ConcurrentHashMap<String, Long> lastFailureTimestamp = new ConcurrentHashMap<>();

    @Override
    public void process(Exchange exchange) throws Exception {
        Date date = new Date();
        FlowEvent flowEvent = new FlowEvent(exchange.getFromRouteId(), date, exchange.getException().getMessage());

        String flowId;
        if(!flowEvent.getFlowId().contains("-")){
            flowId = flowEvent.getFlowId();
        }else{
            flowId = StringUtils.substringBefore(flowEvent.getFlowId(),"-");
        }

        lastFailureTimestamp.put(flowId, System.currentTimeMillis());
    }

}