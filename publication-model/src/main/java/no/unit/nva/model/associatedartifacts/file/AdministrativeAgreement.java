package no.unit.nva.model.associatedartifacts.file;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.time.Instant;
import java.util.UUID;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonTypeName(AdministrativeAgreement.TYPE)
public class AdministrativeAgreement extends File {

    public static final String TYPE = "UnpublishableFile";
    private static final String NO_LEGAL_NOTE = null;

    /**
     * Constructor for no.unit.nva.file.model.File objects. A file object is valid if it has a license or is explicitly
     * marked as an administrative agreement.
     * @param identifier              A UUID that identifies the file in storage
     * @param name                    The original name of the file
     * @param mimeType                The mimetype of the file
     * @param size                    The size of the file
     * @param license                 The license for the file, may be null if and only if the file is an administrative
     *                                agreement
     * @param administrativeAgreement True if the file is an administrative agreement
     * @param publishedVersion        Accepts boolean, enum or null. True if the file owner has publisher authority
     * @param embargoDate             The date after which the file may be published
     * @param uploadDetails           Information regarding who and when inserted the file into the system
     */
    @JsonCreator
    public AdministrativeAgreement(
        @JsonProperty(IDENTIFIER_FIELD) UUID identifier,
        @JsonProperty(NAME_FIELD) String name,
        @JsonProperty(MIME_TYPE_FIELD) String mimeType,
        @JsonProperty(SIZE_FIELD) Long size,
        @JsonProperty(LICENSE_FIELD) Object license,
        @JsonProperty(ADMINISTRATIVE_AGREEMENT_FIELD) boolean administrativeAgreement,
        @JsonProperty(PUBLISHER_VERSION_FIELD) PublisherVersion publishedVersion,
        @JsonProperty(EMBARGO_DATE_FIELD) Instant embargoDate,
        @JsonProperty(UPLOAD_DETAILS_FIELD) UploadDetails uploadDetails) {
        super(identifier, name, mimeType, size, license, administrativeAgreement, publishedVersion,
              embargoDate, null,  NO_LEGAL_NOTE, null,
              uploadDetails);
    }
    
    @Override
    public boolean isVisibleForNonOwner() {
        return false;
    }
    
    @Override
    public UnpublishedFile toUnpublishedFile() {
        throw new IllegalStateException("Cannot make an unpublishable file publishable");
    }
    
    @Override
    public PublishedFile toPublishedFile() {
        throw new IllegalStateException("Cannot make an unpublishable file publishable");
    }

    @Override
    public Builder copy() {
        return builder()
                   .withIdentifier(this.getIdentifier())
                   .withName(this.getName())
                   .withMimeType(this.getMimeType())
                   .withSize(this.getSize())
                   .withLicense(this.getLicense())
                   .withAdministrativeAgreement(this.isAdministrativeAgreement())
                   .withPublisherVersion(this.getPublisherVersion())
                   .withEmbargoDate(this.getEmbargoDate().orElse(null))
                   .withUploadDetails(this.getUploadDetails());
    }
}
