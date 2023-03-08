package org.assimbly.integrationrest.utils;

import java.util.Properties;
import java.util.UUID;

public class CamelContextUtil {

    public enum Field {
        id,
        routeId1,
        routeId2,
        camelContext
    }

    static public Properties buildExample() {
        Properties props = new Properties();
        UUID randomContextPath = UUID.randomUUID();

        StringBuffer camelContextBuf = new StringBuffer();
        camelContextBuf.append("<camelContext id=\"ID_63ee34e25827222b3d000022\" xmlns=\"http://camel.apache.org/schema/blueprint\" useMDCLogging=\"true\" streamCache=\"true\">");
        camelContextBuf.append("<jmxAgent id=\"agent\" loadStatisticsEnabled=\"true\"/>");
        camelContextBuf.append("<streamCaching id=\"streamCacheConfig\" spoolThreshold=\"0\" spoolDirectory=\"tmp/camelcontext-#camelId#\" spoolUsedHeapMemoryThreshold=\"70\"/>");
        camelContextBuf.append("<threadPoolProfile id=\"wiretapProfile\" defaultProfile=\"false\" poolSize=\"0\" maxPoolSize=\"5\" maxQueueSize=\"2000\" rejectedPolicy=\"DiscardOldest\" keepAliveTime=\"10\"/>");
        camelContextBuf.append("<threadPoolProfile id=\"defaultProfile\" defaultProfile=\"true\" poolSize=\"0\" maxPoolSize=\"10\" maxQueueSize=\"1000\" rejectedPolicy=\"CallerRuns\" keepAliveTime=\"30\"/>");
        camelContextBuf.append("<onException>");
        camelContextBuf.append("<exception>java.lang.Exception</exception>");
        camelContextBuf.append("<redeliveryPolicy maximumRedeliveries=\"0\" redeliveryDelay=\"5000\"/>");
        camelContextBuf.append("<setExchangePattern pattern=\"InOnly\"/>");
        camelContextBuf.append("</onException>");
        camelContextBuf.append("<route id=\"0bc12100-ae01-11ed-8f2a-c39ccdb17c7e\">");
        camelContextBuf.append("<from uri=\"jetty:https://0.0.0.0:9001/1/"+randomContextPath.toString()+"?matchOnUriPrefix=false\"/>");
        camelContextBuf.append("<removeHeaders pattern=\"CamelHttp*\"/>");
        camelContextBuf.append("<to uri=\"direct:ID_63ee34e25827222b3d000022_test_0bc12100-ae01-11ed-8f2a-c39ccdb17c7e?exchangePattern=InOut\"/>");
        camelContextBuf.append("</route>");
        camelContextBuf.append("<route id=\"0e3d92b0-ae01-11ed-8f2a-c39ccdb17c7e\">");
        camelContextBuf.append("<from uri=\"direct:ID_63ee34e25827222b3d000022_test_0bc12100-ae01-11ed-8f2a-c39ccdb17c7e\"/>");
        camelContextBuf.append("<setHeader headerName=\"CamelVelocityTemplate\">");
        camelContextBuf.append("<simple>sdfgsdfgdsfg</simple>");
        camelContextBuf.append("</setHeader>");
        camelContextBuf.append("<to uri=\"velocity:generate\"/>");
        camelContextBuf.append("</route>");
        camelContextBuf.append("</camelContext>");

        props.setProperty(Field.id.name(), "ID_63ee34e25827222b3d000022");
        props.setProperty(Field.routeId1.name(), "0bc12100-ae01-11ed-8f2a-c39ccdb17c7e");
        props.setProperty(Field.routeId2.name(), "0e3d92b0-ae01-11ed-8f2a-c39ccdb17c7e");
        props.setProperty(Field.camelContext.name(), camelContextBuf.toString());

        return props;
    }
}
