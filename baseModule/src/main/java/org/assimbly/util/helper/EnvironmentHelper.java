package org.assimbly.util.helper;

public class EnvironmentHelper {

    public static boolean isTest() {
        return "true".equals(System.getProperty("isTest?"));
    }
}
