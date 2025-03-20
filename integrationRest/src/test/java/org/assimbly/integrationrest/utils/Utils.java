package org.assimbly.integrationrest.utils;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class Utils {

    public static boolean isValidDate(String dateStr, String format) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
            formatter.parse(dateStr);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }
}
