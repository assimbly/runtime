package org.assimbly.dil.transpiler.marshalling.core;

import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.apache.xerces.dom.DocumentImpl;
import org.assimbly.dil.transpiler.marshalling.catalog.CustomKameletCatalog;
import org.assimbly.docconverter.DocConverter;
import org.assimbly.util.IntegrationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.transform.*;
import javax.xml.xpath.*;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;

import static org.assimbly.util.IntegrationUtil.iterable;

public class RouteTemplate {

    protected Logger log = LoggerFactory.getLogger(getClass());
    private final TreeMap<String, String> properties;
    private final XMLConfiguration conf;
    private final Document templateDoc;
    private String uri;
    private Document contentRouteDoc;
    private Element templatedRoutes;
    private Element templatedRoute;
    private String routeId;
    private String templateId;
    private String path;
    private String scheme;
    private String options;
    private String blockType;
    private String transport;
    private String blockUri;
    private String baseUri;
    private String outList;
    private String outRulesList;

    private static final String ZERO = "0";
    private static final String SYNC_TRANSPORT = "sync";

    public RouteTemplate(TreeMap<String, String> properties, XMLConfiguration conf) {
        this.properties = properties;
        this.conf = conf;
        templateDoc = new DocumentImpl();
    }

    public TreeMap<String, String> setRouteTemplate(String flowId, String stepId, String type, String baseUri, String scheme, String path, String options,List<String> optionProperties, String[] links, String stepXPath, int stepIndex) throws Exception {

        this.baseUri = baseUri;
        this.options = options;
        this.scheme = scheme;
        this.path = path;

        routeId = flowId + "-" + stepId;

        templatedRoute = templateDoc.createElementNS("http://camel.apache.org/schema/spring", "templatedRoute");
        templatedRoute.setAttribute("routeTemplateRef", templateId);
        templatedRoute.setAttribute("group", flowId);

        createTemplatedRoutes();

        createTemplateId(baseUri, type);

        if(baseUri.equalsIgnoreCase("content") && type.equalsIgnoreCase("router") ) {
            if (contentRouteDoc == null) {
                contentRouteDoc = new DocumentImpl();
            }
            createContentRouter(links, stepXPath, type, stepId);
        }else if(baseUri.startsWith("block")){
            createCustomStep(optionProperties, links, type, stepXPath, stepIndex, flowId, stepId);
        }else{
            createStep(optionProperties, links, stepXPath,  stepIndex, type, flowId, stepId);
        }

        return properties;

    }

    private void createContentRouter(String[] links, String stepXPath, String type, String stepId)  throws Exception {

        createContentRoute(links, stepXPath);

        String route = DocConverter.convertDocToString(contentRouteDoc);

        properties.put(type + "." + stepId + ".route", route);
        properties.put(type + "." + stepId + ".route.id",  routeId);

    }

    private void createContentRoute(String[] links, String stepXPath){

        Element contentRoutes = contentRouteDoc.createElement("routes");
        contentRouteDoc.appendChild(contentRoutes);

        Element contentRoute = createRoute(links, stepXPath);
        contentRoutes.appendChild(contentRoute);

    }


    private Element createRoute(String[] links, String stepXPath){

        Element route = contentRouteDoc.createElement("route");
        route.setAttribute("id", routeId);

        Element fromEndpoint = createFrom(links, stepXPath);
        route.appendChild(fromEndpoint);

        Element choice = contentRouteDoc.createElement("choice");
        choice = createWhens(links, stepXPath, choice);
        choice = createOtherwise(links, stepXPath, choice);
        route.appendChild(choice);

        return route;

    }


    public Element createFrom(String[] links, String stepXPath) {

        Element fromEndpoint = contentRouteDoc.createElement("from");

        int index = 1;
        int linksLength = links.length;

        for(int i = 0; i < linksLength; i++) {

            String linkXPath = stepXPath + "links/link[" + index + "]/";

            //get values from configuration
            String bound = Objects.toString(conf.getProperty(linkXPath + "bound"), null);
            String linkTransport = createLinkTransport(linkXPath);
            String pattern = Objects.toString(conf.getProperty(linkXPath + "pattern"), null);
            String id = Objects.toString(conf.getProperty(linkXPath + "id"), null);
            options = createLinkOptions(linkXPath, bound, linkTransport, pattern);
            String endpoint = createLinkEndpoint(linkTransport, id);

            if(bound!=null && bound.equalsIgnoreCase("in") ){
                fromEndpoint.setAttribute("uri", endpoint);
            }

            index++;

        }

        return fromEndpoint;
    }


