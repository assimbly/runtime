package org.assimbly.dil.transpiler.marshalling.catalog;

import java.util.*;

public class CustomKameletCatalog {

    private static final Set<String> names = new HashSet<>();

    public static Set<String> getNames() {
        return names;
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

