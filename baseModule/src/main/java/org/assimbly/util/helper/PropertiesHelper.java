package org.assimbly.util.helper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class PropertiesHelper {

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
            Properties props = loadProperties(FILE_NAME);
            return props.getProperty(key);
        } catch (IOException e) {
            Logger.getLogger(PropertiesHelper.class.getName()).log(Level.SEVERE, null, e);
        }

        return "";
    }

    /**
     * Load the contents of the module's config file into a properties object.
     *
     * @param fileName to get the config file by.
     * @return a Properties object representing all the properties.
     * @throws IOException when something went wrong while loading the file.
     */
    private static Properties loadProperties(String fileName) throws IOException {
        ClassLoader loader = PropertiesHelper.class.getClassLoader();

        InputStream resourceStream = loader.getResourceAsStream(fileName);

        Properties props = new Properties();
        props.load(resourceStream);

        return props;
    }

}