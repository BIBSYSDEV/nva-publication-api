package no.unit.nva.publication.events.bodies;

import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;

public class DoiMetadataUpdateEvent {

    public static final String UPDATE_DOI_EVENT_TOPIC = "PublicationService.Doi.UpdateRequest";
    public static final String TOPIC = "topic";
    public static final String PUBLICATION_ID = "publicationId";
    public static final String CUSTOMER_ID = "customerId";
    public static final String DOI = "doi";
    private static final String DUPLICATE_OF = "duplicateOf";
    protected static final String EMPTY_EVENT_TOPIC = "empty";
    @JsonProperty(TOPIC)
    private final String topic;

    @JsonProperty(PUBLICATION_ID)
    private final URI publicationId;

    @JsonProperty(CUSTOMER_ID)
    private final URI customerId;

    @JsonProperty(DOI)
    private final URI doi;

    @JsonProperty(DUPLICATE_OF)
    private final URI duplicateOf;

    @JacocoGenerated
    @JsonCreator
    public DoiMetadataUpdateEvent(
        @JsonProperty(TOPIC) String type,
        @JsonProperty(PUBLICATION_ID) URI publicationId,
        @JsonProperty(CUSTOMER_ID) URI customerId,
        @JsonProperty(DOI) URI doi,
        @JsonProperty(DUPLICATE_OF) URI duplicateOf) {
        this.topic = type;
        this.publicationId = publicationId;
        this.customerId = customerId;
        this.doi = doi;
        this.duplicateOf = duplicateOf;
    }

    public static DoiMetadataUpdateEvent createUpdateDoiEvent(Publication newEntry, String apiHost) {
        URI publicationId = inferPublicationId(newEntry, apiHost);
        URI customerId = extractCustomerId(newEntry);
        URI doi = extractDoi(newEntry);
        URI duplicateOf  = newEntry.getDuplicateOf();
        return new DoiMetadataUpdateEvent(
            UPDATE_DOI_EVENT_TOPIC,
            publicationId,
            customerId,
            doi,
            duplicateOf);
    }

    public static DoiMetadataUpdateEvent empty() {
        return new DoiMetadataUpdateEvent(EMPTY_EVENT_TOPIC, null, null, null, null);
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getTopic(), getPublicationId(), getCustomerId(), getDoi(), getDuplicateOf());
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DoiMetadataUpdateEvent that)) {
            return false;
        }
        return Objects.equals(getTopic(), that.getTopic())
               && Objects.equals(getCustomerId(), that.getCustomerId())
               && Objects.equals(getDoi(), that.getDoi())
               && Objects.equals(getPublicationId(), that.getPublicationId())
               && Objects.equals(getDuplicateOf(), that.getDuplicateOf());
    }

    @JacocoGenerated
    public String getTopic() {
        return topic;
    }

    @JacocoGenerated
    public URI getCustomerId() {
        return customerId;
    }

    @JacocoGenerated
    public URI getPublicationId() {
        return publicationId;
    }

    @JacocoGenerated
    public URI getDoi() {
        return doi;
    }

    @JacocoGenerated
    public URI getDuplicateOf() {
        return duplicateOf;
    }

    public String toJsonString() {
        return attempt(() -> JsonUtils.dtoObjectMapper.writeValueAsString(this)).orElseThrow();
    }

    private static URI extractDoi(Publication newEntry) {
        return newEntry.getDoi();
    }

    private static URI extractCustomerId(Publication publication) {
        return Optional.ofNullable(publication.getPublisher()).map(Organization::getId).orElse(null);
    }

    private static URI inferPublicationId(Publication newEntry, String apiHost) {
        return UriWrapper.fromHost(apiHost)
                   .addChild("publication")
                   .addChild(newEntry.getIdentifier().toString())
                   .getUri();
    }
}
