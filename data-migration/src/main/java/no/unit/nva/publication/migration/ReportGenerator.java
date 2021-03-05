package no.unit.nva.publication.migration;

import static java.util.Objects.nonNull;
import static nva.commons.core.attempt.Try.attempt;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.JsonUtils;

public class ReportGenerator {

    private static final String FAILURES_FILE_PREFIX = "failures_";
    private static final String JSON_SUFFIX = ".json";
    private static final String DOUBLE_QUOTES = "\"";
    private static final String EMPTY_STRING = "";
    private static final String DIFFERENCES_FILE_PREFIX = "differences_";
    private final File failuresReportFile;
    private final File differencesReportFile;
    private final List<ResourceUpdate> resourceUpdates;

    public ReportGenerator(List<ResourceUpdate> resourceUpdates) {
        this.resourceUpdates = resourceUpdates;
        this.failuresReportFile = newFailuresFile(defaultFailuresFile());
        this.differencesReportFile = newDifferencesFile(defaultDifferencesFile());
    }

    public String reportFailures() {
        var failures = resourceUpdates.stream()
                           .filter(ResourceUpdate::isFailure)
                           .collect(Collectors.toList());
        return attempt(() -> JsonUtils.objectMapperWithEmpty.writeValueAsString(failures))
                   .orElseThrow();
    }

    public String reportDifferences() {
        var failures = resourceUpdates.stream()
                           .filter(ResourceUpdate::isSuccess)
                           .filter(publicationUpdate -> !publicationUpdate.versionsAreEquivalent())
                           .collect(Collectors.toList());
        return attempt(() -> JsonUtils.objectMapperWithEmpty.writeValueAsString(failures))
                   .orElseThrow();
    }

    public void writeFailures() throws IOException {
        String json = reportFailures();
        writeToFile(failuresReportFile, json);
    }

    public void writeDifferences() throws IOException {
        String json = reportDifferences();
        writeToFile(differencesReportFile, json);
    }

    private static File defaultFailuresFile() {
        return new File(failuresFilename());
    }

    private static File defaultDifferencesFile() {
        return new File(differencesFilename());
    }

    private static String failuresFilename() {
        String instant = nowAsString();
        return formatFilename(instant, FAILURES_FILE_PREFIX);
    }

    private static String formatFilename(String instant, String failuresFilePrefix) {
        return failuresFilePrefix + instant + JSON_SUFFIX;
    }

    @JacocoGenerated
    private static String differencesFilename() {
        String instant = nowAsString();
        return formatFilename(instant, DIFFERENCES_FILE_PREFIX);
    }

    private static String nowAsString() {
        return attempt(() -> JsonUtils.objectMapperWithEmpty.writeValueAsString(Instant.now()))
                   .map(ReportGenerator::removeDoubleQuotes)
                   .orElseThrow();
    }

    private static String removeDoubleQuotes(String str) {
        return str.replaceAll(DOUBLE_QUOTES, EMPTY_STRING);
    }

    private File newFailuresFile(File failuresReportFile) {
        return nonNull(failuresReportFile) ? failuresReportFile : defaultFailuresFile();
    }

    private File newDifferencesFile(File differencesFile) {
        return nonNull(differencesFile) ? differencesFile : defaultDifferencesFile();
    }

    private void writeToFile(File file, String json) throws IOException {
        try (Writer writer = new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(file.toPath())))) {
            writer.write(json);
            writer.flush();
        }
    }
}
