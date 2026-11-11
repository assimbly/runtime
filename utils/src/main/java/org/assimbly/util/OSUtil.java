package org.assimbly.util;

import java.util.Locale;

public class OSUtil {

    public enum OS {
        WINDOWS, LINUX, MAC, SOLARIS
    }

    private static final OS os = detectOS();

    public static OS getOS() {
        return os;
    }

    private static OS detectOS() {
        // Locale.ROOT: in a Turkish locale "WINDOWS" lowercases to a name without "win"
        String operSys = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        if (operSys.contains("win"))       return OS.WINDOWS;
        if (operSys.contains("nix") || operSys.contains("nux") || operSys.contains("aix")) return OS.LINUX;
        if (operSys.contains("mac"))       return OS.MAC;
        if (operSys.contains("sunos"))     return OS.SOLARIS;
        throw new IllegalStateException("Unknown OS: " + operSys);
    }

}