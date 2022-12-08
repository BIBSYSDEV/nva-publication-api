package no.sikt.nva.brage.migration.record.content;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

@JacocoGenerated
public class ResourceContent {

    private List<ContentFile> contentFiles;

    @JsonCreator
    public ResourceContent(@JsonProperty("contentFiles") List<ContentFile> contentFiles) {
        this.contentFiles = contentFiles;
    }

    @JsonProperty("contentFiles")
    public List<ContentFile> getContentFiles() {
        return contentFiles;
    }

    @JacocoGenerated
    public void setContentFiles(List<ContentFile> contentFiles) {
        this.contentFiles = contentFiles;
    }

    public ContentFile getContentFileByFilename(String filename) {
        return contentFiles.stream()
                   .filter(contentFile -> contentFile.getFilename().equals(filename))
                   .findAny()
                   .orElse(null);
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(contentFiles);
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ResourceContent that = (ResourceContent) o;
        return Objects.equals(contentFiles, that.contentFiles);
    }

    public enum BundleType {
        ORIGINAL("ORIGINAL"),
        TEXT("TEXT"),
        THUMBNAIL("THUMBNAIL"),
        LICENSE("LICENSE"),
        CCLICENSE("CC-LICENSE"),
        ORE("ORE"),
        SWORD("SWORD"),
        METADATA("METADATA");

        private final String value;

        @JacocoGenerated
        BundleType(String value) {
            this.value = value;
        }

        @JacocoGenerated
        public String getValue() {
            return value;
        }
    }
}
