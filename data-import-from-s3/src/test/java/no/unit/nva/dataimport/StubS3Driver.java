package no.unit.nva.dataimport;

import static nva.commons.core.attempt.Try.attempt;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.ioutils.IoUtils;

public class StubS3Driver extends S3Driver {

    private final List<String> resourceFiles;

    public StubS3Driver(String bucketName, List<String> resourceFiles) {
        super(null, bucketName);
        this.resourceFiles = resourceFiles;
    }

    @Override
    public List<String> listFiles(Path folder) {
        return resourceFiles;
    }

    @Override
    public String getFile(String filename) {
        Stream<String> lines = fileContent(filename);
        return lines.collect(Collectors.joining(System.lineSeparator()));
    }

    public List<String> getAllIonItems() {
        return listFiles(null)
                   .stream()
                   .flatMap(this::fileContent)
                   .collect(Collectors.toList());
    }

    private Stream<String> fileContent(String filename) {
        InputStream inputStream = IoUtils.inputStreamFromResources(filename);
        if (streamIsEmpty(inputStream)) {
            throw new IllegalStateException("File does not exist or file is empty");
        }
        GZIPInputStream gzipInputStream = attempt(() -> new GZIPInputStream(inputStream)).orElseThrow();
        BufferedReader reader = new BufferedReader(new InputStreamReader(gzipInputStream));
        return reader.lines();
    }

    private boolean streamIsEmpty(InputStream inputStream) {
        return attempt(() -> inputStream.available() == 0).orElseThrow();
    }
}