    public Element createWhens(String[] links, String stepXPath, Element choice) {

        int index = 1;

        for(int i = 0; i < links.length; i++) {

            Element when = contentRouteDoc.createElement("when");

            String linkXPath = stepXPath + "links/link[" + index + "]/";

            //get values from configuration
            String bound = Objects.toString(conf.getProperty(linkXPath + "bound"), null);
            String linkTransport = createLinkTransport(linkXPath);
            String pattern = Objects.toString(conf.getProperty(linkXPath + "pattern"), null);
            String id = Objects.toString(conf.getProperty(linkXPath + "id"), null);
            String rule = Objects.toString(conf.getProperty(linkXPath + "rule"), null);
            String expression = Objects.toString(conf.getProperty(linkXPath + "expression"), null);
            options = createLinkOptions(linkXPath, bound, linkTransport, pattern);
            String endpoint = createLinkEndpoint(linkTransport, id);

            if(bound!=null && bound.equalsIgnoreCase("out") && rule != null && expression != null) {

                Element elementRule = contentRouteDoc.createElement(rule);
                elementRule.setTextContent(expression);
                when.appendChild(elementRule);

                Element toEndpoint = contentRouteDoc.createElement("to");
                toEndpoint.setAttribute("uri",endpoint);
                when.appendChild(toEndpoint);

                choice.appendChild(when);

            }

            index++;

        }

        return choice;
    }


    public Element createOtherwise(String[] links, String stepXPath, Element choice) {

        for(int index = 1; index < links.length; index++) {

            Element otherwise = contentRouteDoc.createElement("otherwise");

            String linkXPath = stepXPath + "links/link[" + index + "]/";

            //get values from configuration
            String bound = Objects.toString(conf.getProperty(linkXPath + "bound"), null);
            String linkTransport = createLinkTransport(linkXPath);
            String pattern = Objects.toString(conf.getProperty(linkXPath + "pattern"), null);
            String id = Objects.toString(conf.getProperty(linkXPath + "id"), null);
            String rule = Objects.toString(conf.getProperty(linkXPath + "rule"), null);
            String expression = Objects.toString(conf.getProperty(linkXPath + "expression"), null);
            options = createLinkOptions(linkXPath, bound, linkTransport, pattern);
            String endpoint = createLinkEndpoint(linkTransport, id);

            if(bound!=null && bound.equalsIgnoreCase("out") && rule == null && expression == null) {

                Element toEndpoint = contentRouteDoc.createElement("to");
                toEndpoint.setAttribute("uri",endpoint);
                otherwise.appendChild(toEndpoint);

                choice.appendChild(otherwise);

            }

        }

        return choice;
    }

    private void createStep(List<String> optionProperties, String[] links, String stepXPath, int stepIndex, String type, String flowId, String stepId) throws Exception {

        createTemplatedRoute(optionProperties, links, stepXPath, stepIndex, type, flowId);

        String routeTemplate = DocConverter.convertDocToString(templateDoc);

        properties.put(type + "." + stepId + ".routetemplate", routeTemplate);
        properties.put(type + "." + stepId + ".routetemplate.id",  routeId);

    }

    private void createCustomStep(List<String> optionProperties, String[] links, String type, String stepXPath, int stepIndex,String flowId, String stepId) throws Exception {

        Node node = IntegrationUtil.getNode(conf,"/dil/" + stepXPath + "blocks");

        if(node != null && node.hasChildNodes() && node instanceof Element nodeElement) {

                NodeList blocks = nodeElement.getElementsByTagName("block");

                for (int i = 0; i < blocks.getLength(); i++) {

                    Node block = blocks.item(i);

                    if(block instanceof Element blockElement) {

                        blockType = getBlockType(blockElement);
                        blockUri = getBlockUri(blockElement);

                        if(blockUri.contains(":")) {
                            String[] splittedBlockUri = blockUri.split(":");
                            String componentName = splittedBlockUri[0];
                            templateId = componentName + "-" + type;
                        }

                    }

                    createStepByType(type, stepId, flowId, stepXPath, stepIndex,optionProperties, links);

                }

            }


    }

