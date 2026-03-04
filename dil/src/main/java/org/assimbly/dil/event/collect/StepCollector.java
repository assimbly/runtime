package org.assimbly.dil.event.collect;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.spi.CamelEvent;
import org.apache.camel.support.EventNotifierSupport;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.assimbly.dil.event.domain.Filter;
import org.assimbly.dil.event.domain.MessageEvent;
import org.assimbly.dil.event.domain.Store;
import org.assimbly.dil.event.store.StoreManager;
import org.assimbly.dil.event.util.EventUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

//Check following page for all Event instances: https://www.javadoc.io/doc/org.apache.camel/camel-api/latest/org/apache/camel/spi/CamelEvent.html

public class StepCollector extends EventNotifierSupport {

    private final StoreManager storeManager;
    private final String expiryInHours;
    private final ArrayList<Filter> filters;
    private final ArrayList<String> events;
    private final String flowId;
    private final String flowVersion;

    private static final String MSG_COLLECTOR_LIMIT_BODY_LENGTH = "MSG_COLLECTOR_LIMIT_BODY_LENGTH";
    private static final int MSG_COLLECTOR_DEFAULT_LIMIT_BODY_LENGTH = 250000;
    
    private static final String BREADCRUMB_ID_HEADER = "breadcrumbId";
    public static final String COMPONENT_INIT_TIME_HEADER = "ComponentInitTime";
    public static final String FLOW_ID_HEADER = "TRACING_FLOWID";
    public static final String FLOW_VERSION_HEADER = "TRACING_FLOWVERSION";
    public static final String RESPONSE_TIME_PROPERTY = "ResponseTime";
    public static final String TIMESTAMP_PROPERTY = "Timestamp";
    public static final String MESSAGE_HEADERS_SIZE_PROPERTY = "HeadersSize";
    public static final String MESSAGE_BODY_SIZE_PROPERTY = "BodySize";
    public static final String MESSAGE_BODY_TYPE_PROPERTY = "BodyType";
    public static final String EXCHANGE_PATTERN_PROPERTY = "ExchangePattern";
    private static final String BLACKLISTED_ROUTES_PARTS = "BLACKLISTED_ROUTES_PARTS";
    private static final String[] blacklistedRoutesParts = getBlacklistedRoutesParts();

    private static final Charset CHARSET = StandardCharsets.UTF_8;

    // Add this to StepCollector class
    private static final ThreadPoolExecutor collectionPool = new ThreadPoolExecutor(
            10, 10, 0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(5000),
            new ThreadPoolExecutor.DiscardPolicy()
    );

    protected Logger log = LoggerFactory.getLogger(getClass());

    public StepCollector(String collectorId, String flowId, String flowVersion, ArrayList<String> events, ArrayList<Filter> filters, ArrayList<org.assimbly.dil.event.domain.Store> stores) {
        this.flowId = flowId;
        this.flowVersion = flowVersion;
        this.events = events;
        this.filters = filters;
        this.storeManager = new StoreManager(collectorId, stores);
        List<Store> elasticStores = stores.stream().filter(p -> p.getType().equals("elastic")).toList();
        if(elasticStores.size()==1){
            this.expiryInHours = elasticStores.getFirst().getExpiryInHours();
        }else{
            this.expiryInHours = "1";
        }
    }

    public static boolean isQueueReady() {
        return collectionPool.getQueue().remainingCapacity() > 200; // Keep a 10% buffer
    }


    @Override
    public void notify(CamelEvent event) {

        //filter only the configured events
        if (events != null && events.contains(event.getType().name())) {

            if (!isQueueReady()) {
                log.warn("Skipping event processing: ElasticStore is at capacity.");
                return;
            }

            // Cast to exchange event
            CamelEvent.StepEvent stepEvent = (CamelEvent.StepEvent) event;

            // Get the stepid
            String routeId = stepEvent.getStepId();
            String stepId = StringUtils.substringAfter(routeId, flowId + "-");

            if(stepId!= null && !isBlackListed(stepId) && (filters == null || EventUtil.isFilteredEquals(filters, stepId))){

                // materialize body BEFORE async
                byte[] body = stepEvent.getExchange().getMessage().getBody(byte[].class);
                // create a copy of the exchange for async processing
                Exchange exchange = stepEvent.getExchange().copy();
                // replace the body in the copied exchange with the materialized byte[]
                exchange.getMessage().setBody(body);

                long stepTimestamp = stepEvent.getTimestamp();

                // Hand off the HEAVY processing to a background thread
                collectionPool.submit(() -> processEvent(exchange, stepId, stepTimestamp));

            }

        }
    }

    private void processEvent(Exchange exchange, String stepId, long stepTimestamp){

        //set fields
        Message message = exchange.getMessage();
        Map<String, Object> headers = message.getHeaders();
        Map<String, Object> properties = exchange.getProperties();

        //use breadcrumbId when available, otherwise set custom
        String transactionId = message.getHeader(BREADCRUMB_ID_HEADER, String.class);
        if (transactionId == null || transactionId.isEmpty()) {
            transactionId = message.getMessageId() + "_" + stepId;
            message.setHeader(BREADCRUMB_ID_HEADER, transactionId);
        }

        // get previous flowId and flowVersion
        String previousFlowId = exchange.getMessage().getHeader(FLOW_ID_HEADER, String.class);
        String previousFlowVersion = exchange.getMessage().getHeader(FLOW_VERSION_HEADER, String.class);
        // set flowId and flowVersion
        exchange.getMessage().setHeader(FLOW_ID_HEADER, flowId);
        exchange.getMessage().setHeader(FLOW_VERSION_HEADER, flowVersion);

        //calculate times
        String timestamp = EventUtil.getCreatedTimestamp(stepTimestamp);
        String expiryDate = EventUtil.getExpiryTimestamp(expiryInHours);

        MessageEvent messageEvent = getMessageEvent(exchange, stepId, stepTimestamp, timestamp, transactionId, previousFlowId,
                previousFlowVersion, headers, properties, expiryDate, isExceptionCaught(exchange));

        String json = messageEvent.toJson();

        //store the event
        storeManager.storeEvent(json);
    }

