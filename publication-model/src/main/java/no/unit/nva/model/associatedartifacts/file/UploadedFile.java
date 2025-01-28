package no.unit.nva.model.associatedartifacts.file;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.UUID;
import no.unit.nva.model.associatedartifacts.RightsRetentionStrategy;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonTypeName(UploadedFile.TYPE)
public class UploadedFile extends File {

    public static final String TYPE = "UploadedFile";

    @JsonCreator
    public UploadedFile(@JsonProperty(IDENTIFIER_FIELD) UUID identifier,
                        @JsonProperty(NAME_FIELD) String name,
                        @JsonProperty(MIME_TYPE_FIELD) String mimeType,
                        @JsonProperty(SIZE_FIELD) Long size,
                        @JsonProperty(RIGHTS_RETENTION_STRATEGY) RightsRetentionStrategy rightsRetentionStrategy,
                        @JsonProperty(UPLOAD_DETAILS_FIELD) UploadDetails uploadDetails) {
        super(identifier, name, mimeType, size, null, null, null, rightsRetentionStrategy, null, null, uploadDetails);
    }

    @Override
    public boolean isVisibleForNonOwner() {
        return false;
    }

    @Override
    public Builder copy() {
        return builder()
                   .withIdentifier(this.getIdentifier())
                   .withName(this.getName())
                   .withMimeType(this.getMimeType())
                   .withSize(this.getSize())
                   .withLicense(this.getLicense())
                   .withPublisherVersion(this.getPublisherVersion())
                   .withEmbargoDate(this.getEmbargoDate().orElse(null))
                   .withRightsRetentionStrategy(this.getRightsRetentionStrategy())
                   .withLegalNote(this.getLegalNote())
                   .withUploadDetails(this.getUploadDetails());
    }

    @Override
    public boolean canBeConvertedTo(File file) {
        return switch (file) {
            case UploadedFile ignore -> true;
            case PendingInternalFile ignore -> true;
            case PendingOpenFile ignore -> true;
            case HiddenFile ignore -> true;
            default -> false;
        };
    }
}

