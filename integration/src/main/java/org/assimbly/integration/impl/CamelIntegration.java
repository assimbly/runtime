package org.assimbly.integration.impl;

import com.google.common.io.Resources;
import net.sf.saxon.xpath.XPathFactoryImpl;
import org.apache.camel.*;
import org.apache.camel.api.management.ManagedCamelContext;
import org.apache.camel.api.management.mbean.ManagedRouteMBean;
import org.apache.camel.catalog.DefaultCamelCatalog;
import org.apache.camel.catalog.EndpointValidationResult;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.engine.ExplicitCamelContextNameStrategy;
import org.apache.camel.language.xpath.XPathBuilder;
import org.apache.camel.spi.*;
import org.apache.camel.support.DefaultExchange;
import org.apache.camel.support.SimpleRegistry;
import org.apache.commons.lang3.StringUtils;
import org.assimbly.dil.validation.*;
import org.assimbly.dil.validation.beans.FtpSettings;
import org.assimbly.dil.validation.beans.Regex;
import org.assimbly.dil.validation.beans.ValidationExpression;
import org.assimbly.dil.validation.beans.script.EvaluationRequest;
import org.assimbly.dil.validation.beans.script.EvaluationResponse;
import org.assimbly.docconverter.DocConverter;
import org.assimbly.integration.impl.manager.ConfigManager;
import org.assimbly.integration.impl.manager.FlowManager;
import org.assimbly.integration.impl.manager.SSLManager;
import org.assimbly.integration.impl.manager.StatsManager;
import org.assimbly.util.EncryptionUtil;
import org.assimbly.util.error.ValidationErrorMessage;
import org.assimbly.util.helper.JsonHelper;
import org.jasypt.properties.EncryptableProperties;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.cert.Certificate;
import java.util.*;
import java.util.stream.IntStream;

public class CamelIntegration extends BaseIntegration {

    protected static final Logger log = LoggerFactory.getLogger(CamelIntegration.class);

    private final CamelContext context;
    private final SimpleRegistry registry = new SimpleRegistry();
    private final FlowManager flowManager;
    private final SSLManager sslManager;
    private final StatsManager statsManager;
    private final ConfigManager configManager;
    private boolean started;
    private RouteController routeController;
    private ManagedCamelContext managed;
    private Properties encryptionProperties;

    public CamelIntegration() throws Exception {
        super();
        context = new DefaultCamelContext(registry);
        this.sslManager = new SSLManager();
        this.flowManager = new FlowManager(context, flowsMap);
        this.statsManager = new StatsManager(context, flowManager, flowsMap);
        this.configManager = new ConfigManager(context, registry);
        init(false);
    }

    public CamelIntegration(boolean useDefaultSettings) throws Exception {
        super();
        context = new DefaultCamelContext(registry);
        this.sslManager = new SSLManager();
        this.flowManager = new FlowManager(context, flowsMap);
        this.statsManager = new StatsManager(context, flowManager, flowsMap);
        this.configManager = new ConfigManager(context, registry);
        init(useDefaultSettings);
    }

    public final void init(boolean useDefaultSettings) throws Exception {

        //set the name of the runtime
        context.setNameStrategy(new ExplicitCamelContextNameStrategy("assimbly"));
        context.setManagementName("assimbly");

        //setting tracing standby to true, so it can be enabled during runtime
        context.setTracingStandby(true);

        //set management tasks
        routeController = context.getRouteController();

        //load settings into a separate thread
        if (useDefaultSettings) {
            setDefaultSettings();
        }

    }

    public void setDefaultSettings() throws Exception {

        configManager.setRouteTemplates();

        configManager.setGlobalOptions();

        configManager.setDefaultBlocks();

        configManager.setDefaultThreadProfile(5, 50, 5000);

        configManager.setThreadProfile("wiretapProfile", 0, 10, 2500);

        configManager.setCertificateStore(true, sslManager);

        configManager.setDebugging(false);

        configManager.setSuppressLoggingOnTimeout(true);

        configManager.setStreamCaching(true);

        configManager.setMetrics(true, statsManager);

        configManager.setHistoryMetrics(true, statsManager);

        configManager.setHealthChecks(true, statsManager);

        setCaching(false);

    }

    public void start() throws Exception {

        // start Camel context
        if (!started) {

            context.start();
            started = true;

            log.info("Integration Runtime started");
        }

    }

