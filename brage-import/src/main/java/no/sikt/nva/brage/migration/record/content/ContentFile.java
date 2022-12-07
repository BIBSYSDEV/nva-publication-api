package no.sikt.nva.brage.migration.record.content;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import java.util.UUID;
import no.sikt.nva.brage.migration.record.content.ResourceContent.BundleType;
import no.sikt.nva.brage.migration.record.license.License;
import nva.commons.core.JacocoGenerated;

public class ContentFile {

    private String filename;
    private BundleType bundleType;
    private String description;
    private UUID identifier;
    private License license;
    private String embargoDate;

    @JacocoGenerated
    public ContentFile() {

    }

    @JsonCreator
    public ContentFile(@JsonProperty("fileName") String filename,
                       @JsonProperty("bundleType") BundleType bundleType,
                       @JsonProperty("description") String description,
                       @JsonProperty("identifier") UUID identifier,
                       @JsonProperty("license") License license,
                       @JsonProperty("embargoDate") String embargoDate) {
        this.filename = filename;
        this.bundleType = bundleType;
        this.description = description;
        this.identifier = identifier;
        this.license = license;
        this.embargoDate = embargoDate;
    }

    @JsonProperty("embargoDate")
    public String getEmbargoDate() {
        return embargoDate;
    }

    @JacocoGenerated
    public void setEmbargoDate(String embargoDate) {
        this.embargoDate = embargoDate;
    }

    @JsonProperty("identifier")
    public UUID getIdentifier() {
        return identifier;
    }

    @JacocoGenerated
    public void setIdentifier(UUID identifier) {
        this.identifier = identifier;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(filename, bundleType, description, identifier, license, embargoDate);
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
        ContentFile that = (ContentFile) o;
        return Objects.equals(filename, that.filename)
               && bundleType == that.bundleType
               && Objects.equals(description, that.description)
               && Objects.equals(identifier, that.identifier)
               && Objects.equals(license, that.license)
               && Objects.equals(embargoDate, that.embargoDate);
    }

    @JacocoGenerated
    @JsonProperty("filename")
    public String getFilename() {
        return filename;
    }

    @JacocoGenerated
    public void setFilename(String filename) {
        this.filename = filename;
    }

    @JsonProperty("bundleType")
    public BundleType getBundleType() {
        return bundleType;
    }

    @JacocoGenerated
    public void setBundleType(BundleType bundleType) {
        this.bundleType = bundleType;
    }

    @JsonProperty("license")
    public License getLicense() {
        return license;
    }

    @JacocoGenerated
    public void setLicense(License license) {
        this.license = license;
    }

    @JsonProperty("description")
    public String getDescription() {
        return description;
    }

    @JacocoGenerated
    public void setDescription(String description) {
        this.description = description;
    }
}
