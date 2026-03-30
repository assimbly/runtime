package org.assimbly.dil.store;

import org.assimbly.util.BaseDirectory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class DILStoreFactory {

    private static final String PERSISTENT_STORE_PROPERTY = "dil.persistent.store";

    public static DILStore create() {
        if (Boolean.getBoolean(PERSISTENT_STORE_PROPERTY)) {
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
        return cacheDir.resolve("flowsMap.db").toFile();
    }
}