package org.assimbly.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

public final class ConfigHelper {

    private static final Logger log = LoggerFactory.getLogger("org.assimbly.util.ConfigHelper");

    private final String fileName;

    public ConfigHelper(String fileName) {
        this.fileName = fileName;
    }

    /**
     * Get a property by its key from the module's config file.
     *
     * @param key to get the property by.
     * @return the found property.
     */
    public String get(String key) {
        return loadFile(fileName).getProperty(key);
    }

    /**
     * Load the contents of the module's config file into a properties object.
     *
     * @param fileName to get the config file by.
     * @return a Properties object representing all the properties; empty when the file can't be read.
     */
    private Properties loadFile(String fileName) {

        Properties props = new Properties();

        try (
            InputStream fileInputStream = Files.newInputStream(Paths.get("/opt/karaf/etc/" + fileName))
        ) {
            props.load(fileInputStream);
        } catch (IOException e) {
            log.error("Loading config file {} failed", fileName, e);
        }

       return props;
    }

}
