package org.assimbly.dil.event.store.impl;

import org.apache.commons.io.FileUtils;
import org.assimbly.util.BaseDirectory;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

public class FileStore {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final String baseDir = BaseDirectory.getInstance().getBaseDirectory();
    private final String collectorId;
    private final String uri;

    public FileStore(String collectorId, org.assimbly.dil.event.domain.Store store) {
        this.collectorId = collectorId;
        this.uri = store.getUri();
    }

    public void store(String json) {
        List<String> line = Collections.singletonList(json);
        try {
            FileUtils.writeLines(getFile(), line, true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Without an uri the events go to a file per day, so the date is resolved on every write
    private File getFile() {
        if (uri != null) {
            return new File(uri);
        }

        // Use LocalDate with explicit ZoneId to resolve both java.time and S8688 warnings
        String today = LocalDate.now(ZoneId.systemDefault()).format(DATE_FORMATTER);

        return new File(baseDir + "/events/" + collectorId + "/" + today + "_events.log");
    }

}
