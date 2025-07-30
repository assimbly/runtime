package org.assimbly.dil.event.store.impl;

import org.apache.commons.io.FileUtils;
import org.assimbly.util.BaseDirectory;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class FileStore {

    private final String baseDir = BaseDirectory.getInstance().getBaseDirectory();
    private File file;
    private final Date date = new Date();

    public FileStore(String collectorId, org.assimbly.dil.event.domain.Store store) {

        createFile(collectorId, store);

    }

    public void store(String json) {
        List<String> line = Collections.singletonList(json);
        try {
            FileUtils.writeLines(file, line, true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void createFile(String collectorid, org.assimbly.dil.event.domain.Store store){

        String uri = store.getUri();
        String today = new SimpleDateFormat("yyyyMMdd").format(date);

        file = new File(Objects.requireNonNullElseGet(uri, () -> baseDir + "/events/" + collectorid + "/" + today + "_events.log"));

    }

}


