package no.unit.nva.model.associatedartifacts.file;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.time.Instant;
import java.util.Objects;
import no.unit.nva.model.Username;
import nva.commons.core.JacocoGenerated;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonTypeName(UploadDetails.TYPE)
public class UploadDetails {

    public static final String TYPE = "UploadDetails";
    private static final String UPLOADED_BY = "uploadedBy";
    private static final String UPLOADED_DATE = "uploadedDate";

    @JsonProperty(UPLOADED_BY)
    private final Username uploadedBy;

    @JsonProperty(UPLOADED_DATE)
    private final Instant uploadedDate;

    /**
     * Constructor for no.unit.nva.file.model.Inserted.
     *
     * @param uploadedBy        The person or job that inserted the file into the system
     * @param uploadedDate      The date which the file was inserted into the system
     */
    @JsonCreator
    public UploadDetails(@JsonProperty(UPLOADED_BY) Username uploadedBy,
                         @JsonProperty(UPLOADED_DATE) Instant uploadedDate) {
        this.uploadedBy = uploadedBy;
        this.uploadedDate = uploadedDate;
    }

    @JacocoGenerated
    public Username getUploadedBy() {
        return uploadedBy;
    }

    @JacocoGenerated
    public Instant getUploadedDate() {
        return uploadedDate;
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
        UploadDetails uploadDetails = (UploadDetails) o;
        return Objects.equals(uploadedBy, uploadDetails.uploadedBy) && Objects.equals(uploadedDate,
                                                                                      uploadDetails.uploadedDate);
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(uploadedBy, uploadedDate);
    }
}