    public void stop() throws Exception {
        super.getFlowConfigurations().clear();
        if (context != null) {
            for (Route route : context.getRoutes()) {
                routeController.stopRoute(route.getId());
                context.removeRoute(route.getId());
            }

            context.stop();
            started = false;
            log.info("Integration stopped");

        }
    }

    public boolean isStarted() {
        return started;
    }

    public String installFlow(String flowId, long timeout, String mediaType, String configuration) {

        try {
            super.setFlowConfiguration(flowId, mediaType, configuration);
        } catch (Exception e) {
            log.error("Flow configuration failed for flowId: {} and mediaType: {}", flowId, mediaType, e);
            flowManager.initFlowActionReport(flowId);
            flowManager.finishFlowActionReport(flowId, "error", e.getMessage(), "error");
            return flowManager.getFlowReport();
        }

        return flowManager.startFlow(flowId, super.getFlowConfiguration(flowId), sslManager, timeout);

    }

    public String uninstallFlow(String flowId, long timeout) {
        return flowManager.stopFlow(flowId, timeout);
    }

    public void setCaching(boolean cache) {

        if (cache) {
            initFlowDB();
            flowManager.startAllFlows(sslManager);
        } else {
            initFlowMap();
        }

    }

    public CamelContext getContext() {
        return context;
    }

    public ProducerTemplate getProducerTemplate() {
        return context.createProducerTemplate();
    }

    public ConsumerTemplate getConsumerTemplate() {
        return context.createConsumerTemplate();
    }

    public void send(Object messageBody, ProducerTemplate template) {
        template.sendBody(messageBody);
    }

    public void sendWithHeaders(Object messageBody, TreeMap<String, Object> messageHeaders, ProducerTemplate template) {
        template.sendBodyAndHeaders(messageBody, messageHeaders);
    }


    public void send(String uri, Object messageBody, Integer numberOfTimes) throws IOException {

        try (ProducerTemplate template = context.createProducerTemplate()) {

            if (numberOfTimes.equals(1)) {
                log.info("Sending {} message to {}", numberOfTimes, uri);
                template.sendBody(uri, messageBody);
            } else {
                log.info("Sending {} messages to {}", numberOfTimes, uri);
                IntStream.range(0, numberOfTimes).forEach(i -> template.sendBody(uri, messageBody));
            }
        }
    }

    public void sendWithHeaders(String uri, Object messageBody, TreeMap<String, Object> messageHeaders, Integer numberOfTimes) throws IOException {

        try (ProducerTemplate template = context.createProducerTemplate()) {

            Exchange exchange = new DefaultExchange(context);
            exchange.getIn().setBody(messageBody);
            exchange = setHeaders(exchange, messageHeaders);

            if (numberOfTimes.equals(1)) {
                log.info("Sending {} message to {}", numberOfTimes, uri);
                template.send(uri, exchange);
            } else {
                log.info("Sending {} messages to {}", numberOfTimes, uri);
                Exchange finalExchange = exchange;
                IntStream.range(0, numberOfTimes).forEach(i -> template.send(uri, finalExchange));
            }
        }

    }

    public String sendRequest(String uri, Object messageBody) throws IOException {

        try (ProducerTemplate template = context.createProducerTemplate()) {

            log.info("Sending request message to {}", uri);

            return template.requestBody(uri, messageBody, String.class);
        }
    }

