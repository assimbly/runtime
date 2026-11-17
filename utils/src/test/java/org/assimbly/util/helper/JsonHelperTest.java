package org.assimbly.util.helper;

import org.json.JSONArray;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonHelperTest {

    @Test
    void overrideReplacesBaseObjectWithSameKey() {
        String base = "[{\"name\":\"http\",\"producerOnly\":true},{\"name\":\"file\"}]";
        String overrides = "[{\"name\":\"http\",\"producerOnly\":false}]";

        JSONArray merged = new JSONArray(JsonHelper.mergeJsonArray(base, overrides, "name"));

        assertEquals(2, merged.length());
        assertEquals("file", merged.getJSONObject(0).getString("name"));
        assertEquals("http", merged.getJSONObject(1).getString("name"));
        assertEquals(false, merged.getJSONObject(1).getBoolean("producerOnly"));
    }

    @Test
    void overrideWithNewKeyIsAppended() {
        JSONArray merged = new JSONArray(JsonHelper.mergeJsonArray("[{\"name\":\"file\"}]", "[{\"name\":\"zip\"}]", "name"));

        assertEquals(2, merged.length());
        assertEquals("zip", merged.getJSONObject(1).getString("name"));
    }

}
