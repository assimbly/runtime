package org.assimbly.integration.impl.manager;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.apache.camel.support.SimpleRegistry;
import org.assimbly.docconverter.DocConverter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The DocConverter simple functions, like ${xmltojson}, convert the message body. */
class DocConverterSimpleFunctionsTest {

    private static final String XML = "<order><id>1</id><item>book</item></order>";

    private CamelContext context;

    @BeforeEach
    void start() {
        context = new DefaultCamelContext();
        new ConfigManager(context, new SimpleRegistry()).setDocConverterSimpleFunctions();
        context.start();
    }

    @AfterEach
    void stop() {
        context.stop();
    }

    @Test
    void convertsXmlBody() {
        assertEquals(DocConverter.xmlToJson(XML), convert("xmltojson", XML));
        assertEquals(DocConverter.xmlToYaml(XML), convert("xmltoyaml", XML));
        assertEquals(DocConverter.xmlToCsv(XML), convert("xmltocsv", XML));
    }

    @Test
    void convertsJsonBody() {
        String json = DocConverter.xmlToJson(XML);
        assertEquals(DocConverter.jsonToXml(json), convert("jsontoxml", json));
        assertEquals(DocConverter.jsonToYaml(json), convert("jsontoyaml", json));
        assertEquals(DocConverter.jsonToCsv(json), convert("jsontocsv", json));
    }

    @Test
    void convertsYamlBody() {
        String yaml = DocConverter.xmlToYaml(XML);
        assertEquals(DocConverter.yamlToXml(yaml), convert("yamltoxml", yaml));
        assertEquals(DocConverter.yamlToJson(yaml), convert("yamltojson", yaml));
        assertEquals(DocConverter.yamlToCsv(yaml), convert("yamltocsv", yaml));
    }

    @Test
    void convertsCsvBody() {
        String csv = "id,item\n1,book\n";
        assertEquals(DocConverter.csvToXml(csv), convert("csvtoxml", csv));
        assertEquals(DocConverter.csvToJson(csv), convert("csvtojson", csv));
        assertEquals(DocConverter.csvToYaml(csv), convert("csvtoyaml", csv));
    }

    private String convert(String function, String body) {
        Exchange exchange = new DefaultExchange(context);
        exchange.getMessage().setBody(body);
        return context.resolveLanguage("simple").createExpression("${" + function + "}").evaluate(exchange, String.class);
    }
}
