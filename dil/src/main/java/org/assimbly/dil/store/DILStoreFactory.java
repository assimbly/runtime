package org.assimbly.dil.store;

import org.assimbly.util.BaseDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class DILStoreFactory {

    private static final Logger log = LoggerFactory.getLogger(DILStoreFactory.class);

    private static final String ASSIMBLY_CACHE_PROPERTY = System.getenv("ASSIMBLY_CACHE");

    public static DILStore create() {

        if (Boolean.parseBoolean(ASSIMBLY_CACHE_PROPERTY)) {
            log.info("Create persistent store. Storing DIL objects like flows and collectors");
            return createPersistentStore();
        }
        return new DILMemoryStore();
    }

    private static DILPersistentStore createPersistentStore() {
        File dbFile = resolveDatabaseFile();
        return new DILPersistentStore(dbFile);
    }

    private static File resolveDatabaseFile() {
        Path cacheDir = Path.of(BaseDirectory.getInstance().getBaseDirectory(), "cache");
        try {
            Files.createDirectories(cacheDir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create cache directory: " + cacheDir, e);
        }
        return cacheDir.resolve("dil.db").toFile();
    }

}