    private String getBlockType(Element blockElement){
        Node nodeBlockType = blockElement.getElementsByTagName("type").item(0);
        if(nodeBlockType!=null){
            return nodeBlockType.getTextContent();
        }else{
            return "routeTemplate";
        }
    }

    private String getBlockUri(Element blockElement){

        String stringBlockUri = "";
        Node nodeBlockUri = blockElement.getElementsByTagName("uri").item(0);

        if(nodeBlockUri!=null){
            stringBlockUri = nodeBlockUri.getTextContent();
            baseUri = stringBlockUri;
        }

        return stringBlockUri;

    }

    public void createStepByType(String type, String stepId, String flowId, String stepXPath, int stepIndex, List<String> optionProperties, String[] links) throws Exception {
        if(blockType.equalsIgnoreCase("routeTemplate")){
            defineRouteTemplate(templateId, type, stepId);
            createStep(optionProperties, links, stepXPath, stepIndex, type, flowId, stepId);
        }else if(blockType.equalsIgnoreCase("message") && blockUri.contains(":")){
            String[] splittedBlockUri = blockUri.split(":");
            baseUri = splittedBlockUri[0] + ":message:" + splittedBlockUri[1];
            createStep(optionProperties, links, stepXPath, stepIndex, type, flowId, stepId);
        }else if(blockType.equalsIgnoreCase("route")){

            routeId = baseUri;
            Node routeNode = IntegrationUtil.getNode(conf,"/dil/core/routes/route[@id='" + routeId + "']");
            String route = DocConverter.convertNodeToString(routeNode);

            properties.put(type + "." + stepId + ".route.id", routeId);
            properties.put(type + "." + stepId + ".route", route);

        }else if(blockType.equalsIgnoreCase("routeConfiguration")){

            String routeConfigurationId = baseUri;
            String timestamp = getTimestamp();
            Node routeNode = IntegrationUtil.getNode(conf,"/dil/core/routeConfigurations/routeConfiguration[@id='" + routeConfigurationId + "']");
            String routeConfiguration = DocConverter.convertNodeToString(routeNode);

            String updatedRouteConfigurationId = baseUri + "_" + timestamp;
            String updatedRouteConfiguration = StringUtils.replace(routeConfiguration,routeConfigurationId,updatedRouteConfigurationId);

            if (updatedRouteConfiguration.contains("<dataFormats>")){
                updatedRouteConfiguration = updatedRouteConfiguration.replaceAll("<dataFormats>((.\\n)*)</dataFormats>", "");
            }

            properties.put(type + "." + stepId + ".routeconfiguration.id", updatedRouteConfiguration);
            properties.put(type + "." + stepId + ".routeconfiguration", updatedRouteConfiguration);
        }
    }


    private void createTemplateId(String uri,String type){

        if(uri==null || uri.isEmpty()){
            templateId = "link-" + type;
        }else{
            String templateName = scheme + "-" + type;

            if(templateExists(templateName)){
                templateId = templateName;
            }else if(uri.startsWith("block")){
                String componentName = path;
                componentName = componentName.toLowerCase();
                templateId = componentName + "-" + type;
            }else{
                templateId = "generic-" + type;
            }

        }

    }

    private boolean templateExists(String templateName) {
        String fullTemplateName = "kamelets/" + templateName + ".kamelet.yaml";
        return CustomKameletCatalog.getNames().contains(fullTemplateName);
    }


    private void createTemplatedRoutes(){
        templatedRoutes = templateDoc.createElementNS("http://camel.apache.org/schema/spring", "templatedRoutes");
        templateDoc.appendChild(templatedRoutes);
    }

    private void createTemplatedRoute(List<String> optionProperties, String[] links, String stepXPath, int stepIndex, String type, String flowId){

        templatedRoute = templateDoc.createElementNS("http://camel.apache.org/schema/spring", "templatedRoute");
        templatedRoute.setAttribute("routeTemplateRef", templateId);
        templatedRoute.setAttribute("group", flowId);

        templatedRoute.setAttribute("routeId", routeId);
        templatedRoutes.appendChild(templatedRoute);

        Element param = createParameter(templateDoc,"routeId", routeId);
        templatedRoute.appendChild(param);

        try {
            createUriValues();
        } catch (XPathExpressionException | TransformerException e) {
            throw new RuntimeException(e);
        }

        createTemplateParameters();

        createOptionParameters(optionProperties);

        createTransport(flowId);

        createLinks(links, stepXPath, stepIndex, type, flowId);

        createConfigurationId();

    }

