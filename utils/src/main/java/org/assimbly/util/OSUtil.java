package org.assimbly.util;

public class OSUtil {

    public enum OS {
        WINDOWS, LINUX, MAC, SOLARIS
    }

    private static final OS os = detectOS();

    public static OS getOS() {
        return os;
    }

    private static OS detectOS() {
        String operSys = System.getProperty("os.name").toLowerCase();
        if (operSys.contains("win"))       return OS.WINDOWS;
        if (operSys.contains("nix") || operSys.contains("nux") || operSys.contains("aix")) return OS.LINUX;
        if (operSys.contains("mac"))       return OS.MAC;
        if (operSys.contains("sunos"))     return OS.SOLARIS;
        throw new IllegalStateException("Unknown OS: " + operSys);
    }

}