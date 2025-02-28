package org.assimbly.dil.event.collect;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.spi.CamelEvent;
import org.apache.camel.support.EventNotifierSupport;
import org.apache.commons.lang3.StringUtils;
import org.assimbly.dil.event.domain.Filter;
import org.assimbly.dil.event.domain.Store;
import org.assimbly.dil.event.store.StoreManager;
import org.assimbly.dil.event.util.EventUtil;
import org.assimbly.dil.event.domain.MessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

//Check following page for all Event instances: https://www.javadoc.io/doc/org.apache.camel/camel-api/latest/org/apache/camel/spi/CamelEvent.html

public class ExchangeCollector extends EventNotifierSupport {
    private final StoreManager storeManager;
    private final String expiryInHours;
    private final ArrayList<Filter> filters;
    private final ArrayList<String> events;
    private final String collectorId;
    private final String flowId;
    private final String flowVersion;

    private static final String MSG_COLLECTOR_LIMIT_BODY_LENGTH = "MSG_COLLECTOR_LIMIT_BODY_LENGTH";
    private final int MSG_COLLECTOR_DEFAULT_LIMIT_BODY_LENGTH = 250000;

    private static final String BREADCRUMB_ID_HEADER = "breadcrumbId";
    public static final String COMPONENT_INIT_TIME_HEADER = "ComponentInitTime";

    public static final String RESPONSE_TIME_PROPERTY = "ResponseTime";
    public static final String TIMESTAMP_PROPERTY = "Timestamp";
    public static final String MESSAGE_HEADERS_SIZE_PROPERTY = "HeadersSize";
    public static final String MESSAGE_BODY_SIZE_PROPERTY = "BodySize";

    private static final String BLACKLISTED_ROUTES_PARTS = "BLACKLISTED_ROUTES_PARTS";
    private static String[] blacklistedRoutesParts = getBlacklistedRoutesParts();

    protected Logger log = LoggerFactory.getLogger(getClass());

    public ExchangeCollector(String collectorId, String flowId, String flowVersion, ArrayList<String> events, ArrayList<Filter> filters, ArrayList<org.assimbly.dil.event.domain.Store> stores) {
        this.collectorId = collectorId;
        this.flowId = flowId;
        this.flowVersion = flowVersion;
        this.events = events;
        this.filters = filters;
        this.storeManager = new StoreManager(collectorId, stores);
        List<Store> elasticStores = stores.stream().filter(p -> p.getType().equals("elastic")).collect(Collectors.toList());
        if(elasticStores.size()==1){
            this.expiryInHours = elasticStores.get(0).getExpiryInHours();
        }else{
            this.expiryInHours = "1";
        }
    }


    @Override
    public void notify(CamelEvent event) throws Exception {

        //filter only the configured events
        if (events != null && events.contains(event.getType().name())) {

            // Cast to exchange event
            CamelEvent.ExchangeEvent exchangeEvent = (CamelEvent.ExchangeEvent) event;

            // Get the message exchange from exchange event
            Exchange exchange = exchangeEvent.getExchange();

            // Get the stepid
            String routeId = exchange.getFromRouteId();

            if(routeId!= null && routeId.startsWith(flowId) && !isBlackListed(routeId)){

                String stepId = StringUtils.substringAfter(routeId, flowId + "-");

                // set custom properties
                setCustomProperties(exchange, stepId);

                //process and store the exchange
                processEvent(exchange, stepId);
            }
        }
    }

    private void processEvent(Exchange exchange, String stepId){

        //set fields
        Message message = exchange.getMessage();
        String body = getBody(exchange);
        Map<String, Object> headers = message.getHeaders();
        Map<String, Object> properties = exchange.getProperties();
        String messageId = message.getMessageId();

        //use breadcrumbId when available
        messageId = message.getHeader(BREADCRUMB_ID_HEADER, messageId, String.class);

        //calculate times
        String timestamp = EventUtil.getCreatedTimestamp(exchange.getCreated());
        String expiryDate = EventUtil.getExpiryTimestamp(expiryInHours);

        //create json
        MessageEvent messageEvent = new MessageEvent(
                timestamp, messageId, flowId, flowVersion, stepId, headers, properties, body, expiryDate
        );
        String json = messageEvent.toJson();

        //store the event
        storeManager.storeEvent(json);
    }


    public String getBody(Exchange exchange) {

        try {
            byte[] body = exchange.getMessage().getBody(byte[].class);
            int limitBodyLength = getLimitBodyLength();

            if (body == null || body.length == 0) {
                return "<empty>";
            }else if (body.length > limitBodyLength) {
                return new String(Arrays.copyOfRange(body, 0, limitBodyLength), StandardCharsets.UTF_8);
            }else{
                return new String (body, StandardCharsets.UTF_8);
            }

        } catch (Exception e) {
            String typeName = exchange.getMessage().getBody().getClass().getTypeName();
            if(typeName!= null && !typeName.isEmpty()){
                return "<" + typeName + ">";
            }else{
                return "<unable to convert>";
            }
        }

    }

    private int getLimitBodyLength() {
        try {
            String bodyLength = System.getenv(MSG_COLLECTOR_LIMIT_BODY_LENGTH);
            return Integer.parseInt(bodyLength);
        } catch (Exception e) {
            return MSG_COLLECTOR_DEFAULT_LIMIT_BODY_LENGTH;
        }
    }

    private void setCustomProperties(Exchange exchange, String stepId) {
        if (EventUtil.isFilteredEquals(filters, stepId)) {
            // set response time property
            setResponseTimeProperty(exchange);
        }

        // set timestamp property
        Calendar calNow = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSS");
        exchange.setProperty(TIMESTAMP_PROPERTY, sdf.format(calNow.getTime()));

        // set BodyLength property
        byte[] body = exchange.getMessage().getBody(byte[].class);
        exchange.setProperty(MESSAGE_BODY_SIZE_PROPERTY, body != null ? body.length : 0);

        // set HeadersLength property
        Map<String, Object> headersMap = MessageEvent.filterHeaders(exchange.getMessage().getHeaders());
        exchange.setProperty(MESSAGE_HEADERS_SIZE_PROPERTY, EventUtil.calcMapLength(headersMap));
    }

    private void setResponseTimeProperty(Exchange exchange){
        //Set default headers for the response time
        long created = exchange.getCreated();

        if(created!=0) {
            Object initTime = exchange.getIn().getHeader(COMPONENT_INIT_TIME_HEADER, Long.class);
            exchange.getIn().setHeader(COMPONENT_INIT_TIME_HEADER, created);
            if (initTime != null) {
                long duration = created - (long) initTime;
                exchange.setProperty(RESPONSE_TIME_PROPERTY, Long.toString(duration));
            }
        }
    }

    private boolean isBlackListed(String routeId) {
        return Arrays.stream(blacklistedRoutesParts).anyMatch(routeId::contains);
    }

    private static String[] getBlacklistedRoutesParts() {
        String[] blacklistedRoutesParts = {};

        try {
            String blacklistedRoutesPartsStr = System.getenv(BLACKLISTED_ROUTES_PARTS);
            if(blacklistedRoutesPartsStr!=null) {
                blacklistedRoutesParts = blacklistedRoutesPartsStr.split(",");
            }
            return blacklistedRoutesParts;

        } catch (Exception e) {
            return blacklistedRoutesParts;
        }
    }

}