package no.unit.nva.model.associatedartifacts.file;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.time.Instant;
import java.util.UUID;
import no.unit.nva.model.associatedartifacts.PublicAssociatedArtifact;
import no.unit.nva.model.associatedartifacts.RightsRetentionStrategy;

@SuppressWarnings("PMD.ExcessiveParameterList")
@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonTypeName(PendingOpenFile.TYPE)
public class PendingOpenFile extends File implements PendingFile<OpenFile>, PublicAssociatedArtifact {
    public static final String TYPE = "PendingOpenFile";

    /**
     * Constructor for no.unit.nva.file.model.File objects. A file object is valid if it has a license or is explicitly
     * marked as an administrative agreement.
     *
     * @param identifier              A UUID that identifies the file in storage
     * @param name                    The original name of the file
     * @param mimeType                The mimetype of the file
     * @param size                    The size of the file
     * @param license                 The license for the file, may be null if and only if the file is an administrative
     *                                agreement
     * @param administrativeAgreement True if the file is an administrative agreement
     * @param publisherVersion        True if the file owner has publisher authority
     * @param embargoDate             The date after which the file may be published
     * @param rightsRetentionStrategy The rights retention strategy for the file
     * @param legalNote
     * @param uploadDetails           Information regarding who and when inserted the file into the system
     */
    @JsonCreator
    public PendingOpenFile(
        @JsonProperty(IDENTIFIER_FIELD) UUID identifier,
        @JsonProperty(NAME_FIELD) String name,
        @JsonProperty(MIME_TYPE_FIELD) String mimeType,
        @JsonProperty(SIZE_FIELD) Long size,
        @JsonProperty(LICENSE_FIELD) Object license,
        @JsonProperty(ADMINISTRATIVE_AGREEMENT_FIELD) boolean administrativeAgreement,
        @JsonProperty(PUBLISHER_VERSION_FIELD) PublisherVersion publisherVersion,
        @JsonProperty(EMBARGO_DATE_FIELD) Instant embargoDate,
        @JsonProperty(RIGHTS_RETENTION_STRATEGY) RightsRetentionStrategy rightsRetentionStrategy,
        @JsonProperty(LEGAL_NOTE_FIELD) String legalNote,
        @JsonProperty(UPLOAD_DETAILS_FIELD) UploadDetails uploadDetails) {
        super(identifier, name, mimeType, size, license, administrativeAgreement, publisherVersion, embargoDate,
              rightsRetentionStrategy, legalNote, null, uploadDetails);
    }

    @Override
    public boolean isVisibleForNonOwner() {
        return false;
    }

    @Override
    @JsonIgnore
    public boolean needsApproval() {
        return true;
    }

    @Override
    public RejectedFile reject() {
        return new RejectedFile(getIdentifier(), getName(), getMimeType(), getSize(), getLicense(),
                                isAdministrativeAgreement(), getPublisherVersion(), getEmbargoDate().orElse(null),
                                getRightsRetentionStrategy(), getLegalNote(), getUploadDetails());
    }

    @Override
    public OpenFile approve() {
        return new OpenFile(getIdentifier(), getName(), getMimeType(), getSize(), getLicense(),
                                isAdministrativeAgreement(), getPublisherVersion(), getEmbargoDate().orElse(null),
                                getRightsRetentionStrategy(), getLegalNote(), Instant.now(), getUploadDetails());
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
                   .withRightsRetentionStrategy(this.getRightsRetentionStrategy())
                   .withLegalNote(this.getLegalNote())
                   .withUploadDetails(this.getUploadDetails());
    }
}
