package org.assimbly.brokerrest.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Properties;

public class TestApplicationContext {

    private static final Logger log = LoggerFactory.getLogger(TestApplicationContext.class);

    public static String ASSIMBLY_ENV = "test";
    public static String MONGO_SECRET_KEY = "c3RlbXNldmVyeXRoaW5ncmVhbHdoaWNoZWZmb3J0b2ZmaWNlc3RpZmZjYWtlZ2VuZXJhbGVsZWN0cmljbWFpbA==";
    public static String ASSIMBLY_BROKER_JMX_PORT = "1616";

    public enum CamelContextField {
        id,
        httpRetryQueue,
        camelContext
    }

    public static Properties buildSchedulerEnrichExample() {
        Properties props = new Properties();

        try {
            String camelContext = readFileFromResources("SchedulerHttpRetryCamelContext.xml");

            props.setProperty(CamelContextField.id.name(), "67c740bc349ced00070004a9");
            props.setProperty(CamelContextField.httpRetryQueue.name(), "67c740bc349ced00070004a9_test_5258ee10-4e8d-4b15-a86f-de6bcc16263b_http_retry");
            props.setProperty(CamelContextField.camelContext.name(), camelContext);
        } catch (Exception e) {
            log.error("Error to build scheduler enrich camel context example", e);
        }

        return props;
    }

    private static String readFileFromResources(String fileName) throws IOException {
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Path path = Path.of(Objects.requireNonNull(classLoader.getResource(fileName)).toURI());
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error(String.format("Error to load %s file from resources", fileName), e);
            return null;
        }
    }

}
