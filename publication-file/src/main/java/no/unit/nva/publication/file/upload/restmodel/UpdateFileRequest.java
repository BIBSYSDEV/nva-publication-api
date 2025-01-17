package no.unit.nva.publication.file.upload.restmodel;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import no.unit.nva.model.associatedartifacts.file.MutableFileMetadata;
import no.unit.nva.model.associatedartifacts.file.PublisherVersion;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonTypeName(UpdateFileRequest.TYPE)
public record UpdateFileRequest(UUID identifier, URI license, PublisherVersion publisherVersion, Instant embargoDate,
                                String legalNote) implements MutableFileMetadata {

    public static final String TYPE = "UpdateFileRequest";

    @Override
    public URI getLicense() {
        return license;
    }

    @Override
    public PublisherVersion getPublisherVersion() {
        return publisherVersion;
    }

    @Override
    public Optional<Instant> getEmbargoDate() {
        return Optional.ofNullable(embargoDate);
    }

    @Override
    public String getLegalNote() {
        return legalNote;
    }
}
