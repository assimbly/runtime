package org.assimbly.integration.impl;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** HTTP and HTTPS can be a Source (a preconfigured Jetty server) as well as an Action or Sink. */
class CamelIntegrationComponentsTest {

    private static CamelIntegration integration;

    @BeforeAll
    static void createIntegration() throws Exception {
        integration = new CamelIntegration(false);
    }

    @Test
    void httpAndHttpsAreListedOnceAndCanBeASource() throws Exception {
        JSONArray components = new JSONArray(integration.getComponents(true, "application/json"));

        for (String name : new String[]{"http", "https"}) {
            long listed = IntStream.range(0, components.length()).filter(i -> name.equals(components.getJSONObject(i).optString("name"))).count();
            JSONObject component = IntStream.range(0, components.length()).mapToObj(components::getJSONObject)
                    .filter(candidate -> name.equals(candidate.optString("name"))).findFirst().orElseThrow();

            assertEquals(1, listed, name + " is listed once");
            assertFalse(component.getBoolean("producerOnly"), name + " can be a Source");
            assertFalse(component.getBoolean("consumerOnly"), name + " can be an Action or Sink");
        }
    }

    @Test
    void httpsSourceOptionsComeFromTheKamelet() throws Exception {
        JSONObject schema = new JSONObject(integration.getComponentSchema("https", "source", "application/json"));

        assertEquals("kamelet", schema.getJSONObject("component").getString("kind"));
        assertTrue(schema.getJSONObject("properties").has("matchOnUriPrefix"));
    }

    @Test
    void httpActionOptionsStillComeFromTheCamelCatalog() throws Exception {
        JSONObject schema = new JSONObject(integration.getComponentSchema("http", "action", "application/json"));

        assertEquals("component", schema.getJSONObject("component").getString("kind"));
        assertTrue(schema.getJSONObject("properties").has("httpUri"));
    }

}
