package org.assimbly.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class BaseDirectory {

    private static final Logger log = LoggerFactory.getLogger("org.assimbly.util.BaseDirectory");

    private static final BaseDirectory INSTANCE = new BaseDirectory();

    private volatile String baseDirectoryPath = System.getProperty("user.home") + "/.assimbly";

    public static BaseDirectory getInstance() {
        return INSTANCE;
    }

    public String getBaseDirectory() {
        return baseDirectoryPath;
    }

    public void setBaseDirectory(String baseDirectoryPath) {
        File directory = new File(baseDirectoryPath);
        if (! directory.exists()){
            boolean dirCreated = directory.mkdirs();
            if(!dirCreated){
                log.warn("Could not create base directory: {}", baseDirectoryPath);
            }
        }
        this.baseDirectoryPath = baseDirectoryPath;
    }

}