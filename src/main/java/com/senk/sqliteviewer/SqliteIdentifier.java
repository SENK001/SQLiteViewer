package com.senk.sqliteviewer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public final class SqliteIdentifier {

    private static final byte[] SQLITE_HEADER = "SQLite format 3\0".getBytes();
    private static final int HEADER_LENGTH = 16;

    private SqliteIdentifier() {
    }

    public static boolean isSqliteFile(File file) {
        if (file == null || !file.isFile() || !file.canRead() || file.length() < HEADER_LENGTH) {
            return false;
        }
        byte[] buffer = new byte[HEADER_LENGTH];
        try (FileInputStream fis = new FileInputStream(file)) {
            int bytesRead = fis.read(buffer);
            if (bytesRead < HEADER_LENGTH) {
                return false;
            }
            for (int i = 0; i < HEADER_LENGTH; i++) {
                if (buffer[i] != SQLITE_HEADER[i]) {
                    return false;
                }
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
