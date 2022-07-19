package no.unit.nva.publication;

import static nva.commons.core.attempt.Try.attempt;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.ioutils.IoUtils;
import nva.commons.core.paths.UnixPath;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

@JacocoGenerated
public class StubS3Driver extends S3Driver {
    
    private final List<UnixPath> filesInBucket;
    
    public StubS3Driver(String bucketName, List<UnixPath> filesInBucket) {
        super(null, bucketName);
        this.filesInBucket = new ArrayList<>(filesInBucket);
    }
    
    @Override
    public List<UnixPath> listAllFiles(UnixPath folder) {
        return filesInBucket;
    }
    
    @Override
    public String getFile(UnixPath filename) {
        List<String> lines = fileContent(filename);
        return String.join(System.lineSeparator(), lines);
    }
    
    public List<String> getAllIonItems() {
        return listAllFiles(null)
            .stream()
            .map(this::fileContent)
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
    }
    
    private List<String> fileContent(UnixPath filename) {
        try (InputStream inputStream = attempt(() -> IoUtils.inputStreamFromResources(filename.toString()))
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
