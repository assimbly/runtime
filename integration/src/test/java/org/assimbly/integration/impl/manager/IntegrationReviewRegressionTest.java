package org.assimbly.integration.impl.manager;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.apache.camel.CamelContext;
import org.apache.camel.component.mail.MailAuthenticator;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.PluginHelper;
import org.assimbly.dil.loader.FlowLoaderReport;
import org.assimbly.integration.impl.CamelIntegration;
import org.assimbly.util.IntegrationUtil;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXParseException;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
    void stoppingAFlowLeavesFlowsWithTheSameIdPrefixAlone() throws Exception {
        try (CamelContext context = new DefaultCamelContext()) {
            FlowManager flowManager = new FlowManager(context);
            PluginHelper.getRoutesLoader(context).loadRoutes(IntegrationUtil.setResource("""
                    <routes xmlns="http://camel.apache.org/schema/xml-io">
                      <route id="1-10" group="1"><from uri="direct:one"/><to uri="mock:one"/></route>
                      <route id="10-10" group="10"><from uri="direct:ten"/><to uri="mock:ten"/></route>
                    </routes>"""));
            context.start();

            flowManager.stopFlow("1", 1000);

            assertNull(context.getRoute("1-10"));
            assertNotNull(context.getRoute("10-10"));
            assertTrue(flowManager.isFlowStarted("10"));
        }
    }

    @Test
    void flowWhoseSecurityCannotBeSetUpFailsToLoad() throws Exception {
        TreeMap<String, String> properties = new TreeMap<>();
        properties.put("security.email", "true");
        properties.put("route.1.routetemplate", """
                <!DOCTYPE templatedRoutes>
                <templatedRoutes><templatedRoute routeTemplateRef="smtp-action" routeId="1-1"/></templatedRoutes>""");

        try (CamelContext context = new DefaultCamelContext()) {
            FlowLoaderReport report = new FlowManager(context).loadFlow("1", properties);

            assertTrue(report.isStatusFailed());
            assertTrue(report.getReport().contains("DOCTYPE"), report.getReport());
        }
    }

    @Test
    void reloadingAFlowReplacesItsMailAuthenticator() throws Exception {
        TreeMap<String, String> properties = new TreeMap<>();
        properties.put("security.email", "true");
        properties.put("route.1.routetemplate", """
                <templatedRoutes>
                  <templatedRoute routeTemplateRef="smtp-action" routeId="1-1">
                    <parameter name="username" value="user"/>
                    <parameter name="accessToken" value="RAW(token)"/>
                    <parameter name="tenantDbName" value="tenant"/>
                  </templatedRoute>
                </templatedRoutes>""");

        try (CamelContext context = new DefaultCamelContext()) {
            FlowManager flowManager = new FlowManager(context);
            flowManager.loadFlow("1", properties);
            flowManager.loadFlow("1", properties);

            assertEquals(1, context.getRegistry().findByType(MailAuthenticator.class).size());
        }
    }

    @Test
    void integrationWithoutStoreAnswersInsteadOfFailing() throws Exception {
        CamelIntegration integration = new CamelIntegration(false);

        assertTrue(integration.getIntegrationAlertsCount().isEmpty());
        assertTrue(integration.getFlowInfo("1", "application/json").contains("unconfigured"));
        assertTrue(integration.startFlow("1", 1000).contains("Flow is not installed"));
        integration.stop();
    }

    @Test
    void unknownFlowIsReportedAsNotInstalled() throws Exception {
        CamelIntegration integration = new CamelIntegration(false);
        integration.initDilStore();
        try {
            for (String report : List.of(integration.startFlow("unknown", 1000), integration.restartFlow("unknown", 1000),
                    integration.resumeFlow("unknown"), integration.pauseFlow("unknown"))) {
                assertTrue(report.contains("Flow is not installed"), report);
            }
        } finally {
            integration.stop();
        }
    }

    @Test
    void testingAFlowKeepsItsInstalledConfiguration() throws Exception {
        CamelIntegration integration = new CamelIntegration(false);
        integration.initDilStore();
        try {
            TreeMap<String, String> installed = new TreeMap<>(Map.of("id", "t1", "flow.name", "installed"));
            integration.setFlowConfiguration("t1", installed);

            integration.testFlow("t1", 1000, "application/json", """
                    {"dil":{"integrations":{"integration":{"flows":{"flow":{"id":"t1","name":"tested","type":"esb"}}}}}}""");
            assertEquals(installed, integration.getFlowConfiguration("t1"));

            integration.testFlow("t2", 1000, "application/json", """
                    {"dil":{"integrations":{"integration":{"flows":{"flow":{"id":"t2","name":"tested","type":"esb"}}}}}}""");
            assertNull(integration.getFlowConfiguration("t2"));
        } finally {
            integration.stop();
        }
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
