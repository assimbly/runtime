package org.assimbly.connector.connect.util;

import java.io.File;

public class BaseDirectory {

    private static final BaseDirectory INSTANCE = new BaseDirectory();

    private volatile String baseDirectory = System.getProperty("user.home") + "/.assimbly";

    public static BaseDirectory getInstance() {
        return INSTANCE;
    }

    public String getBaseDirectory() {
        return baseDirectory;
    }

    public void setBaseDirectory(String baseDirectory) {
        File directory = new File(baseDirectory);
        if (! directory.exists()){
            directory.mkdirs();
        }
        this.baseDirectory = baseDirectory;
    }

    //example usage
    //BaseDirectory.getInstance().setBaseDirectory(10);
    //System.out.println(BaseDirectory.getInstance().getBaseDirectory());

}


