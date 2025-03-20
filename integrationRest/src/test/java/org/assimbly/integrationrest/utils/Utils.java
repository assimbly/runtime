package org.assimbly.integrationrest.utils;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class Utils {

    public static boolean isValidDate(String dateStr, String format) {
        try {
            // adjust milliseconds dynamically if needed
            String normalizedDateStr = normalizeMilliseconds(dateStr, format);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
            formatter.parse(normalizedDateStr);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    private static String normalizeMilliseconds(String dateStr, String format) {
        // only modify if the format expects milliseconds
        if (format.contains("SSS")) {
            // 1-digit milliseconds -> add two trailing zeros
            dateStr = dateStr.replaceAll("(\\.\\d)(?!\\d)", "$100");
            // 2-digit milliseconds -> add a trailing zero
            dateStr = dateStr.replaceAll("(\\.\\d{2})(?!\\d)", "$10");
        }
        return dateStr;
    }
}
