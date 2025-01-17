package no.unit.nva.publication.file.upload.restmodel;

import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.associatedartifacts.file.MutableFileMetadata;
import no.unit.nva.model.associatedartifacts.file.PublisherVersion;

@JsonTypeInfo(use = Id.NAME, property = "type")
public record UpdateFileRequest(String type, UUID identifier, URI license, PublisherVersion publisherVersion,
                                Instant embargoDate, String legalNote)
    implements JsonSerializable, MutableFileMetadata {

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

    public File toFile() {
        return attempt(() -> JsonUtils.dtoObjectMapper.readValue(this.toJsonString(), File.class)).orElseThrow();
    }
}
