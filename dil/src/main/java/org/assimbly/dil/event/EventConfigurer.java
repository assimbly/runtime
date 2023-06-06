package org.assimbly.dil.event;

import ch.qos.logback.classic.Level;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.camel.CamelContext;
import org.apache.camel.spi.EventNotifier;
import org.assimbly.dil.event.collect.MessageCollector;
import org.assimbly.dil.event.collect.LogCollector;
import org.assimbly.dil.event.collect.StepCollector;
import org.assimbly.dil.event.domain.Collection;
import org.assimbly.dil.event.domain.Filter;
import org.assimbly.dil.event.domain.Store;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.LoggerContext;

import java.util.ArrayList;


public class EventConfigurer {

    protected Logger log = LoggerFactory.getLogger(getClass());

    private CamelContext context;

    private String collectorId;

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

        String result = configureCollector();

        return result;

    }

    public String add(Collection configuration) {

        this.configuration = configuration;

        String result = configureCollector();

        return result;

    }

    public String remove(String collectorId) {

        log.info("Removing collector with id=" + collectorId);

        Object collector = context.getRegistry().lookupByName(collectorId);

       if(collector instanceof MessageCollector){
           ((MessageCollector) collector).shutdown();
           context.getManagementStrategy().removeEventNotifier((EventNotifier)collector);
           log.info("Removed message collector with id=" + collectorId);
       }else if(collector instanceof StepCollector){
           ((StepCollector) collector).shutdown();
           context.getManagementStrategy().removeEventNotifier((EventNotifier)collector);
           log.info("Removed step collector with id=" + collectorId);
       }else if(collector instanceof LogCollector ){
            LogCollector logCollector = (LogCollector) collector;
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
        }else if(!type.equals("log") && !type.equals("message") && !type.equals("step")){
            return "Invalid event collector: " + type + ". Valid types are message,log or step.";
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
        if(collector==null){
            return false;
        }else{
            return true;
        }
    }

    public String configureCollector(){

        String checkMessage = checkConfiguration();

        if(!checkMessage.equals("ok")){
            return checkMessage;
        }else{

            try {

                switch (type) {
                    case "message":
                        configureMessageCollector();
                        break;
                    case "step":
                        configureStepCollector();
                        break;
                    case "log":
                        configureLogCollector();
                        break;
                }

            } catch (Exception e){
                e.printStackTrace();
                return e.getMessage();
            }

            return "configured";

        }

    }

    public void configureStepCollector() {

        log.info("Configure collection of step events");
        String id = configuration.getId();
        ArrayList<String> events = configuration.getEvents();
        ArrayList<Filter> filters = configuration.getFilters();
        ArrayList<Store> stores = configuration.getStores();

        StepCollector stepCollector = new StepCollector(id, events, filters, stores);
        stepCollector.setIgnoreCamelContextEvents(true);
        stepCollector.setIgnoreCamelContextInitEvents(true);
        stepCollector.setIgnoreExchangeEvents(true);
        stepCollector.setIgnoreServiceEvents(true);
        stepCollector.setIgnoreStepEvents(true);

        context.getManagementStrategy().addEventNotifier(stepCollector);
        context.getRegistry().bind(id, stepCollector);
    }

    public void configureMessageCollector() {

        log.info("Configure collection of message events");

        String id = configuration.getId();
        ArrayList<String> events = configuration.getEvents();
        ArrayList<Filter> filters = configuration.getFilters();
        ArrayList<Store> stores = configuration.getStores();

        MessageCollector messageCollector = new MessageCollector(id, events, filters, stores);
        messageCollector.setIgnoreCamelContextEvents(true);
        messageCollector.setIgnoreCamelContextInitEvents(true);
        messageCollector.setIgnoreRouteEvents(true);
        messageCollector.setIgnoreServiceEvents(true);
        messageCollector.setIgnoreStepEvents(true);

        context.getManagementStrategy().addEventNotifier(messageCollector);
        context.getRegistry().bind(id, messageCollector);

    }

    public void configureLogCollector() {

        log.info("Configure collection of log events");

        String id = configuration.getId();
        String tag = "LOG";
        ArrayList<String> events = configuration.getEvents();
        ArrayList<Filter> filters = configuration.getFilters();
        ArrayList<Store> stores = configuration.getStores();

        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

        LogCollector logCollector = new LogCollector(id, tag, events, filters, stores);
        logCollector.setContext(loggerContext);
        logCollector.setName(collectorId);
        logCollector.start();

        ArrayList<String> packageNames = configuration.getEvents();

        if(packageNames.size() > 0) {
            for (String packageName : packageNames) {
                log.info("Add log event: " + packageName);
                addLogger(logCollector, packageName, "info");
            }
        }else{
            log.error("No log events are configured. Please provide one or more packageName");
        }

        context.getRegistry().bind(id, logCollector);

    }

    private void addLogger(LogCollector logCollector, String packageName, String logLevel){

        ch.qos.logback.classic.Logger logbackLogger = null;

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