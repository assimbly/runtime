package org.assimbly.util.helper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

public final class JsonHelper {

    public static String mergeJsonArray(String s1, String s2){
        JSONArray sourceArray = new JSONArray(s2);
        JSONArray destinationArray = new JSONArray(s1);

        for (int i = 0; i < sourceArray.length(); i++) {
            destinationArray.put(sourceArray.getJSONObject(i));
        }

        return destinationArray.toString();

    }

    /**
     * Appends the objects of the overrides array to the base array. An object in the base array that has the same
     * value for the key as an override is dropped, so the override takes its place.
     */
    public static String mergeJsonArray(String base, String overrides, String key){
        JSONArray overrideArray = new JSONArray(overrides);
        JSONArray baseArray = new JSONArray(base);

        Set<String> overriddenKeys = new HashSet<>();
        for (int i = 0; i < overrideArray.length(); i++) {
            overriddenKeys.add(overrideArray.getJSONObject(i).optString(key));
        }

        JSONArray merged = new JSONArray();
        for (int i = 0; i < baseArray.length(); i++) {
            JSONObject object = baseArray.getJSONObject(i);
            if (!overriddenKeys.contains(object.optString(key))) {
                merged.put(object);
            }
        }

        for (int i = 0; i < overrideArray.length(); i++) {
            merged.put(overrideArray.getJSONObject(i));
        }

        return merged.toString();
    }

}
