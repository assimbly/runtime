package org.assimbly.dil.event.store.impl;

import org.apache.commons.io.FileUtils;
import org.assimbly.util.BaseDirectory;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class FileStore {

    private final String baseDir = BaseDirectory.getInstance().getBaseDirectory();
    private File file;
    private Date date = new Date();
    private String collectorId;
    private org.assimbly.dil.event.domain.Store store;

    public FileStore(String collectorId, org.assimbly.dil.event.domain.Store store) {
        this.collectorId = collectorId;
        this.store = store;

        if(file==null){
            createFile(this.collectorId, this.store);
        }

    }

    public void store(String json) {
        List<String> line = Arrays.asList(json);
        try {
            FileUtils.writeLines(file, line, true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void createFile(String collectorid, org.assimbly.dil.event.domain.Store store){

        String uri = store.getUri();
        String today = new SimpleDateFormat("yyyyMMdd").format(date);

        if(uri!=null)
            file = new File(uri);
        else{
            file = new File(baseDir + "/events/" + collectorid + "/" + today + "_events.log");
        }

    }

}


