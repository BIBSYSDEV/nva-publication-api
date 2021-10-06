package no.unit.nva.publication.s3imports;

import java.io.InputStream;
import java.util.Map;
import java.util.Objects;
import nva.commons.core.paths.UnixPath;

public class FileContent {

    private final UnixPath filename;
    private final InputStream fileContent;

    public FileContent(UnixPath filename, InputStream fileContent) {
        this.filename = filename;
        this.fileContent = fileContent;
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

    public InputStream getFileContent() {
        return fileContent;
    }

    public Map<String, InputStream> toMap() {
        return Map.of(filename.toString(), fileContent);
    }
}
