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
import javax.xml.transform.TransformerException;
import javax.xml.xpath.*;
import java.sql.Timestamp;
import java.util.*;

import static org.assimbly.util.IntegrationUtil.*;

public class RouteTemplate {

    protected Logger log = LoggerFactory.getLogger(getClass());
    private TreeMap<String, String> properties;
    private XMLConfiguration conf;
    private String uri;
    private Document contentRouteDoc;
    private Document templateDoc;
    private Element templatedRoutes;
    private Element templatedRoute;
    private String routeId;
    private String templateId;
    private String path;
    private String scheme;
    private String options;
    private String blockType;
    private Element parameter;
    private String transport;
    private String blockUri;
    private String baseUri;
    private String outList;
    private String outRulesList;
    private String updatedRouteConfigurationId;
    //private KameletsCatalog kameletCatalog = new KameletsCatalog();

    public RouteTemplate(TreeMap<String, String> properties, XMLConfiguration conf) {
        this.properties = properties;
        this.conf = conf;
    }

    public TreeMap<String, String> setRouteTemplate(String type, String flowId, String stepId, List<String> optionProperties, String[] links, String stepXPath, String baseUri, String options) throws Exception {

        this.baseUri = baseUri;

        templateDoc = new DocumentImpl();

        routeId = flowId + "-" + stepId;
        this.options = options;

        createTemplatedRoutes();

        createTemplateId(baseUri, type);

        if(baseUri.equalsIgnoreCase("content") && type.equalsIgnoreCase("router") ) {
            contentRouteDoc = new DocumentImpl();
            createContentRouter(links, stepXPath, type, flowId, stepId);
        }else if(baseUri.startsWith("block")){
            createCustomStep(optionProperties, links, type, stepXPath, flowId, stepId);
        }else{
            createStep(optionProperties, links, stepXPath, type, flowId, stepId);
        }

        return properties;
    }


    private void createContentRouter(String[] links, String stepXPath, String type, String flowId, String stepId)  throws Exception {

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

        for(String link : links) {

            String linkXPath = stepXPath + "links/link[" + index + "]/";

            //get values from configuration
            String bound = Objects.toString(conf.getProperty(linkXPath + "bound"), null);
            String transport = createLinkTransport(linkXPath);
            String pattern = Objects.toString(conf.getProperty(linkXPath + "pattern"), null);
            String id = Objects.toString(conf.getProperty(linkXPath + "id"), null);
            options = createLinkOptions(linkXPath, bound, transport, pattern);
            String endpoint = createLinkEndpoint(linkXPath, transport, id);

            if(bound!=null & bound.equalsIgnoreCase("in") ){
                fromEndpoint.setAttribute("uri", endpoint);
            }

            index++;

        }

        return fromEndpoint;
    }


