package org.assimbly.dil;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.assimbly.dil.blocks.beans.enrich.zipfile.ZipFileEnrichStrategy;
import org.assimbly.dil.blocks.processors.SqlProcessor;
import org.assimbly.dil.blocks.processors.UnzipProcessor;
import org.assimbly.dil.event.EventConfigurer;
import org.assimbly.dil.event.domain.Collection;
import org.assimbly.dil.event.domain.Store;
import org.assimbly.dil.event.store.impl.FileStore;
import org.assimbly.dil.loader.FlowLoader;
import org.assimbly.dil.loader.FlowLoaderReport;
import org.assimbly.util.EncryptionUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regression tests for the defects fixed in the DIL review round. */
class DilReviewRegressionTest {

    @Test
    void unzipFailsWhenTheContentExceedsTheLimit() throws Exception {
        byte[] zip = zip(Map.of("a.txt", new byte[600], "b.bin", new byte[600]));

        try (CamelContext context = new DefaultCamelContext()) {
            Exchange tooLarge = new DefaultExchange(context);
            tooLarge.getIn().setBody(zip);
            assertThrows(IOException.class, () -> new UnzipProcessor(1000).process(tooLarge));

            Exchange fits = new DefaultExchange(context);
            fits.getIn().setBody(zip);
            new UnzipProcessor(1200).process(fits);
            assertEquals(2, fits.getIn().getBody(List.class).size());
        }
    }

    @Test
    void decryptedStepIsNotLogged() throws Exception {
        EncryptionUtil encryptionUtil = new EncryptionUtil("regression-test-key");
        String secret = "plain-text-secret";

        TreeMap<String, String> props = new TreeMap<>();
        props.put("id", "flow1");
        props.put("action.1.routeconfiguration.id", "flow1-configuration");
        props.put("action.1.routeconfiguration", """
                <routeConfiguration id="flow1-configuration">
                  <onException><exception>java.lang.IllegalStateException</exception><log message="%s"/></onException>
                </routeConfiguration>""".formatted(encryptionUtil.encrypt(secret)));

        Logger logger = (Logger) LoggerFactory.getLogger(FlowLoader.class);
        Level level = logger.getLevel();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.setLevel(Level.DEBUG);
        logger.addAppender(appender);

        try (CamelContext context = new DefaultCamelContext()) {
            context.addRoutes(new FlowLoader(props, new FlowLoaderReport("flow1", "flow", "1"), encryptionUtil));
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(level);
        }

        List<String> logged = appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
        assertTrue(logged.stream().anyMatch(message -> message.contains("flow1-configuration")), logged.toString());
        assertTrue(logged.stream().noneMatch(message -> message.contains(secret)), logged.toString());
    }

    @Test
    void routeTemplateDefinitionIsLoaded() throws Exception {
        TreeMap<String, String> props = new TreeMap<>();
        props.put("id", "flow1");
        props.put("action.1.routetemplatedefinition.id", "flow1-template");
        props.put("action.1.routetemplatedefinition", """
                <routeTemplates xmlns="http://camel.apache.org/schema/xml-io">
                  <routeTemplate id="flow1-template">
                    <templateParameter name="in"/>
                    <route><from uri="direct:{{in}}"/><to uri="mock:out"/></route>
                  </routeTemplate>
                </routeTemplates>""");

        try (CamelContext context = new DefaultCamelContext()) {
            FlowLoader flowLoader = new FlowLoader(props, new FlowLoaderReport("flow1", "flow", "1"), null);
            context.addRoutes(flowLoader);

            assertTrue(flowLoader.isFlowLoaded(), flowLoader.getReport());
            assertNotNull(((ModelCamelContext) context).getRouteTemplateDefinition("flow1-template"));
        }
    }