    /**
     * Processes option parameters from either optionProperties list or options string
     */
    private void createOptionParameters(List<String> optionProperties) {
        if (optionProperties != null && !optionProperties.isEmpty()) {
            processOptionProperties(optionProperties);
        } else if (options != null && !options.isEmpty()) {
            processOptionsString(options);
        }
    }

    private void processOptionProperties(List<String> optionProperties) {
        for (String optionProperty : optionProperties) {
            String name = optionProperty.substring(optionProperty.lastIndexOf('/') + 1);
            String value = conf.getProperty(optionProperty).toString();
            Element parameter = createParameter(templateDoc, name, value);
            templatedRoute.appendChild(parameter);
        }
    }

    private void processOptionsString(String options) {
        if (options.contains("&")) {
            String[] optionsList = options.split("&");
            for (String option : optionsList) {
                if (option.contains("=")) {
                    createOption(option);
                }
            }
        } else {
            if (options.contains("=")) {
                createOption(options);
            }
        }
    }

    private void createOption(String option){
        int eqIndex = option.indexOf('=');
        if (eqIndex != -1) {
            String name = option.substring(0, eqIndex);
            String value = option.substring(eqIndex + 1);
            templatedRoute.appendChild(createParameter(templateDoc, name, value));
        }
    }

    private void createTemplateParameters(){

        createTemplateParameter("uri", uri);

        createTemplateParameter("path", path);

        createTemplateParameter("scheme", scheme);

        createTemplateParameter("options", options);
    }

    private void createTemplateParameter(String name, String value){
        Element param = createParameter(templateDoc,name,value);
        templatedRoute.appendChild(param);
    }


    private Element createParameter(Document doc, String name, String value){

        Element param = doc.createElementNS("http://camel.apache.org/schema/spring","parameter");
        param.setAttribute("name", name);
        param.setAttribute("value", value);

        return param;

    }

    private void createUriValues() throws XPathExpressionException, TransformerException {

        if(scheme.equals("block") || scheme.equals("component")){
            uri = path;
        }else{
            uri = baseUri;
        }

        if(options!=null && !options.isEmpty() && !baseUri.contains("?")) {
            uri = uri + "?" + options;
        }

        createCoreMessageComponents();

    }

    private void createTransport(String flowId){
        transport = Objects.toString(conf.getProperty("integration/flows/flow[id='" + flowId + "']/transport"), null);

        if(transport==null){
            transport = SYNC_TRANSPORT;
        }
    }

    private void createLinks(String[] links, String stepXPath, int stepIndex, String type, String flowId){

        //set default links when not configured.
        createDefaultLinks(stepXPath, stepIndex, flowId);

        if(links.length > 0){
            //overwrite default links with configured links.
            createCustomLinks(links, stepXPath, type);
        }

    }

    private void createDefaultLinks(String stepXPath, int stepIndex, String flowId){

        int previousStepIndex = stepIndex - 1;
        int nextStepIndex = stepIndex + 1;

        String baseXPath = stepXPath.substring(0, stepXPath.lastIndexOf("/step["));
        String previousStepXPath = "(" + baseXPath + "/step[" + previousStepIndex + "]/id)[1]";
        String nextStepXPath = "(" + baseXPath + "/step[" + nextStepIndex + "]/id)[1]";
        String currentStepXPath = "(" + stepXPath + "id)[1]";

        String previousStepId = Objects.toString(conf.getProperty(previousStepXPath), ZERO);
        String currentStepId = Objects.toString(conf.getProperty(currentStepXPath), ZERO);
        String nextStepId = Objects.toString(conf.getProperty(nextStepXPath), ZERO);

        if (!ZERO.equals(previousStepId)) {
            templatedRoute.appendChild(createParameter(templateDoc, "in",transport + ":" + flowId + "-" + currentStepId));
        }

        if (!ZERO.equals(nextStepId)){
            templatedRoute.appendChild(createParameter(templateDoc, "out", transport + ":" + flowId + "-" + nextStepId));
        }

    }

    private void createCustomLinks(String[] links, String stepXPath, String type){

        int index = 1;

        for(int i = 0; i < links.length; i++) {

            String linkXPath = stepXPath + "links/link[" + index + "]/";

            createCustomLink(linkXPath, type);

            index++;

        }

        if(outList!=null){
            Element param = createParameter(templateDoc,"out_list",outList);
            templatedRoute.appendChild(param);
        }

        if(outRulesList!=null){
            Element param = createParameter(templateDoc,"out_rules_list",outRulesList);
            templatedRoute.appendChild(param);
        }

    }