    public Element createWhens(String[] links, String stepXPath, Element choice) {

        int index = 1;

        for(String link : links) {

            Element when = contentRouteDoc.createElement("when");

            String linkXPath = stepXPath + "links/link[" + index + "]/";

            //get values from configuration
            String bound = Objects.toString(conf.getProperty(linkXPath + "bound"), null);
            String transport = createLinkTransport(linkXPath);
            String pattern = Objects.toString(conf.getProperty(linkXPath + "pattern"), null);
            String id = Objects.toString(conf.getProperty(linkXPath + "id"), null);
            String rule = Objects.toString(conf.getProperty(linkXPath + "rule"), null);
            String expression = Objects.toString(conf.getProperty(linkXPath + "expression"), null);
            options = createLinkOptions(linkXPath, bound, transport, pattern);
            String endpoint = createLinkEndpoint(linkXPath, transport, id);

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

        int index = 1;

        for(String link : links) {

            Element otherwise = contentRouteDoc.createElement("otherwise");

            String linkXPath = stepXPath + "links/link[" + index + "]/";

            //get values from configuration
            String bound = Objects.toString(conf.getProperty(linkXPath + "bound"), null);
            String transport = createLinkTransport(linkXPath);
            String pattern = Objects.toString(conf.getProperty(linkXPath + "pattern"), null);
            String id = Objects.toString(conf.getProperty(linkXPath + "id"), null);
            String rule = Objects.toString(conf.getProperty(linkXPath + "rule"), null);
            String expression = Objects.toString(conf.getProperty(linkXPath + "expression"), null);
            options = createLinkOptions(linkXPath, bound, transport, pattern);
            String endpoint = createLinkEndpoint(linkXPath, transport, id);

            if(bound!=null && bound.equalsIgnoreCase("out") && rule == null && expression == null) {

                Element toEndpoint = contentRouteDoc.createElement("to");
                toEndpoint.setAttribute("uri",endpoint);
                otherwise.appendChild(toEndpoint);

                choice.appendChild(otherwise);

            }

            index++;

        }

        return choice;
    }

    private void createStep(List<String> optionProperties, String[] links, String stepXPath, String type, String flowId, String stepId) throws Exception {

        createTemplatedRoute(optionProperties, links, stepXPath, type, flowId);

        String routeTemplate = DocConverter.convertDocToString(templateDoc);

        properties.put(type + "." + stepId + ".routetemplate", routeTemplate);
        properties.put(type + "." + stepId + ".routetemplate.id",  routeId);

    }

    private void createCustomStep(List<String> optionProperties, String[] links, String type, String stepXPath, String flowId, String stepId) throws Exception {

        Node node = IntegrationUtil.getNode(conf,"/dil/" + stepXPath + "blocks");

        if(node != null && node.hasChildNodes()){

            if(node instanceof Element) {
                Element nodeElement = (Element) node;

                NodeList blocks = nodeElement.getElementsByTagName("block");

                for (int i = 0; i < blocks.getLength(); i++) {
                    Node block = blocks.item(i);

                    if(block instanceof Element) {
                        Element blockElement = (Element)block;

                        Node nodeBlockType = blockElement.getElementsByTagName("type").item(0);
                        if(nodeBlockType!=null){
                            blockType = nodeBlockType.getTextContent();
                        }else{
                            blockType = "routeTemplate";
                        }

                        Node nodeBlockUri = blockElement.getElementsByTagName("uri").item(0);
                        if(nodeBlockUri!=null){
                            blockUri = nodeBlockUri.getTextContent();
                            baseUri = blockUri;
                        }else{
                            blockUri = "";
                        }

                        if(blockUri.contains(":")) {
                            String[] splittedBlockUri = blockUri.split(":");
                            String componentName = splittedBlockUri[0];
                            templateId = componentName + "-" + type;
                        }

                    }

                    if(blockType.equalsIgnoreCase("routeTemplate")){
                        defineRouteTemplate(templateId, type, stepId);
                        createStep(optionProperties, links, stepXPath, type, flowId, stepId);
                    }else if(blockType.equalsIgnoreCase("message") && blockUri.contains(":")){
                        String[] splittedBlockUri = blockUri.split(":");
                        baseUri = splittedBlockUri[0] + ":message:" + splittedBlockUri[1];
                        createStep(optionProperties, links, stepXPath, type, flowId, stepId);
                    }else if(blockType.equalsIgnoreCase("route")){

                        String routeId = baseUri;
                        Node routeNode = IntegrationUtil.getNode(conf,"/dil/core/routes/route[@id='" + routeId + "']");
                        String route = DocConverter.convertNodeToString(routeNode);

                        properties.put(type + "." + stepId + ".route.id", routeId);
                        properties.put(type + "." + stepId + ".route", route);

                    }else if(blockType.equalsIgnoreCase("routeConfiguration")){

                        String routeConfigurationId = baseUri;
                        String timestamp = getTimestamp();
                        Node routeNode = IntegrationUtil.getNode(conf,"/dil/core/routeConfigurations/routeConfiguration[@id='" + routeConfigurationId + "']");
                        String routeConfiguration = DocConverter.convertNodeToString(routeNode);


                        updatedRouteConfigurationId = baseUri + "_" + timestamp;
                        String updatedRouteConfiguration = StringUtils.replace(routeConfiguration,routeConfigurationId,updatedRouteConfigurationId);

                        if (updatedRouteConfiguration.contains("<dataFormats>")){
                            updatedRouteConfiguration = updatedRouteConfiguration.replaceAll("<dataFormats>((.|\\n)*)<\\/dataFormats>", "");
                        }

                        properties.put(type + "." + stepId + ".routeconfiguration.id", updatedRouteConfiguration);
                        properties.put(type + "." + stepId + ".routeconfiguration", updatedRouteConfiguration);
                    }

                }

            }
        }else{
            //create default log block
            log.info("Creating default log block");
        }


    }

    private void createTemplateId(String uri,String type){

        if(uri==null || uri.isEmpty()){
            templateId = "link-" + type;
        }else{
            String[] uriSplitted = uri.split(":");
            String templateName = uriSplitted[0] + "-" + type;

            if(templateExists(templateName)){
                System.out.println("TemplateId exist name=" +templateName);
                templateId = templateName;
            }else if(uri.startsWith("block")){
                System.out.println("TemplateId is block=" +templateName);
                String componentName = uriSplitted[1];
                componentName = componentName.toLowerCase();
                templateId = componentName + "-" + type;
            }else{
                System.out.println("TemplateId is generic=" +templateName);
                templateId = "generic-" + type;
            }

        }

    }

    private boolean templateExists(String templateName) {
        String fullTemplateName = "kamelets/" + templateName + ".kamelet.yaml";
        return CustomKameletCatalog.names.contains(fullTemplateName);
    }


    private void createTemplatedRoutes(){
        templatedRoutes = templateDoc.createElementNS("http://camel.apache.org/schema/spring", "templatedRoutes");
        templateDoc.appendChild(templatedRoutes);
    }

    private void createTemplatedRoute(List<String> optionProperties, String[] links, String stepXPath, String type, String flowId){

        templatedRoute = templateDoc.createElementNS("http://camel.apache.org/schema/spring", "templatedRoute");
        templatedRoute.setAttribute("routeTemplateRef", templateId);

        templatedRoute.setAttribute("routeId", routeId);
        templatedRoutes.appendChild(templatedRoute);

        Element parameter = createParameter(templateDoc,"routeId",routeId);
        templatedRoute.appendChild(parameter);

        try {
            createUriValues();
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        } catch (TransformerException e) {
            throw new RuntimeException(e);
        }

        createTemplateParameters();

        if(optionProperties.isEmpty()){
            for (String optionProperty : optionProperties) {
                String name = optionProperty.split("options.")[1];
                String value = conf.getProperty(optionProperty).toString();

                parameter = createParameter(templateDoc,name,value);
                templatedRoute.appendChild(parameter);

            }

        }else if(options!= null && !options.isEmpty()){
            if(options.contains("&")){
                String[] optionsList = options.split("&");
                for(String option: optionsList){
                    if(option.contains("=")){
                        String name = option.split("=",2)[0];
                        String value = option.split("=",2)[1];

                        parameter = createParameter(templateDoc,name,value);
                        templatedRoute.appendChild(parameter);
                    }
                }
            }else {
                if(options.contains("=")){
                    String name = options.split("=",2)[0];
                    String value = options.split("=",2)[1];

                    parameter = createParameter(templateDoc,name,value);
                    templatedRoute.appendChild(parameter);
                }
            }
        }


        createTransport(flowId);

        createLinks(links, stepXPath, type, flowId);

        createConfigurationId();

    }

    private void createTemplateParameters(){

        createTemplateParameter("uri",uri);

        createTemplateParameter("path",path);

        createTemplateParameter("scheme",scheme);

        createTemplateParameter("options",options);
    }

    private void createTemplateParameter(String name, String value){
        Element parameter = createParameter(templateDoc,name,value);
        templatedRoute.appendChild(parameter);
    }


    private Element createParameter(Document doc, String name, String value){

        Element parameter = doc.createElementNS("http://camel.apache.org/schema/spring","parameter");
        parameter.setAttribute("name", name);
        parameter.setAttribute("value", value);

        return parameter;

    }

    private void createUriValues() throws XPathExpressionException, TransformerException {

        if(baseUri.startsWith("block:")){
            baseUri = StringUtils.substringAfter(baseUri,"block:");
            uri = baseUri;
        }else if(baseUri.startsWith("component:")){
            baseUri = StringUtils.substringAfter(baseUri,"component:");
            uri = baseUri;
        }
        else{
            uri = baseUri;
        }

        if(baseUri.contains(":")){
            scheme = baseUri.split(":",2)[0];
            scheme = scheme.toLowerCase();
        }else{
            scheme = baseUri.toLowerCase();
        }

        if(baseUri.contains(":")){
            if(baseUri.contains("?")){
                String pathAndOptions = baseUri.split(":",2)[1];
                path = pathAndOptions.split("\\?",2)[0];
                options = pathAndOptions.split("\\?",2)[1];
            }else{
                path = baseUri.split(":",2)[1];
            }
        }else{
            path = "";
        }

        if(options!=null && !options.isEmpty() && !baseUri.contains("?")) {
            uri = uri + "?" + options;
        }

        createCoreComponents();

    }

    private void createTransport(String flowId){
        transport = Objects.toString(conf.getProperty("integration/flows/flow[id='" + flowId + "']/transport"), null);

        if(transport==null){
            transport = "sync";
        }
    }

    private void createLinks(String[] links, String stepXPath, String type, String flowId){

        //set default links when not configured.
        createDefaultLinks(stepXPath, flowId);

        if(links.length > 0){
            //overwrite default links with configured links.
            createCustomLinks(links, stepXPath, type);
        }

    }

    private void createDefaultLinks(String stepXPath, String flowId){

        String stepIndex = StringUtils.substringBetween(stepXPath,"step[","]");
        String value;

        if(StringUtils.isNumeric(stepIndex)) {

            Integer previousStepIndex = Integer.parseInt(stepIndex) - 1;
            Integer nextStepIndex = Integer.parseInt(stepIndex) + 1;

            String previousStepXPath = StringUtils.replace(stepXPath, "/step[" + stepIndex + "]", "/step[" + previousStepIndex + "]");

            String nextStepXPath = StringUtils.replace(stepXPath, "/step[" + stepIndex + "]", "/step[" + nextStepIndex + "]");

            String previousStepId = Objects.toString(conf.getProperty("(" + previousStepXPath + "id)[1]"), "0");
            String currentStepId = Objects.toString(conf.getProperty("(" + stepXPath + "id)[1]"), "0");
            String nextStepId = Objects.toString(conf.getProperty("(" + nextStepXPath + "id)[1]"), "0");

            if (!nextStepId.equals("0")){
                value = transport + ":" + flowId + "-" + nextStepId;

                parameter = createParameter(templateDoc, "out", value);
                templatedRoute.appendChild(parameter);
            }

            if (!previousStepId.equals("0")) {
                value = transport + ":" + flowId + "-" + currentStepId;

                parameter = createParameter(templateDoc, "in", value);
                templatedRoute.appendChild(parameter);
            }
        }
    }

    private void createCustomLinks(String[] links, String stepXPath, String type){

        int index = 1;

        for(String link : links) {

            String linkXPath = stepXPath + "links/link[" + index + "]/";

            createCustomLink(linkXPath, type);

            index++;

        }

        if(outList!=null){
            Element parameter = createParameter(templateDoc,"out_list",outList);
            templatedRoute.appendChild(parameter);
        }

        if(outRulesList!=null){
            Element parameter = createParameter(templateDoc,"out_rules_list",outRulesList);
            templatedRoute.appendChild(parameter);
        }

    }

    public void createCustomLink(String linkXPath, String type) {

        //get values from configuration
        String bound = Objects.toString(conf.getProperty(linkXPath + "bound"), null);
        String transport = createLinkTransport(linkXPath);
        String pattern = Objects.toString(conf.getProperty(linkXPath + "pattern"), null);
        String id = Objects.toString(conf.getProperty(linkXPath + "id"), null);
        String rule = Objects.toString(conf.getProperty(linkXPath + "rule"), null);
        String expression = Objects.toString(conf.getProperty(linkXPath + "expression"), null);
        options = createLinkOptions(linkXPath, bound, transport, pattern);
        String endpoint = createLinkEndpoint(linkXPath, transport, id);

        //set values
        if (expression != null) {
            Element parameter = createParameter(templateDoc, "expression", expression);
            templatedRoute.appendChild(parameter);
        }

        if (type.equals("router")) {

            if (rule != null) {
                parameter = createParameter(templateDoc, bound + "_rule", endpoint);
                templatedRoute.appendChild(parameter);
            } else {
                parameter = createParameter(templateDoc, bound , endpoint);
                templatedRoute.appendChild(parameter);
            }

            if (bound != null && bound.equalsIgnoreCase("out")) {
                createLinkLists(rule, expression, endpoint);
                if (rule != null) {
                    parameter = createParameter(templateDoc, bound + "_default", endpoint);
                    templatedRoute.appendChild(parameter);
                }
            }

        } else {
            NodeList oldParameters = templatedRoute.getElementsByTagName("parameter");

            Boolean parameterUpdated = false;
            for (Node oldParameter : iterable(oldParameters)) {

                Node name = oldParameter.getAttributes().getNamedItem("name");

                if (name.getNodeValue().equals(bound)) {
                    parameterUpdated = true;
                    parameter = createParameter(templateDoc, bound, endpoint);
                    templatedRoute.replaceChild(parameter, oldParameter);
                }

            }

            if(!parameterUpdated){
                parameter = createParameter(templateDoc, bound, endpoint);
                templatedRoute.appendChild(parameter);
            }

        }
    }

    public String createLinkTransport(String xpath){

        String transport = Objects.toString(conf.getProperty(xpath + "transport"), "sync");

        return transport;

    }

    public String createLinkEndpoint(String xpath, String transport, String id){

        String endpoint;

        //Objects.toString(conf.getProperty(xpath + "options"), null);

        if (options == null || options.isEmpty()) {
            endpoint = transport + ":" + id;
        } else {
            endpoint = transport + ":" + id + "?" + options;
        }

        return endpoint;

    }

    public String createLinkOptions(String xpath, String bound, String transport, String pattern){

        options = Objects.toString(conf.getProperty(xpath + "options"), null);

        if(bound!= null && transport!=null && pattern!=null) {
            //if (bound.equalsIgnoreCase("in")){
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

            //adjust the templateid to call the correct template
            if (templateId.startsWith("split")) {
                templateId = "split-" + rule + "-router";
                templatedRoute.removeAttribute("routeTemplateRef");
                templatedRoute.setAttribute("routeTemplateRef", templateId);
            } else if (templateId.startsWith("filter")) {
                templateId = "filter-" + rule + "-router";
                templatedRoute.removeAttribute("routeTemplateRef");
                templatedRoute.setAttribute("routeTemplateRef", templateId);
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
        String routeConfiguratinID = Objects.toString(conf.getProperty("integration/flows/flow/steps/step[type='error']/routeconfiguration_id"), null);

        if(routeConfiguratinID!=null){
            Element parameter = createParameter(templateDoc, "routeconfigurationid", updatedRouteConfigurationId);
            templatedRoute.appendChild(parameter);
        }
    }

    private void createCoreComponents() throws XPathExpressionException, TransformerException {

        if(path.startsWith("message:")) {
            String name = StringUtils.substringAfter(path, "message:");

            if (scheme.equalsIgnoreCase("setBody") || scheme.equalsIgnoreCase("setMessage")) {

                String resourceAsString = Objects.toString(conf.getProperty("core/messages/message[name='" + name + "']/body"), null);
                if(resourceAsString == null) {
                    resourceAsString = Objects.toString(conf.getProperty("core/messages/message[id='" + name + "']/body"), null);
                }

                if(resourceAsString == null){

                    Node node = IntegrationUtil.getNode(conf,"/dil/core/messages/message[name='" + name + "']/body/*");
                    if (node == null) {
                        node = IntegrationUtil.getNode(conf,"/dil/core/messages/message[id='" + name + "']/body/*");
                    }
                    resourceAsString = DocConverter.convertNodeToString(node);

                }

                String language = Objects.toString(conf.getProperty("core/messages/message[name='" + name + "']/body/@language"), null);
                if(language == null) {
                    language = Objects.toString(conf.getProperty("core/messages/message[id='" + name + "']/body/@language"), "constant");
                }

                parameter = createParameter(templateDoc, "language", language);
                templatedRoute.appendChild(parameter);

                parameter = createParameter(templateDoc, "path", resourceAsString);
                templatedRoute.appendChild(parameter);
                path = resourceAsString;
            }

            if (scheme.equalsIgnoreCase("setHeaders") || scheme.equalsIgnoreCase("setMessage")) {

                Node node = IntegrationUtil.getNode(conf,"/dil/core/messages/message[name='" + name + "']/headers");
                if (node == null) {
                    node = IntegrationUtil.getNode(conf,"/dil/core/messages/message[id='" + name + "']/headers");
                }

                if (node != null) {
                    String headerKeysAsString = DocConverter.convertNodeToString(node);

                    parameter = createParameter(templateDoc, "headers", headerKeysAsString);
                    templatedRoute.appendChild(parameter);
                }

            }
        }
    }

    private String getTimestamp(){
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        long unix_timestamp = timestamp.getTime();
        return Long.toString(unix_timestamp);
    }

}