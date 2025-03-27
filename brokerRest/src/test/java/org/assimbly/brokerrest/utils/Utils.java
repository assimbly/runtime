package org.assimbly.brokerrest.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;

public class Utils {

    private static final Logger log = LoggerFactory.getLogger(Utils.class);

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

    public static String getNowDate(String format) {
        return Instant.now().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern(format));
    }

    public static String readFileAsStringFromResources(String fileName) throws IOException {
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Path path = Path.of(Objects.requireNonNull(classLoader.getResource(fileName)).toURI());
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error(String.format("Error to load %s file from resources", fileName), e);
            return null;
        }
    }
}
