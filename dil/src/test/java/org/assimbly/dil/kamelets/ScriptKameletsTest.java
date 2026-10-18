package org.assimbly.dil.kamelets;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteConfigurationBuilder;
import org.apache.camel.builder.TemplatedRouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.PluginHelper;
import org.apache.camel.support.ResourceHelper;
import org.assimbly.dil.blocks.beans.UuidExtensionFunction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Every language a Script Step offers has a script kamelet that runs the script on the message. */
class ScriptKameletsTest {

    private CamelContext context;

    @BeforeEach
    void startContext() throws Exception {
        context = new DefaultCamelContext();
        context.getRegistry().bind("uuid-function", new UuidExtensionFunction());
        context.addRoutes(new RouteConfigurationBuilder() {
            @Override
            public void configuration() {
                routeConfiguration("0");
            }
        });
        context.start();
    }

    @AfterEach
    void stopContext() {
        context.stop();
    }

    @Test
    void simple() throws Exception {
        assertEquals("Hello World", run("simple-script", "Hello ${body}", "World"));
    }

    @Test
    void groovy() throws Exception {
        assertEquals(7, run("groovy-script", "bar = \"baz\"\n// comment\nresult = body * 2 + 1", 3));
    }

    @Test
    void javascript() throws Exception {
        assertEquals("HELLO", String.valueOf(run("javascript-script", "body.toUpperCase()", "hello")));
    }

    @Test
    void xslt() throws Exception {
        String xslt = """
            <xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
              <xsl:output method="text"/>
              <xsl:template match="/">Hello <xsl:value-of select="/name"/>?&amp;</xsl:template>
            </xsl:stylesheet>""";
        assertEquals("Hello World?&", run("xslt-script", xslt, "<name>World</name>"));
    }

    @Test
    void jslt() throws Exception {
        assertEquals("{\"greeting\":\"Hello World\"}", run("jslt-script", "{\"greeting\": \"Hello \" + .name}", "{\"name\":\"World\"}"));
    }

    private Object run(String kamelet, String path, Object body) throws Exception {
        PluginHelper.getRoutesLoader(context).loadRoutes(
            ResourceHelper.resolveMandatoryResource(context, "classpath:kamelets/" + kamelet + ".kamelet.yaml"));

        TemplatedRouteBuilder.builder(context, kamelet)
            .routeId(kamelet + "-route")
            .parameter("routeId", kamelet + "-route")
            .parameter("stepId", "1")
            .parameter("in", "direct:in")
            .parameter("sink", "true")
            .parameter("path", path)
            .add();

        try (ProducerTemplate template = context.createProducerTemplate()) {
            return template.requestBody("direct:in", body);
        }
    }
}
