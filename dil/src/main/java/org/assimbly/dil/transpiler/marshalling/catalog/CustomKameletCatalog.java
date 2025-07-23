package org.assimbly.dil.transpiler.marshalling.catalog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CustomKameletCatalog {

    private static final List<String> names = new ArrayList<>();

    public static List<String> getNames() {
        return Collections.unmodifiableList(names); // Return read-only view
    }

    public static void addAllNames(List<String> list) {
        names.addAll(list);
    }

    public static void addName(String name) {
        names.add(name);
    }

    public static boolean removeName(String name) {
        return names.remove(name);
    }

}

