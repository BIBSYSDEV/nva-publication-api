package no.unit.nva.publication.s3imports;

import static nva.commons.core.attempt.Try.attempt;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Objects;
import nva.commons.core.paths.UnixPath;

public class FileContent {
    
    private final UnixPath filename;
    private final ByteBuffer fileContent;
    
    public FileContent(UnixPath filename, InputStream fileContent) {
        this.filename = filename;
        this.fileContent = attempt(() -> ByteBuffer.wrap(fileContent.readAllBytes())).orElseThrow();
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(getFilename(), getFileContent());
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FileContent)) {
            return false;
        }
        FileContent that = (FileContent) o;
        return Objects.equals(getFilename(), that.getFilename())
               && Objects.equals(getFileContent(), that.getFileContent());
    }
    
    public UnixPath getFilename() {
        return filename;
    }
    
    public ByteBuffer getFileContent() {
        return fileContent;
    }
    
    public Map<String, ByteBuffer> toMap() {
        return Map.of(filename.toString(), fileContent);
    }
}
