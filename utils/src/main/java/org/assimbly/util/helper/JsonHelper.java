package org.assimbly.util.helper;

import org.json.JSONArray;

public final class JsonHelper {

    public static String mergeJsonArray(String s1, String s2){
        JSONArray sourceArray = new JSONArray(s2);
        JSONArray destinationArray = new JSONArray(s1);

        for (int i = 0; i < sourceArray.length(); i++) {
            destinationArray.put(sourceArray.getJSONObject(i));
        }

        return destinationArray.toString();

    }

}
