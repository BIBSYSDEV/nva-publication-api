package no.unit.nva.publication.s3imports;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import nva.commons.core.JacocoGenerated;

public class FileImportUtils {

    @JacocoGenerated
    public FileImportUtils() {
    }

    public static String timestampToString(Instant timestamp) {
        return DateTimeFormatter.ISO_INSTANT.format(timestamp);
    }
}
