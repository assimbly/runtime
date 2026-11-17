package org.assimbly.util.helper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PropertiesHelper {

    private static final Logger log = LoggerFactory.getLogger(PropertiesHelper.class);

    private static final String FILE_NAME = "config.properties";

    private PropertiesHelper() {
        //Static class cannot be instantiated.
    }

    /**
     * Get a property by its key from the module's config file.
     *
     * @param key to get the property by.
     * @return the found property.
     */
    public static String getProperty(String key) {
        try {
            Properties props = loadProperties();
            return props.getProperty(key);
        } catch (IOException e) {
            log.error("Loading {} failed", FILE_NAME, e);
        }

        return "";
    }

    /**
     * Load the contents of the module's config file into a properties object.
     *
     * @return a Properties object representing all the properties.
     * @throws IOException when something went wrong while loading the file.
     */
    private static Properties loadProperties() throws IOException {
        ClassLoader loader = PropertiesHelper.class.getClassLoader();

        Properties props = new Properties();

        try (InputStream resourceStream = loader.getResourceAsStream(PropertiesHelper.FILE_NAME)) {
            // without a config file there are no properties
            if (resourceStream != null) {
                props.load(resourceStream);
            }
        }

        return props;
    }

}