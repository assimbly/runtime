package org.assimbly.integration.impl.manager;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXParseException;

import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regression tests for the defects fixed in the integration review round. */
class IntegrationReviewRegressionTest {

    @Test
    void accessTokenThatCannotBeDecryptedIsNotLogged() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(FlowManager.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        TreeMap<String, String> properties = new TreeMap<>();
        properties.put("security.email", "true");
        properties.put("route.1.routetemplate", """
                <templatedRoutes>
                  <templatedRoute routeTemplateRef="smtp-action" routeId="1-1">
                    <parameter name="username" value="user"/>
                    <parameter name="accessToken" value="RAW(plain-secret-token)"/>
                    <parameter name="tenantDbName" value="tenant"/>
                  </templatedRoute>
                </templatedRoutes>""");

        try (CamelContext context = new DefaultCamelContext()) {
            new FlowManager(context).loadFlow("1", properties);
        } finally {
            logger.detachAppender(appender);
        }

        // the failed decryption is reported, without the token
        assertTrue(appender.list.stream().anyMatch(event -> event.getFormattedMessage().contains("decrypt")));
        for (ILoggingEvent event : appender.list) {
            assertFalse(event.getFormattedMessage().contains("plain-secret-token"), event.getFormattedMessage());
        }
    }

    @Test
    void routeTemplateWithDoctypeIsRejected() {
        String xml = """
                <!DOCTYPE templatedRoute [<!ENTITY xxe SYSTEM "file:///etc/passwd">]>
                <templatedRoute routeTemplateRef="smtp-action"><description>&xxe;</description></templatedRoute>""";

        assertThrows(SAXParseException.class, () -> FlowManager.parseXml(xml));
    }

    @Test
    void fileNameCannotChangeTheFlowIdExpression() throws Exception {
        String configuration = "<dil><flows><flow><id>a</id></flow><flow><id>b</id></flow></flows></dil>";

        try (CamelContext context = new DefaultCamelContext()) {
            FlowManager flowManager = new FlowManager(context);

            assertEquals("b", flowManager.getFlowId("b", configuration));
            // no flow has this id, so the first flow is used; injected, the expression selected flow b
            assertEquals("a", flowManager.getFlowId("zzz' or id='b", configuration));
        }
    }

    @Test
    void flowLogsAreOnlyReadInsideTheLogDirectory() throws Exception {
        try (CamelContext context = new DefaultCamelContext()) {
            FlowManager flowManager = new FlowManager(context);

            assertThrows(IllegalArgumentException.class, () -> flowManager.getFlowAlertsLog("..", 3));
            assertThrows(IllegalArgumentException.class, () -> flowManager.getFlowEventsLog("../../outside", 3));
            assertEquals("0", flowManager.getFlowAlertsLog("flow-without-log", 3));
        }
    }

    @Test
    void keystoreNameCannotLeaveTheSecurityDirectory() {
        SSLManager sslManager = new SSLManager();

        assertThrows(IllegalArgumentException.class,
                () -> sslManager.getCertificateFromKeystore("../outside.p12", "password", "alias"));
        assertThrows(IllegalArgumentException.class,
                () -> sslManager.deleteCertificateInKeystore("..", "password", "alias"));
    }

    @Test
    void dependencyHealthUrlIsLoggedWithoutCredentials() {
        assertEquals("http://broker:8161/health?full=true",
                StartupManager.withoutUserInfo("http://admin:secret@broker:8161/health?full=true"));
        assertEquals("http://broker:8161/health", StartupManager.withoutUserInfo("http://broker:8161/health"));
        assertFalse(StartupManager.withoutUserInfo("http://admin:sec ret@broker").contains("sec"));
    }

    @Test
    void stepTemplateNameIsNotAPath() {
        assertTrue(ConfigManager.isResourceName("smtp-action"));
        assertFalse(ConfigManager.isResourceName("../kamelets/smtp-action"));
        assertFalse(ConfigManager.isResourceName("..\\smtp-action"));
        assertFalse(ConfigManager.isResourceName(""));
        assertFalse(ConfigManager.isResourceName(null));
    }

}
