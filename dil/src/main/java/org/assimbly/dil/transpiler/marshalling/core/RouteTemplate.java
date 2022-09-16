package org.assimbly.dil.transpiler.marshalling.core;

import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.assimbly.docconverter.DocConverter;
import org.assimbly.util.DependencyUtil;
import org.assimbly.util.IntegrationUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.*;
import java.util.*;

import static org.assimbly.util.IntegrationUtil.*;

public class RouteTemplate {

    private TreeMap<String, String> properties;
    private XMLConfiguration conf;
    private String uri;
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
    private boolean predefinedStep;
    private String blockUri;
    private String baseUri;

    public RouteTemplate(TreeMap<String, String> properties, XMLConfiguration conf) {
        this.properties = properties;
        this.conf = conf;
    }

    public TreeMap<String, String> setRouteTemplate(String type, String flowId, String stepId, List<String> optionProperties, String[] links, String stepXPath, String baseUri, String options) throws Exception {

        this.baseUri = baseUri;

        templateDoc = createNewDocument();

        routeId = flowId + "-" + stepId;
        this.options = options;

        createTemplatedRoutes();

        createTemplateId(baseUri, type, stepXPath);

        if(!predefinedStep && baseUri.startsWith("block")){
            createCustomStep(optionProperties, links, type, stepXPath, flowId, stepId);
        }else{
            createStep(optionProperties, links, stepXPath, type, flowId, stepId);
        }

        return properties;
    }

    private void createStep(List<String> optionProperties, String[] links, String stepXPath, String type, String flowId, String stepId){

        createTemplatedRoute(optionProperties, links, stepXPath, type, flowId);

        String routeTemplate = DocConverter.convertDocToString(templateDoc);

        properties.put(type + "." + stepId + ".routetemplate", routeTemplate);
        properties.put(type + "." + stepId + ".routetemplate.id",  routeId);

    }
    