    private MessageEvent getMessageEvent(
            Exchange exchange, String stepId, long stepTimestamp, String timestamp, String transactionId,
            String previousFlowId, String previousFlowVersion, Map<String, Object> headers, Map<String,
            Object> properties, String expiryDate, boolean isFailedExchange
    ) {
        // read body only once
        InputStream inputStream = exchange.getMessage().getBody(InputStream.class);

        byte[] body = null;
        if (inputStream != null && exchange.getMessage().getBody() != null) {
            try {
                body = IOUtils.toByteArray(inputStream);
            } catch (Exception _) {
                // Ignoring exception intentionally
            }
        }

        int bodyLength = body != null ? body.length : 0;
        String bodyType = body != null ? exchange.getMessage().getBody().getClass().getSimpleName() : "";

        // set custom properties
        setCustomProperties(exchange, bodyType, bodyLength, stepId, stepTimestamp);

        // body to store
        String bodyToStoreOnEvent = getBodyToStoreOnEvent(exchange, body);

        return new MessageEvent(
                timestamp, transactionId, flowId, flowVersion, previousFlowId, previousFlowVersion, stepId, headers,
                properties, bodyToStoreOnEvent, expiryDate, isFailedExchange
        );
    }

    public String getBodyToStoreOnEvent(Exchange exchange, byte[] body) {

        try {
            int limitBodyLength = getLimitBodyLength();

            if (body == null || body.length == 0) {
                return "<empty>";
            } else if (body.length <= limitBodyLength) {
                if (isText(body)) {
                    return new String(body, CHARSET);
                } else {
                    return "<binary content>";
                }
            }

            if (isText(body)) {
                return new String(Arrays.copyOfRange(body, 0, limitBodyLength), CHARSET);
            } else {
                return "<binary content>";
            }

        } catch (Exception _) {
            String typeName = exchange.getMessage().getBody().getClass().getTypeName();
            if(!typeName.isEmpty()){
                return "<" + typeName + ">";
            }else{
                return "<unable to convert>";
            }
        }
    }

    private boolean isText(byte[] data) {
        try {
            CharsetDecoder decoder = StepCollector.CHARSET.newDecoder();
            decoder.onMalformedInput(CodingErrorAction.REPORT);
            decoder.onUnmappableCharacter(CodingErrorAction.REPORT);
            decoder.decode(ByteBuffer.wrap(data));
            return true;
        } catch (CharacterCodingException _) {
            return false;
        }
    }

    private boolean isExceptionCaught(Exchange exchange) {
        Exception handled = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
        return handled != null;
    }

    private int getLimitBodyLength() {
        try {
            String bodyLength = System.getenv(MSG_COLLECTOR_LIMIT_BODY_LENGTH);
            return Integer.parseInt(bodyLength);
        } catch (Exception _) {
            return MSG_COLLECTOR_DEFAULT_LIMIT_BODY_LENGTH;
        }
    }

    private void setCustomProperties(Exchange exchange, String bodyType, int bodyLength, String stepId, long stepTimestamp) {
        if (EventUtil.isFilteredEquals(filters, stepId)) {
            // set response time property
            setResponseTimeProperty(exchange, stepTimestamp);
        }

        // set timestamp property
        Calendar calNow = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSS");
        exchange.setProperty(TIMESTAMP_PROPERTY, sdf.format(calNow.getTime()));

        // set BodyType property
        exchange.setProperty(MESSAGE_BODY_TYPE_PROPERTY, bodyType);

        // set BodyLength property
        exchange.setProperty(MESSAGE_BODY_SIZE_PROPERTY, bodyLength);

        // set HeadersLength property
        Map<String, Object> headersMap = MessageEvent.filterHeaders(exchange.getMessage().getHeaders());
        exchange.setProperty(MESSAGE_HEADERS_SIZE_PROPERTY, EventUtil.calcMapLength(headersMap));

        // set ExchangePattern name
        exchange.setProperty(EXCHANGE_PATTERN_PROPERTY, exchange.getPattern().name());

    }

    private void setResponseTimeProperty(Exchange exchange, long stepTimestamp){
        //Set default headers for the response time

        Object initTime = exchange.getIn().getHeader(COMPONENT_INIT_TIME_HEADER, Long.class);
        exchange.getIn().setHeader(COMPONENT_INIT_TIME_HEADER, stepTimestamp);
        if (initTime != null) {
            long duration = stepTimestamp - (long) initTime;
            exchange.setProperty(RESPONSE_TIME_PROPERTY, Long.toString(duration));
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

        } catch (Exception _) {
            return blacklistedRoutesParts;
        }
    }

}