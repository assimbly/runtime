package org.assimbly.integrationrest.utils;

import org.assimbly.util.api.ApiUtils;
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

    public enum DILField {
        ID,
        STEP_ID_1,
        STEP_ID_2,
        STEP_ID_3,
        STEP_1,
        DIL
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
            String dil = ApiUtils.readFileAsStringFromResources("InboundHttps.json");

            props.setProperty(DILField.ID.name(), "69fc98bed4e0d00010000071");
            props.setProperty(DILField.STEP_ID_1.name(), "e1ae2f77-d5ee-4803-bf09-c07a8a1bc709");
            props.setProperty(DILField.STEP_ID_2.name(), "3035fb1f-acdf-4f12-89ec-a0ade7fd72bc");
            props.setProperty(DILField.STEP_ID_3.name(), "9545677a-edec-4776-ab98-3bf19fef6842");
            props.setProperty(DILField.DIL.name(), dil);
        } catch (Exception e) {
            log.error("Error to build inboundHttps example", e);
        }

        return props;
    }

    public static Properties buildSchedulerExample() {
        Properties props = new Properties();

        try {
            String dil = ApiUtils.readFileAsStringFromResources("Scheduler.json");

            props.setProperty(DILField.ID.name(), "69fc94aed4e0d00010000040");
            props.setProperty(DILField.STEP_ID_1.name(), "b1c55ffa-2cf0-4135-bfcd-980c57d832d4");
            props.setProperty(DILField.STEP_ID_2.name(), "37738fd1-53d8-4896-be2a-18b65b7abf6e");
            props.setProperty(DILField.STEP_ID_3.name(), "1ae7ab8d-3b16-487c-bf9d-49ba9f56b8c7");
//            props.setProperty(CamelContextField.STEP_1.name(), ApiUtils.extractRouteFromXmlByRouteId(dil, "b1c55ffa-2cf0-4135-bfcd-980c57d832d4"));
            props.setProperty(DILField.DIL.name(), dil);
        } catch (Exception e) {
            log.error("Error to build scheduler example", e);
        }

        return props;
    }

    public static Properties buildCollectorExample() {
        Properties props = new Properties();

        try {
            String collector = ApiUtils.readFileAsStringFromResources("CollectorExample.json");

            props.setProperty(CollectorField.FLOW_ID_LOG.name(), "69fc94aed4e0d00010000040_log");
            props.setProperty(CollectorField.FLOW_ID_ROUTE.name(), "69fc94aed4e0d00010000040_route");
            props.setProperty(CollectorField.FLOW_ID_STEP.name(), "69fc94aed4e0d00010000040_step");
            props.setProperty(CollectorField.COLLECTOR.name(), collector);
        } catch (Exception e) {
            log.error("Error to load collector example file", e);
        }
        return props;
    }

}
