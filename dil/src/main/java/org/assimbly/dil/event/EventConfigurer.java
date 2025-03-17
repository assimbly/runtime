package org.assimbly.dil.event;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.camel.CamelContext;
import org.apache.camel.spi.EventNotifier;
import org.assimbly.dil.event.collect.LogCollector;
import org.assimbly.dil.event.collect.RouteCollector;
import org.assimbly.dil.event.collect.StepCollector;
import org.assimbly.dil.event.domain.Collection;
import org.assimbly.dil.event.domain.Filter;
import org.assimbly.dil.event.domain.Store;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;


public class EventConfigurer {

    protected Logger log = LoggerFactory.getLogger(getClass());

    private final CamelContext context;

    private final String collectorId;

    private Collection configuration;
    private String type;

    public EventConfigurer(String collectorId, CamelContext context) {
        this.collectorId = collectorId;
        this.context = context;
    }

    public String add(String jsonConfiguration) {

        log.info("Check event collector configuration:\n\n" + jsonConfiguration);

        try {
            configuration = new Collection().fromJson(jsonConfiguration);
        } catch (JsonProcessingException e) {
            return e.getMessage();
        }

        return configureCollector();

    }

    public String add(Collection configuration) {

        this.configuration = configuration;

        return configureCollector();

    }

    public String remove(String collectorId) {

        log.info("Removing collector with id=" + collectorId);

        Object collector = context.getRegistry().lookupByName(collectorId);

       if(collector instanceof StepCollector){
           ((StepCollector) collector).shutdown();
           context.getManagementStrategy().removeEventNotifier((EventNotifier)collector);
           log.info("Removed step collector with id=" + collectorId);
       }else if(collector instanceof RouteCollector){
           ((RouteCollector) collector).shutdown();
           context.getManagementStrategy().removeEventNotifier((EventNotifier)collector);
           log.info("Removed route collector with id=" + collectorId);
       }else if(collector instanceof LogCollector logCollector){
           removeLogger(logCollector);
            log.info("Removed log collector with id=" + collectorId);
        }else{
            log.warn("Collector with id=" + collectorId + " does not exist");
        }

        return "removed";
    }

    public String checkConfiguration() {

        if(configuration == null){
            return "Invalid event format (json)";
        }

        type = configuration.getType();

        if(type==null){
            return "The type of collector is missing. Valid types are: message,log or step.";
        }else if(!type.equals("log") && !type.equals("exchange") && !type.equals("route") && !type.equals("step")){
            return "Invalid event collector: " + type + ". Valid types are exchange, route, step or log.";
        }

        String id = configuration.getId();

        if(!this.collectorId.equals(id)){
            return "CollectorId of endpoint and configuration don't match. CollectorId endpoint=" + collectorId + " and CollectorID configuration=" + id;
        }

        //remove if configuration already exists
        if(isConfigured()){
            remove(collectorId);
        }

        log.info("Event collector configuration is valid");

        return "ok";

    }

    public boolean isConfigured(){
        Object collector = context.getRegistry().lookupByName(collectorId);

        return collector==null;

    }

    public String configureCollector(){

        String checkMessage = checkConfiguration();

        if(!checkMessage.equals("ok")){
            return checkMessage;
        }else{

            try {

                switch (type) {
                    case "step":
                        configureStepCollector();
                        break;
                    case "route":
                        configureRouteCollector();
                        break;
                    case "log":
                        configureLogCollector();
                        break;
                    default:
                        log.warn("Unknown collector type: " + type);
                }

            } catch (Exception e){
                e.printStackTrace();
                return e.getMessage();
            }

            return "configured";

        }

    }

    public void configureRouteCollector() {

        log.info("Configure collection of route events");
        String id = configuration.getId();
        String flowId = configuration.getFlowId();

        ArrayList<String> events = configuration.getEvents();
        ArrayList<Store> stores = configuration.getStores();

        RouteCollector routeCollector = new RouteCollector(id, flowId, events, stores);
        routeCollector.setIgnoreCamelContextEvents(true);
        routeCollector.setIgnoreCamelContextInitEvents(true);
        routeCollector.setIgnoreExchangeEvents(true);
        routeCollector.setIgnoreServiceEvents(true);
        routeCollector.setIgnoreStepEvents(true);
        routeCollector.setIgnoreRouteEvents(false);

        context.getManagementStrategy().addEventNotifier(routeCollector);
        context.getRegistry().bind(id, routeCollector);
    }

    public void configureStepCollector() {

        log.info("Configure collection of step events");

        String id = configuration.getId();
        String flowId = configuration.getFlowId();
        String flowVersion = configuration.getFlowVersion();
        ArrayList<String> events = configuration.getEvents();
        ArrayList<String> failedEvents = configuration.getFailedEvents();
        ArrayList<Filter> filters = configuration.getFilters();
        ArrayList<Store> stores = configuration.getStores();

        StepCollector stepCollector = new StepCollector(id, flowId, flowVersion, events, failedEvents, filters, stores);
        stepCollector.setIgnoreCamelContextEvents(true);
        stepCollector.setIgnoreCamelContextInitEvents(true);
        stepCollector.setIgnoreExchangeEvents(true);
        stepCollector.setIgnoreRouteEvents(true);
        stepCollector.setIgnoreServiceEvents(true);
        stepCollector.setIgnoreStepEvents(false);

        context.getManagementStrategy().addEventNotifier(stepCollector);
        context.getManagementStrategy().getEventFactory().setTimestampEnabled(true);
        context.getRegistry().bind(id, stepCollector);

    }

    public void configureLogCollector() {

        log.info("Configure collection of log events");

        String id = configuration.getId();
        String flowId = configuration.getFlowId();
        ArrayList<Filter> filters = configuration.getFilters();
        ArrayList<Store> stores = configuration.getStores();

        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

        LogCollector logCollector = new LogCollector(id, flowId, filters, stores);
        logCollector.setContext(loggerContext);
        logCollector.setName(collectorId);
        logCollector.start();

        ArrayList<String> packageNames = configuration.getEvents();

        if(!packageNames.isEmpty()) {
            for (String packageName : packageNames) {
                log.info("Add log event: {}", packageName);
                addLogger(logCollector, packageName, "info");
            }
        }else{
            log.error("No log events are configured. Please provide one or more packageName");
        }

        context.getRegistry().bind(id, logCollector);

    }

    private void addLogger(LogCollector logCollector, String packageName, String logLevel){

        ch.qos.logback.classic.Logger logbackLogger;

        if(packageName.equalsIgnoreCase("all") || packageName.equalsIgnoreCase("root")){
            logbackLogger = (ch.qos.logback.classic.Logger)LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        }else{
            logbackLogger = (ch.qos.logback.classic.Logger)LoggerFactory.getLogger(packageName);
        }

        //setAdditive to true, so that it's treated as an additional log (log is kept in the main log file)
        logbackLogger.setAdditive(true);
        logbackLogger.setLevel(Level.toLevel(logLevel));
        logbackLogger.addAppender(logCollector);

    }

    private void removeLogger(LogCollector logCollector){
        logCollector.stop();
        ch.qos.logback.classic.Logger logbackLogger = (ch.qos.logback.classic.Logger)LoggerFactory.getLogger(logCollector.getName());
        logbackLogger.detachAppender(logCollector.getName());
    }

}