    private void createCustomStep(List<String> optionProperties, String[] links, String type, String stepXPath, String flowId, String stepId) throws Exception {

        System.out.println("xpath: /dil/" + stepXPath + "blocks");

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
                            System.out.println("blockType" + blockType);
                            System.out.println("blockType name" +nodeBlockType.getNodeName());

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

                    System.out.println("Block Type=" + blockType);
                    System.out.println("Block Uri=" + blockUri);

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
                        Node routeNode = IntegrationUtil.getNode(conf,"/dil/core/routeConfigurations/routeConfiguration[@id='" + routeConfigurationId + "']");
                        String routeConfiguration = DocConverter.convertNodeToString(routeNode);

                        properties.put(type + "." + stepId + ".routeconfiguration.id", routeConfigurationId);
                        properties.put(type + "." + stepId + ".routeconfiguration", routeConfiguration);
                    }

                }

            }
        }else{
            //create default log block
            System.out.println("Creating default log block");
        }


    }
    
    
    
    private Document createNewDocument() throws ParserConfigurationException {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

        // root elements
        Document doc = docBuilder.newDocument();

        return doc;
    }

    private void createTemplateId(String uri,String type, String stepXPath){

        if(uri==null || uri.isEmpty()){
            templateId = "link-" + type;
        }else if(uri.startsWith("block")){
            String componentName = uri.split(":")[1];
            predefinedStep = DependencyUtil.PredefinedBlocks.hasBlock(componentName);
            templateId = componentName + "-" + type;

        }else{
            templateId = "generic-" + type;
        }

    }

    private void createTemplatedRoutes(){
        templatedRoutes = templateDoc.createElementNS("http://camel.apache.org/schema/spring", "templatedRoutes");
        templateDoc.appendChild(templatedRoutes);
    }

    private void createTemplatedRoute(List<String> optionProperties, String[] links, String stepXPath, String type, String flowId){

        templatedRoute = templateDoc.createElement("templatedRoute");
        templatedRoute.setAttribute("routeTemplateRef", templateId);
        templatedRoute.setAttribute("routeId", routeId);
        templatedRoutes.appendChild(templatedRoute);

        try {
            createUriValues();
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        } catch (TransformerException e) {
            throw new RuntimeException(e);
        }

        createTemplateParameters();

        for (String optionProperty : optionProperties) {
            String name = optionProperty.split("options.")[1];
            String value = conf.getProperty(optionProperty).toString();

            Element parameter = createParameter(templateDoc,name,value);
            templatedRoute.appendChild(parameter);

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

        Element parameter = doc.createElement("parameter");
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
        }else{
            scheme = baseUri;
        }

        if(baseUri.contains(":")){
            path = baseUri.split(":",2)[1];
        }else{
            path = "";
        }

        if(options!=null && !options.isEmpty()){
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

        createDefaultLinks(stepXPath, type, flowId);

        if(links.length > 0){
            createCustomLinks(links, stepXPath);
        }

    }

    private void createDefaultLinks(String stepXPath, String type, String flowId){

        String stepIndex = StringUtils.substringBetween(stepXPath,"step[","]");
        String value = "";

        if(StringUtils.isNumeric(stepIndex)) {

            if (type.equals("source") || type.equals("action")) {

                Integer nextStepIndex = Integer.parseInt(stepIndex) + 1;

                String nextStepXPath = StringUtils.replace(stepXPath, "/step[" + stepIndex + "]", "/step[" + nextStepIndex + "]");

                String nextStepId = Objects.toString(conf.getProperty("(" + nextStepXPath + "id)[1]"), "0");

                value = transport + ":" + flowId + "-" + nextStepId;

                parameter = createParameter(templateDoc, "out", value);
                templatedRoute.appendChild(parameter);

            }

            if (type.equals("sink") || type.equals("action")) {

                String currentStepId = Objects.toString(conf.getProperty("(" + stepXPath + "id)[1]"), "0");

                value = transport + ":" + flowId + "-" + currentStepId;

                parameter = createParameter(templateDoc, "in", value);
                templatedRoute.appendChild(parameter);

            }
        }
    }

    private void createCustomLinks(String[] links, String stepXPath){

        int index = 1;

        for(String link : links) {

            String linkXPath = stepXPath + "links/link[" + index + "]/";

            String value = "";

            String bound = Objects.toString(conf.getProperty(linkXPath + "bound"), null);
            String linktransport = Objects.toString(conf.getProperty(linkXPath + "transport"), null);
            String id = Objects.toString(conf.getProperty(linkXPath + "id"), null);
            options = Objects.toString(conf.getProperty(linkXPath + "options"), null);

            if(linktransport!=null){
                transport = linktransport;
            }

            if (options == null || options.isEmpty()) {
                value = transport + ":" + id;
            } else {
                value = transport + ":" + id + "?" + options;
            }

            NodeList oldParameters = templatedRoute.getElementsByTagName("parameter");

            for(Node oldParameter: iterable(oldParameters)){

                Node name = oldParameter.getAttributes().getNamedItem("name");
                if(name.getNodeValue().equals(bound)){
                    parameter = createParameter(templateDoc,bound, value);

                    templatedRoute.replaceChild(parameter,oldParameter);

                }

            }

            index++;

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
            Element parameter = createParameter(templateDoc, "routeconfigurationid", routeConfiguratinID);
            templatedRoute.appendChild(parameter);
        }
    }

    private void createCoreComponents() throws XPathExpressionException, TransformerException {

        System.out.println("scheme=" + scheme);
        System.out.println("path=" + path);

        if(path.startsWith("message:")) {
            String name = StringUtils.substringAfter(path, "message:");

            if (scheme.equalsIgnoreCase("setBody") || scheme.equalsIgnoreCase("setMessage")) {
                Node node = IntegrationUtil.getNode(conf,"/dil/core/messages/message[name='" + name + "']/body/*");
                if (node == null) {
                    node = IntegrationUtil.getNode(conf,"/dil/core/messages/message[id='" + name + "']/body/*");
                }
                String resourceAsString = DocConverter.convertNodeToString(node);

                parameter = createParameter(templateDoc, "path", resourceAsString);
                templatedRoute.appendChild(parameter);
                path = resourceAsString;
            }

            if (scheme.equalsIgnoreCase("setHeaders") || scheme.equalsIgnoreCase("setMessage")) {
                Node node = IntegrationUtil.getNode(conf,"/dil/core/messages/message[name='" + name + "']/headers");
                if (node == null) {
                    node = IntegrationUtil.getNode(conf,"/dil/core/messages/message[id='" + name + "']/headers");
                }
                String headerKeysAsString = DocConverter.convertNodeToString(node);
                parameter = createParameter(templateDoc, "headers", headerKeysAsString);
                templatedRoute.appendChild(parameter);
            }
        }
    }


}