    public void createCustomLink(String linkXPath, String type) {
        // get values from configuration
        String bound = Objects.toString(conf.getProperty(linkXPath + "bound"), null);
        String linkTransport = createLinkTransport(linkXPath);
        String pattern = Objects.toString(conf.getProperty(linkXPath + "pattern"), null);
        String id = Objects.toString(conf.getProperty(linkXPath + "id"), null);
        String rule = Objects.toString(conf.getProperty(linkXPath + "rule"), null);
        String expression = Objects.toString(conf.getProperty(linkXPath + "expression"), null);

        // Assuming 'options' is an instance variable. If not, it should be passed or returned.
        options = createLinkOptions(linkXPath, bound, linkTransport, pattern);
        String endpoint = createLinkEndpoint(linkTransport, id);

        // Set values based on retrieved configuration and type
        if (expression != null) {
            Element param = createParameter(templateDoc, "expression", expression);
            templatedRoute.appendChild(param);
        }

        if (type.equals("router")) {
            handleRouterLinkType(bound, rule, expression, endpoint);
        } else {
            handleNonRouterLinkType(bound, endpoint);
        }
    }

    private void handleRouterLinkType(String bound, String rule, String expression, String endpoint) {
        Element param;
        if (rule != null) {
            param = createParameter(templateDoc, bound + "_rule", endpoint);
        } else {
            param = createParameter(templateDoc, bound, endpoint);
        }
        templatedRoute.appendChild(param);

        if (bound != null && bound.equalsIgnoreCase("out")) {
            createLinkLists(rule, expression, endpoint);
            if (rule == null) {
                param = createParameter(templateDoc, bound + "_default", endpoint);
                templatedRoute.appendChild(param);
            }
        }
    }

    private void handleNonRouterLinkType(String bound, String endpoint) {
        NodeList oldParameters = templatedRoute.getElementsByTagName("parameter");
        boolean parameterUpdated = false;

        for (Node oldParameter : iterable(oldParameters)) {
            Node name = oldParameter.getAttributes().getNamedItem("name");

            if (name != null && name.getNodeValue().equals(bound)) { // Added null check for 'name' attribute
                parameterUpdated = true;
                Element param = createParameter(templateDoc, bound, endpoint);
                templatedRoute.replaceChild(param, oldParameter);
                break; // No need to continue loop once updated
            }
        }

        if (!parameterUpdated) {
            Element param = createParameter(templateDoc, bound, endpoint);
            templatedRoute.appendChild(param);
        }
    }

    public String createLinkTransport(String xpath){
        return Objects.toString(conf.getProperty(xpath + "transport"), SYNC_TRANSPORT);
    }

    public String createLinkEndpoint(String linkTransport, String id){

        String endpoint;

        if (options == null || options.isEmpty()) {
            endpoint = linkTransport + ":" + id;
        } else {
            endpoint = linkTransport + ":" + id + "?" + options;
        }

        return endpoint;

    }

    public String createLinkOptions(String xpath, String bound, String transport, String pattern){

        options = Objects.toString(conf.getProperty(xpath + "options"), null);

        if(bound!= null && transport!=null && pattern!=null) {

            if (pattern.equalsIgnoreCase("inout") || pattern.equalsIgnoreCase("requestreply")) {
                if (options == null) {
                    options = "exchangePattern=InOut";
                } else {
                    options = options + "&exchangePattern=InOut";
                }
            } else if (pattern.equalsIgnoreCase("inonly") || pattern.equalsIgnoreCase("oneway") || pattern.equalsIgnoreCase("event") || pattern.equalsIgnoreCase("fireandforget")) {
                if (options == null) {
                    options = "exchangePattern=InOnly";
                } else {
                    options = options + "&exchangePattern=InOnly";
                }
            }

        }

        return options;

    }

    private void createLinkLists(String rule, String expression, String endpoint){

        if (rule != null && expression != null) {

            String newRule = rule + "#;#" + expression + "#;#" + endpoint;
            if (outRulesList == null) {
                outRulesList = newRule;
            } else {
                outRulesList = outRulesList + "#|#" + newRule;
            }

        } else {
            if (outList == null) {
                outList = endpoint;
            } else {
                outList = outList + "," + endpoint;
            }
        }

    }

