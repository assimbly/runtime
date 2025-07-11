package org.assimbly.integrationrest.utils;

import org.assimbly.commons.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class TestApplicationContext {

    private static final Logger log = LoggerFactory.getLogger(TestApplicationContext.class);

    public static String assimblyEnv = "test";
    public static String mongoSecretKey = "c3RlbXNldmVyeXRoaW5ncmVhbHdoaWNoZWZmb3J0b2ZmaWNlc3RpZmZjYWtlZ2VuZXJhbGVsZWN0cmljbWFpbA==";
    public static String db = "mongo";
    public static String domainName = "test.assimbly.org";

    // user related
    public static String firstNameUser = "Anne";
    public static String lastNameUser = "Frank";
    public static String passwordUser = "frankfurt1929";
    public static String emailUser = "anne.frank@assimbly.com";

    public enum CamelContextField {
        ID,
        ROUTE_ID_1,
        ROUTE_ID_2,
        ROUTE_ID_3,
        ROUTE_1,
        CAMEL_CONTEXT
    }

    public enum CollectorField {
        FLOW_ID_LOG,
        FLOW_ID_STEP,
        FLOW_ID_ROUTE,
        COLLECTOR
    }

    public static Properties buildInboundHttpsExample() {
        Properties props = new Properties();

        try {
            String camelContext = Utils.readFileAsStringFromResources("InboundHttpsCamelContext.xml");

            props.setProperty(CamelContextField.ID.name(), "67921474ecaafe0007000000");
            props.setProperty(CamelContextField.ROUTE_ID_1.name(), "3d01e43c-6e86-4c9e-9972-7c872ecc37f6");
            props.setProperty(CamelContextField.ROUTE_ID_2.name(), "0e2208f0-3a58-4a9f-a0ae-41a66f184282");
            props.setProperty(CamelContextField.ROUTE_ID_3.name(), "44ac76a8-a1d1-4b1d-a93c-1c9ce4c615e9");
            props.setProperty(CamelContextField.CAMEL_CONTEXT.name(), camelContext);
        } catch (Exception e) {
            log.error("Error to build inboundHttps camel context example", e);
        }

        return props;
    }

    public static Properties buildSchedulerExample() {
        Properties props = new Properties();

        try {
            String camelContext = Utils.readFileAsStringFromResources("SchedulerCamelContext.xml");

            props.setProperty(CamelContextField.ID.name(), "67c740bc349ced00070004a9");
            props.setProperty(CamelContextField.ROUTE_ID_1.name(), "0df9d084-4783-492b-a9d4-488f2ee298a5");
            props.setProperty(CamelContextField.ROUTE_ID_2.name(), "9aa3aff8-e37c-4059-b9fd-4321454fd9ab");
            props.setProperty(CamelContextField.ROUTE_ID_3.name(), "979912f6-f6a1-43c8-9aa9-f8b480d31237");
            props.setProperty(CamelContextField.ROUTE_1.name(), Utils.extractRouteFromXmlByRouteId(camelContext, "0df9d084-4783-492b-a9d4-488f2ee298a5"));
            props.setProperty(CamelContextField.CAMEL_CONTEXT.name(), camelContext);
        } catch (Exception e) {
            log.error("Error to build scheduler camel context example", e);
        }

        return props;
    }

    public static Properties buildCollectorExample() {
        Properties props = new Properties();

        try {
            String collector = Utils.readFileAsStringFromResources("CollectorExample.json");

            props.setProperty(CollectorField.FLOW_ID_LOG.name(), "67c740bc349ced00070004a9_log");
            props.setProperty(CollectorField.FLOW_ID_ROUTE.name(), "67c740bc349ced00070004a9_route");
            props.setProperty(CollectorField.FLOW_ID_STEP.name(), "67c740bc349ced00070004a9_step");
            props.setProperty(CollectorField.COLLECTOR.name(), collector);
        } catch (Exception e) {
            log.error("Error to load collector example file", e);
        }
        return props;
    }

}
