package no.unit.nva.publication;

import static nva.commons.core.attempt.Try.attempt;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.s3.UnixPath;
import nva.commons.core.ioutils.IoUtils;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

public class StubS3Driver extends S3Driver {

    private final List<String> filesInBucket;

    public StubS3Driver(String bucketName, List<String> filesInBucket) {
        super(null, bucketName);
        this.filesInBucket = filesInBucket;
    }

    @Override
    public List<String> listFiles(UnixPath folder) {
        return filesInBucket;
    }

    @Override
    public String getFile(String filename) {
        List<String> lines = fileContent(filename);
        return String.join(System.lineSeparator(), lines);
    }

    public List<String> getAllIonItems() {
        return listFiles(null)
                   .stream()
                   .map(this::fileContent)
                   .flatMap(Collection::stream)
                   .collect(Collectors.toList());
    }

    private List<String> fileContent(String filename) {
        try (InputStream inputStream = attempt(() -> IoUtils.inputStreamFromResources(filename))
                                           .orElseThrow(fail -> fileNotFoundException());
            GZIPInputStream gzipInputStream = attempt(() -> new GZIPInputStream(inputStream)).orElseThrow();
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(gzipInputStream, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private NoSuchKeyException fileNotFoundException() {
        return NoSuchKeyException.builder().message("File does not exist or file is empty").build();
    }
}