    private void defineRouteTemplate(String templateId, String type, String stepId) throws Exception {

        Node node = IntegrationUtil.getNode(conf,"/dil/core/routeTemplates/routeTemplate[@id='" + templateId + "']");

        String routeTemplateAsString = DocConverter.convertNodeToString(node);

        properties.put(type + "." + stepId + ".routetemplatedefinition.id", templateId);
        properties.put(type + "." + stepId + ".routetemplatedefinition", routeTemplateAsString);

    }

    private void createConfigurationId(){
        String routeConfigurationID = Objects.toString(conf.getProperty("integration/flows/flow/steps/step[type='error']/routeconfiguration_id"), null);

        if(routeConfigurationID!=null){
            Element param = createParameter(templateDoc, "routeconfigurationid", routeConfigurationID);
            templatedRoute.appendChild(param);
        }
    }

    private void createCoreMessageComponents() throws XPathExpressionException {
        if (!path.startsWith("message:")) {
            return;
        }

        String name = StringUtils.substringAfter(path, "message:");

        if (scheme.equalsIgnoreCase("setBody") || scheme.equalsIgnoreCase("setMessage")) {
            setCoreMessageBody(name);
        }

        if (scheme.equalsIgnoreCase("setHeaders") || scheme.equalsIgnoreCase("setMessage")) {
            setCoreMessageHeaders(name);
        }
    }

    private void setCoreMessageBody(String messageName) throws XPathExpressionException {
        String resourceAsString = getBodyResource(messageName);

        String language = Objects.toString(conf.getProperty("core/messages/message[name='" + messageName + "']/body/@language"), null);
        if (language == null) {
            language = Objects.toString(conf.getProperty("core/messages/message[id='" + messageName + "']/body/@language"), "constant");
        }

        // Assuming 'parameter', 'templateDoc', and 'templatedRoute' are instance variables
        Element parameter = createParameter(templateDoc, "language", language);
        templatedRoute.appendChild(parameter);

        parameter = createParameter(templateDoc, "path", resourceAsString);
        templatedRoute.appendChild(parameter);

        // Assuming 'path' is an instance variable being updated
        path = resourceAsString;
    }

    private String getBodyResource(String messageName) throws XPathExpressionException {
        String resourceAsString = Objects.toString(conf.getProperty("core/messages/message[name='" + messageName + "']/body"), null);
        if (resourceAsString == null) {
            resourceAsString = Objects.toString(conf.getProperty("core/messages/message[id='" + messageName + "']/body"), null);
        }

        if (resourceAsString == null) {
            Node node = IntegrationUtil.getNode(conf, "/dil/core/messages/message[name='" + messageName + "']/body/*");
            if (node == null) {
                node = IntegrationUtil.getNode(conf, "/dil/core/messages/message[id='" + messageName + "']/body/*");
            }
            resourceAsString = DocConverter.convertNodeToString(node);
        }
        return resourceAsString;
    }


    private void setCoreMessageHeaders(String messageName) {
        // Assuming 'conf' is an instance variable and getDocument() exists
        Node node = getHeadersNode(conf.getDocument(), messageName, "name");

        // Backup when name is not found
        if (node == null) {
            node = getHeadersNode(conf.getDocument(), messageName, "id");
        }

        if (node != null) {
            String headerKeysAsString = DocConverter.convertNodeToString(node);
            // Assuming 'parameter', 'templateDoc', and 'templatedRoute' are instance variables
            Element parameter = createParameter(templateDoc, "headers", headerKeysAsString);
            templatedRoute.appendChild(parameter);
        }
    }

    private String getTimestamp(){
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        long unixTimestamp = timestamp.getTime();
        return Long.toString(unixTimestamp);
    }

    public static Node getHeadersNode(Document doc, String value, String type) {
        NodeList messages = doc.getElementsByTagName("message");

        for (int i = 0; i < messages.getLength(); i++) {
            Element message = (Element) messages.item(i);
            NodeList nodeList = message.getElementsByTagName(type);

            if (nodeList.getLength() > 0) {
                String typeValue = nodeList.item(0).getTextContent().trim();
                if (value.equals(typeValue)) {
                    NodeList headersNodes = message.getElementsByTagName("headers");
                    if (headersNodes.getLength() > 0) {
                        return headersNodes.item(0);
                    }
                }
            }
        }
        return null;
    }

}