    @Test
    void zipEnrichAddsTheFileAndKeepsTheOthers() throws Exception {
        try (CamelContext context = new DefaultCamelContext()) {
            Exchange original = new DefaultExchange(context);
            original.getIn().setBody(zip(Map.of("a.txt", "a".getBytes(StandardCharsets.UTF_8))));
            Exchange resource = new DefaultExchange(context);
            resource.getIn().setBody("b");
            resource.getIn().setHeader(Exchange.FILE_NAME, "dir/b.txt");

            new ZipFileEnrichStrategy().aggregate(original, resource);

            assertEquals(Set.of("a.txt", "dir/", "dir/b.txt"), entryNames(original.getIn().getBody(byte[].class)));
        }
    }

    @Test
    void zipEnrichFailsWithoutAFileName() throws Exception {
        try (CamelContext context = new DefaultCamelContext()) {
            Exchange original = new DefaultExchange(context);
            original.getIn().setBody(zip(Map.of("a.txt", new byte[1])));
            Exchange resource = new DefaultExchange(context);
            resource.getIn().setBody("b");

            ZipFileEnrichStrategy strategy = new ZipFileEnrichStrategy();
            assertThrows(IllegalArgumentException.class, () -> strategy.aggregate(original, resource));
        }
    }

    @Test
    void sqlResultWithoutRowCountHeader() throws Exception {
        try (CamelContext context = new DefaultCamelContext()) {
            Exchange noRows = new DefaultExchange(context);
            noRows.getMessage().setHeader("Content-Type", "application/xml");
            noRows.getMessage().setBody(List.of());
            new SqlProcessor().process(noRows);
            assertTrue(noRows.getMessage().getBody(String.class).contains("ResultSet"));

            Exchange oneRow = new DefaultExchange(context);
            oneRow.getMessage().setHeader("Content-Type", "application/xml");
            oneRow.getMessage().setBody(List.of(Map.of("ID", 7)));
            new SqlProcessor().process(oneRow);
            String xml = oneRow.getMessage().getBody(String.class);
            assertTrue(xml.contains("<ResultSize>1</ResultSize>") && xml.contains("<ID>7</ID>"), xml);
        }
    }

    @Test
    void removedLogCollectorIsDetachedFromItsLoggers() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger("org.assimbly.dil.regression");

        try (CamelContext context = new DefaultCamelContext()) {
            EventConfigurer configurer = new EventConfigurer("collector1", context);
            String result = configurer.add("""
                    {"id":"collector1","type":"log","flowId":"flow1","events":["org.assimbly.dil.regression"],"stores":[{"type":"console"}],"filters":[]}""");
            assertEquals("configured", result);
            assertNotNull(logger.getAppender("collector1"));

            configurer.remove("collector1");

            assertNull(logger.getAppender("collector1"));
        } finally {
            logger.setLevel(null);
        }
    }

    @Test
    void fileStoreAppendsToTheConfiguredFile(@TempDir Path directory) throws Exception {
        Path file = directory.resolve("events.log");
        Store store = new Collection().fromJson("""
                {"id":"collector1","type":"step","stores":[{"type":"file","uri":"%s"}]}""".formatted(file.toString().replace('\\', '/'))).getStores().getFirst();

        FileStore fileStore = new FileStore("collector1", store);
        fileStore.store("{\"event\":1}");
        fileStore.store("{\"event\":2}");

        assertEquals(List.of("{\"event\":1}", "{\"event\":2}"), Files.readAllLines(file));
    }

    private static Set<String> entryNames(byte[] zip) throws IOException {
        Set<String> names = new HashSet<>();
        try (ZipInputStream input = new ZipInputStream(new ByteArrayInputStream(zip))) {
            for (ZipEntry entry = input.getNextEntry(); entry != null; entry = input.getNextEntry()) {
                names.add(entry.getName());
            }
        }
        return names;
    }

    private static byte[] zip(Map<String, byte[]> entries) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                zip.write(entry.getValue());
                zip.closeEntry();
            }
        }
        return bytes.toByteArray();
    }

}
