package org.assimbly.dil.event.collect;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.builder.ExpressionBuilder;
import org.apache.camel.spi.CamelEvent;
import org.apache.camel.support.EventNotifierSupport;
import org.assimbly.dil.event.domain.Filter;
import org.assimbly.dil.event.domain.Store;
import org.assimbly.dil.event.store.StoreManager;
import org.assimbly.dil.event.util.EventUtil;
import org.assimbly.dil.event.domain.MessageEvent;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

//Check following page for all Event instances: https://www.javadoc.io/doc/org.apache.camel/camel-api/latest/org/apache/camel/spi/CamelEvent.html

public class MessageCollector extends EventNotifierSupport {

    private final StoreManager storeManager;
    private final String expiryInHours;
    private final ArrayList<Filter> filters;
    private final ArrayList<String> events;

    public MessageCollector(String collectorId, ArrayList<String> events, ArrayList<Filter> filters, ArrayList<org.assimbly.dil.event.domain.Store> stores) {
        this.events = events;
        this.filters = filters;
        this.storeManager = new StoreManager(collectorId, stores);
        List<Store> elasticStores = stores.stream().filter(p -> p.getType().equals("elastic")).collect(Collectors.toList());
        if(elasticStores.size()==1){
            this.expiryInHours = elasticStores.get(0).getExpiryInHours();
        }else{
            this.expiryInHours = "8";
        }
    }


    @Override
    public void notify(CamelEvent event) throws Exception {

        String type = event.getType().name();

        //filter only the configured events
        if(events!=null && events.contains(type)) {

            //Cast to exchange event
            CamelEvent.ExchangeEvent exchangeEvent = (CamelEvent.ExchangeEvent) event;

            //Get the message exchange from exchange event
            Exchange exchange = exchangeEvent.getExchange();

            //get the stepid
            String stepId = ExpressionBuilder.routeIdExpression().evaluate(exchange, String.class);

            //process and store the exchange
            if(stepId!=null && filters==null){
                processEvent(exchange, stepId);
            }else if(stepId!=null && EventUtil.isFiltered(filters, stepId)){
                processEvent(exchange, stepId);
            }

        }

    }

    private void processEvent(Exchange exchange, String stepId){

        //set fields
        Message message = exchange.getMessage();
        String body = getBody(message);
        Map<String, Object> headers = message.getHeaders();
        String messageId = message.getMessageId();
        String timestamp = EventUtil.getTimestamp();

        //create json
        MessageEvent messageEvent = new MessageEvent(timestamp, messageId, stepId, "0", stepId, headers, body, expiryInHours);
        String json = messageEvent.toJson();

        //store the event
        storeManager.storeEvent(json);
    }

    public String getBody(Message message) {

        try {

            byte[] body = message.getBody(byte[].class);

            if (body == null) {
                return "<null>";
            }else if (body.length == 0) {
                return "<empty>";
            }if (!(message.getBody() instanceof String)) {
                String typeName = message.getBody().getClass().getTypeName();
                return "<" + typeName + ">";
            }else if (body.length > 250000) {
                return new String(Arrays.copyOfRange(body, 0, 250000));
            }else{
                return new String (body);
            }

        } catch (Exception e) {
            return "<unable to convert>";
        }

    }

}