    public String sendRequestWithHeaders(String uri, Object messageBody, TreeMap<String, Object> messageHeaders) {

        try (ProducerTemplate template = context.createProducerTemplate()) {
            Exchange exchange = new DefaultExchange(context);
            exchange.getIn().setBody(messageBody);
            exchange = setHeaders(exchange, messageHeaders);
            exchange.setPattern(ExchangePattern.InOut);

            log.info("Sending request message to {}", uri);
            Exchange result = template.send(uri, exchange);

            return result.getMessage().getBody(String.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public Exchange setHeaders(Exchange exchange, TreeMap<String, Object> messageHeaders) {
        for (Map.Entry<String, Object> messageHeader : messageHeaders.entrySet()) {

            String key = messageHeader.getKey();
            String value = StringUtils.substringBetween(messageHeader.getValue().toString(), "(", ")");
            String language = StringUtils.substringBefore(messageHeader.getValue().toString(), "(");
            String result;

            if (value.startsWith("constant")) {
                exchange.getIn().setHeader(key, value);
            } else if (value.startsWith("xpath")) {
                XPathFactory fac = new XPathFactoryImpl();
                result = XPathBuilder.xpath(key).factory(fac).evaluate(exchange, String.class);
                exchange.getIn().setHeader(key, result);
            } else {
                Language resolvedLanguage = exchange.getContext().resolveLanguage(language);
                Expression expression = resolvedLanguage.createExpression(value);
                result = expression.evaluate(exchange, String.class);
                exchange.getIn().setHeader(key, result);
            }

        }

        return exchange;
    }

    private TreeMap<String, String> getProperties(String flowId) {
        return super.getFlowConfigurations().stream()
                .filter(properties -> flowId.equals(properties.get("id")))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Flow not found"));
    }

    @Override
    public void setEncryptionProperties(Properties encryptionProperties) {
        this.encryptionProperties = encryptionProperties;
        EncryptionUtil encryptionUtil = getEncryptionUtil();

        EncryptableProperties encryptableProperties = new EncryptableProperties(encryptionUtil.getTextEncryptor());

        registry.bind("encryptableProperties", encryptableProperties);
        registry.bind("encryptionUtil", EncryptionUtil.class, encryptionUtil);

    }

    public EncryptionUtil getEncryptionUtil() {
        return new EncryptionUtil(encryptionProperties.getProperty("password"), encryptionProperties.getProperty("algorithm"));
    }

    @Override
    public String addCollectorsConfiguration(String mediaType, String configuration) throws Exception {
        return configManager.addCollectorsConfiguration(mediaType, configuration);
    }

    @Override
    public String addCollectorConfiguration(String collectorId, String mediaType, String configuration) throws Exception {
        return configManager.addCollectorConfiguration(collectorId, mediaType, configuration);
    }

    @Override
    public String removeCollectorConfiguration(String collectorId) {
        return configManager.removeCollectorConfiguration(collectorId);
    }

    @Override
    public String getStepTemplate(String mediaType, String stepName) throws Exception {
        return configManager.getStepTemplate(mediaType, stepName);
    }

    @Override
    public String getListOfStepTemplates() throws Exception {
        return configManager.getListOfStepTemplates();
    }

    public String getCamelRouteConfiguration(String id, String mediaType) throws Exception {

        StringBuilder buf = new StringBuilder();

        for (Route route : context.getRoutes()) {
            if (route.getId().equals(id) || route.getId().startsWith(id + "-")) {
                ManagedRouteMBean managedRoute = managed.getManagedRoute(route.getId());
                String xmlConfiguration = managedRoute.dumpRouteAsXml(true);
                xmlConfiguration = xmlConfiguration.replaceAll("<\\?xml(.+?)\\?>", "").trim();
                buf.append(xmlConfiguration);
            }
        }

        String camelRouteConfiguration = buf.toString();

        if (camelRouteConfiguration.isEmpty()) {
            camelRouteConfiguration = "0";
        } else {
            camelRouteConfiguration = "<routes xmlns=\"http://camel.apache.org/schema/spring\">" +
                    camelRouteConfiguration +
                    "</routes>";
            if (mediaType.contains("json")) {
                camelRouteConfiguration = DocConverter.convertXmlToJson(camelRouteConfiguration);
            }
        }

        return camelRouteConfiguration;
    }

    public String getListOfSoapActions(String url, String mediaType) {

        String result;

        Class<?> clazz;
        try {
            clazz = Class.forName("org.assimbly.soap.SoapActionsService");
            Object soapActions = clazz.getDeclaredConstructor().newInstance();
            Method method = clazz.getDeclaredMethod("getSoapActions", String.class);
            result = (String) method.invoke(soapActions, url);
        } catch (Exception e) {
            log.error("SOAP Actions couldn't be retrieved.", e);
            result = "[]";
        }

        return result;

    }

    public String getDocumentation(String componentType, String mediaType) {

        DefaultCamelCatalog catalog = new DefaultCamelCatalog();

        String doc = catalog.componentJSonSchema(componentType);

        if (doc == null || doc.isEmpty()) {
            doc = "Unknown component";
        }

        return doc;
    }

    public String getDocumentationVersion() {

        DefaultCamelCatalog catalog = new DefaultCamelCatalog();

        return catalog.getCatalogVersion();
    }

    public String getComponents(boolean includeCustomComponents, String mediaType) throws Exception {

        DefaultCamelCatalog catalog = new DefaultCamelCatalog();

        String components = catalog.listComponentsAsJson();

        if (includeCustomComponents) {
            URL url = Resources.getResource("custom-steps.json");
            String customComponent = Resources.toString(url, StandardCharsets.UTF_8);
            components = JsonHelper.mergeJsonArray(components, customComponent);
        }

        if (mediaType.contains("xml")) {
            components = DocConverter.convertJsonToXml(components);
        }

        return components;
    }

    public String getComponentSchema(String componentType, String mediaType) throws Exception {

        DefaultCamelCatalog catalog = new DefaultCamelCatalog();

        String schema = catalog.componentJSonSchema(componentType);

        if (schema == null || schema.isEmpty()) {
            URL url = Resources.getResource("custom-steps-parameters.json");
            String customSchemas = Resources.toString(url, StandardCharsets.UTF_8);
            JSONArray jsonArray = new JSONArray(customSchemas);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject components = jsonArray.getJSONObject(i);
                JSONObject component = components.getJSONObject("component");
                String name = component.getString("name");
                if (name.equalsIgnoreCase(componentType)) {
                    schema = components.toString();
                    break;
                }
            }
        }

        if (schema == null || schema.isEmpty()) {
            schema = "Unknown component";
        } else if (mediaType.contains("xml")) {
            schema = DocConverter.convertJsonToXml(schema);
        }

        return schema;
    }

    @Override
    public String getComponentParameters(String componentType, String mediaType) throws Exception {

        DefaultCamelCatalog catalog = new DefaultCamelCatalog();

        String parameters = catalog.componentJSonSchema(componentType);

        if (parameters == null || parameters.isEmpty()) {
            parameters = "Unknown component";
        } else if (mediaType.contains("xml")) {
            parameters = DocConverter.convertJsonToXml(parameters);
        }

        return parameters;
    }

    @Override
    public boolean hasFlow(String flowId) {
        return flowManager.hasFlow(flowId);
    }

    @Override
    public boolean isFlowStarted(String flowId) {
        return flowManager.isFlowStarted(flowId);
    }

    @Override
    public String getFlowInfo(String flowId, String mediaType) throws Exception {
        return flowManager.getFlowInfo(flowId, mediaType);
    }

    @Override
    public String getFlowStatus(String flowId) {
        return flowManager.getFlowStatus(flowId);
    }

    @Override
    public String getFlowUptime(String flowId) {
        return flowManager.getFlowUptime(flowId);
    }

    @Override
    public String getFlowLastError(String flowId) {
        return flowManager.getFlowLastError(flowId);
    }

    @Override
    public String getListOfFlows(String filter, String mediaType) throws Exception {
        return flowManager.getListOfFlows(filter, mediaType);
    }

    @Override
    public String getListOfFlowsDetails(String filter, String mediaType) throws Exception {
        return flowManager.getListOfFlowsDetails(filter, mediaType);
    }

    @Override
    public String resolveDependency(String scheme) {
        return flowManager.resolveDependency(scheme);
    }

    @Override
    public void setConnection(TreeMap<String, String> props, String stepType) throws Exception {
        flowManager.setConnection(props, stepType);
    }

    @Override
    public String info(String mediaType) throws Exception {
        return statsManager.info(mediaType);
    }

    @Override
    public String getFlowMessages(String flowId, boolean includeSteps, String mediaType) throws Exception {
        return statsManager.getFlowMessages(flowId, includeSteps, mediaType);
    }

    @Override
    public String getFlowTotalMessages(String flowId) {
        return statsManager.getFlowTotalMessages(flowId);
    }

    @Override
    public String getFlowCompletedMessages(String flowId) {
        return statsManager.getFlowCompletedMessages(flowId);
    }

    @Override
    public String getFlowFailedMessages(String flowId) {
        return statsManager.getFlowFailedMessages(flowId);
    }

    @Override
    public String getFlowPendingMessages(String flowId) {
        return statsManager.getFlowPendingMessages(flowId);
    }

    @Override
    public String getStepMessages(String flowId, String stepId, String mediaType) throws Exception {
        return statsManager.getStepMessages(flowId, stepId, mediaType);
    }

    @Override
    public String getFlowAlertsLog(String flowId, Integer numberOfEntries) throws Exception {
        return flowManager.getFlowAlertsLog(flowId, numberOfEntries);
    }

    @Override
    public String getFlowAlertsCount(String flowId) throws Exception {
        return flowManager.getFlowAlertsCount(flowId);
    }

    @Override
    public TreeMap<String, String> getIntegrationAlertsCount() {
        return flowManager.getIntegrationAlertsCount();
    }

    @Override
    public String getFlowEventsLog(String flowId, Integer numberOfEntries) throws Exception {
        return flowManager.getFlowEventsLog(flowId, numberOfEntries);
    }

    @Override
    public String getFlowStats(String flowId, boolean fullStats, boolean includeMetaData, boolean includeSteps, String filter) throws Exception {
        return statsManager.getFlowStats(flowId, fullStats, includeMetaData, includeSteps);
    }

    @Override
    public String getFlowStepStats(String flowId, String stepId, boolean fullStats) throws Exception {
        return statsManager.getFlowStepStats(flowId, stepId, fullStats);
    }

    @Override
    public String getHealth(String type, String mediaType) throws Exception {
        return statsManager.getHealth(type, mediaType);
    }

    @Override
    public String getHealthByFlowIds(String flowIds, String type, String mediaType) throws Exception {
        return statsManager.getHealthByFlowIds(flowIds, type, mediaType);
    }

    @Override
    public String getFlowHealth(String flowId, String type, boolean includeError, boolean includeSteps, boolean includeDetails, String mediaType) throws Exception {
        return statsManager.getFlowHealth(flowId, type, includeError, includeSteps, includeDetails, mediaType);
    }

    @Override
    public String getFlowStepHealth(String flowId, String stepId, String type, boolean includeError, boolean includeDetails, String mediaType) throws Exception {
        return statsManager.getFlowStepHealth(flowId, stepId, type, includeError, includeDetails, mediaType);
    }

    @Override
    public String getThreads(String mediaType, String filter, int topEntries) throws Exception {
        return statsManager.getThreads(mediaType, filter, topEntries);
    }

    @Override
    public String countFlows(String filter, String mediaType) {
        return statsManager.countFlows(filter);
    }

    @Override
    public String countSteps(String filter, String mediaType) {
        return statsManager.countSteps(filter);
    }

    @Override
    public Certificate[] getCertificates(String url) {
        return sslManager.getCertificates(url);
    }

    @Override
    public Certificate getCertificateFromKeystore(String keystoreName, String keystorePassword, String certificateName) {
        return sslManager.getCertificateFromKeystore(keystoreName, keystorePassword, certificateName);
    }

    @Override
    public void setCertificatesInKeystore(String keystoreName, String keystorePassword, String url) {
        sslManager.setCertificatesInKeystore(keystoreName, keystorePassword, url);
    }

    @Override
    public String importCertificateInKeystore(String keystoreName, String keystorePassword, String certificateName, Certificate certificate) {
        return sslManager.importCertificateInKeystore(keystoreName, keystorePassword, certificateName, certificate);
    }

    @Override
    public Map<String, Certificate> importCertificatesInKeystore(String keystoreName, String keystorePassword, Certificate[] certificates) throws Exception {
        return sslManager.importCertificatesInKeystore(keystoreName, keystorePassword, certificates);
    }

    @Override
    public Map<String, Certificate> importP12CertificateInKeystore(String keystoreName, String keystorePassword, String p12Certificate, String p12Password) throws Exception {
        return sslManager.importP12CertificateInKeystore(keystoreName, keystorePassword, p12Certificate, p12Password);
    }

    @Override
    public void deleteCertificateInKeystore(String keystoreName, String keystorePassword, String certificateName) {
        sslManager.deleteCertificateInKeystore(keystoreName, keystorePassword);
    }

    @Override
    public boolean removeFlow(String flowId) throws Exception {
        return flowManager.removeFlow(flowId);
    }

    @Override
    public void startAllFlows() {
        flowManager.startAllFlows(sslManager);
    }

    @Override
    public String restartAllFlows() {
        return flowManager.restartAllFlows(sslManager);
    }

    @Override
    public String pauseAllFlows() {
        return flowManager.pauseAllFlows();
    }

    @Override
    public String resumeAllFlows() {
        return flowManager.resumeAllFlows(sslManager);
    }

    @Override
    public String stopAllFlows() {
        return flowManager.stopAllFlows();
    }

    @Override
    public String startFlow(String flowId, long timeout) {
        TreeMap<String, String> flowProperties = getProperties(flowId);
        return flowManager.startFlow(flowId, flowProperties, sslManager, timeout);
    }

    @Override
    public String restartFlow(String flowId, long timeout) {
        TreeMap<String, String> flowProperties = getProperties(flowId);
        return flowManager.restartFlow(flowId, flowProperties, sslManager, timeout);
    }

    @Override
    public String stopFlow(String flowId, long timeout) {
        return flowManager.stopFlow(flowId, timeout);
    }

    @Override
    public String resumeFlow(String flowId) {
        TreeMap<String, String> flowProperties = getProperties(flowId);
        return flowManager.resumeFlow(flowId, flowProperties, sslManager);
    }

    @Override
    public String pauseFlow(String flowId) {
        return flowManager.pauseFlow(flowId);
    }

    @Override
    public String installRoute(String routeId, String route) throws Exception {
        return flowManager.installRoute(routeId, route);
    }

    @Override
    public String getStats(String mediaType) throws Exception {
        return statsManager.getStats(mediaType);
    }

    @Override
    public String getStepsStats(String mediaType) throws Exception {
        return statsManager.getStepsStats(mediaType);
    }

    @Override
    public String getFlowsStats(String mediaType) throws Exception {
        return statsManager.getFlowsStats(mediaType);
    }

    @Override
    public String getMessages(String mediaType) throws Exception {
        return statsManager.getMessages(mediaType);
    }

    @Override
    public String getStatsByFlowIds(String flowIds, String filter, String mediaType) {
        return statsManager.getStatsByFlowIds(flowIds);
    }

    @Override
    public String getMetrics(String mediaType) throws Exception {
        return statsManager.getMetrics(mediaType);
    }

    @Override
    public String getHistoryMetrics(String mediaType) throws Exception {
        return statsManager.getHistoryMetrics(mediaType);
    }

    public String validateFlow(String uri) {

        DefaultCamelCatalog catalog = new DefaultCamelCatalog();
        EndpointValidationResult valid = catalog.validateEndpointProperties(uri);

        if (valid.hasErrors()) {
            return "invalid: " + valid.summaryErrorMessage(false);
        } else {
            return "valid";
        }

    }

    @Override
    public ValidationErrorMessage validateCron(String cronExpression) {
        CronValidator cronValidator = new CronValidator();
        return cronValidator.validate(cronExpression);
    }

    @Override
    public HttpsCertificateValidator.ValidationResult validateCertificate(String httpsUrl) {
        HttpsCertificateValidator httpsCertificateValidator = new HttpsCertificateValidator();
        return httpsCertificateValidator.validate(httpsUrl);
    }

    @Override
    public ValidationErrorMessage validateUrl(String url) {
        UrlValidator urlValidator = new UrlValidator();
        return urlValidator.validate(url);
    }

    @Override
    public List<ValidationExpression> validateExpressions(List<org.assimbly.dil.validation.beans.ValidationExpression> expressions, boolean isPredicate) {
        ExpressionsValidator expressionValidator = new ExpressionsValidator();
        return expressionValidator.validate(expressions, isPredicate);
    }

    @Override
    public ValidationErrorMessage validateFtp(FtpSettings ftpSettings) throws IOException {
        FtpValidator ftpValidator = new FtpValidator();
        return ftpValidator.validate(ftpSettings);
    }

    @Override
    public AbstractMap.SimpleEntry<Integer, String> validateRegex(Regex regex) {
        RegexValidator regexValidator = new RegexValidator();
        return regexValidator.validate(regex);
    }

    @Override
    public EvaluationResponse validateScript(EvaluationRequest scriptRequest) {
        ScriptValidator scriptValidator = new ScriptValidator();
        return scriptValidator.validate(scriptRequest);
    }

    @Override
    public List<ValidationErrorMessage> validateXslt(String url, String xsltBody) {
        XsltValidator xsltValidator = new XsltValidator();
        return xsltValidator.validate(url, xsltBody);
    }

}