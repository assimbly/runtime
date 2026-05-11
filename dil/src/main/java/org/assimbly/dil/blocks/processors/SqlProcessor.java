package org.assimbly.dil.blocks.processors;

import java.sql.*;
import java.util.List;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.assimbly.docconverter.DocConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class SqlProcessor implements Processor {

    private static final Logger log = LoggerFactory.getLogger(org.assimbly.dil.blocks.processors.SqlProcessor.class);

    @Override
    public void process(Exchange exchange) throws Exception {

        Message message = exchange.getMessage();
        String contentType = message.getHeader("Content-Type", String.class);
        String output = "";

        if(contentType.equalsIgnoreCase("text/plain")){
            output = message.getBody(String.class);
        } else {
            Document result = processResult(message);
            output = prettyPrint(result);
            if (contentType.equalsIgnoreCase("application/json")){
                output = DocConverter.convertXmlToJson(output);
            }
        }

        message.removeHeader("hasErrors");
        message.removeHeader("errorMessage");
        message.removeHeaders("CamelSql*");

        message.setBody(output);
    }

    private Document processResult(Message message) throws ParserConfigurationException {

        Document doc = newDocument();
        if (doc == null) throw new ParserConfigurationException();

        Element rootElement = doc.createElement("ResultSet");
        doc.appendChild(rootElement);

        boolean hasErrors= message.getHeader("HasErrors",Boolean.class);

        if(hasErrors){
            String errorMessage= message.getHeader("errorMessage",String.class);
            appendErrorNodes(doc, rootElement, errorMessage);
        }else{
            Element results = doc.createElement("Results");

            List<Map<String, Object>> rows = message.getBody(List.class);

            int rowCount = 0;
            if(rows == null || rows.isEmpty()){
                rows = message.getHeader("CamelSqlGeneratedKeyRows",List.class);
                rowCount= message.getHeader("CamelSqlGeneratedKeysRowCount",int.class);

            }else{
                rowCount= message.getHeader("CamelSqlRowCount",int.class);
            }

            if (rows != null) {
                appendResultSize(doc, rootElement, rowCount);
                for (Map<String, Object> row : rows) {
                    results.appendChild(buildResultRow(doc, row));
                }
            }

            rootElement.appendChild(results);

        }

        return doc;

    }

    private void appendErrorNodes(Document doc, Element rootElement, String errorMessage) {
        appendHasErrors(doc, rootElement);
        Node errorMessageNode = doc.createElement("ErrorMessage");
        errorMessageNode.setTextContent(errorMessage);
        rootElement.appendChild(errorMessageNode);
    }

    private Node buildResultRow(Document doc, Map<String, Object> row) {
        Node result = doc.createElement("Result");

        row.forEach((key, value) -> {
            Node attribute = doc.createElement(key);
            attribute.setTextContent(value.toString());
            result.appendChild(attribute);
        });

        return result;
    }

    private void appendResultSize(Document doc, Element rootElement, int rowCount) {
        Node resultSize = doc.createElement("ResultSize");
        resultSize.setTextContent(String.valueOf(rowCount));
        rootElement.appendChild(resultSize);
    }

    private void appendHasErrors(Document doc, Element rootElement) {
        Node hasErrorsNode = doc.createElement("HasErrors");
        hasErrorsNode.setTextContent(String.valueOf(true));
        rootElement.appendChild(hasErrorsNode);
    }

    private void appendErrorNodes(Document doc, Element rootElement, Exception e) {
        appendHasErrors(doc, rootElement);
        Node errorMessage = doc.createElement("ErrorMessage");
        errorMessage.setTextContent(e.getMessage());
        rootElement.appendChild(errorMessage);
    }

    public static Document newDocument() {
        DocumentBuilderFactory icFactory;
        DocumentBuilder icBuilder;

        try {
            icFactory = DocumentBuilderFactory.newInstance();
            icBuilder = icFactory.newDocumentBuilder();

            return icBuilder.newDocument();
        } catch (ParserConfigurationException e) {
            log.error(e.getMessage(), e);
        }

        return null;
    }

    public String prettyPrint(Document doc) {
        DOMImplementationLS domImplementation = (DOMImplementationLS) doc.getImplementation();
        LSSerializer lsSerializer = domImplementation.createLSSerializer();

        // Set the pretty print hint
        lsSerializer.getDomConfig().setParameter("format-pretty-print", Boolean.TRUE);

        return lsSerializer.writeToString(doc);
    }


}