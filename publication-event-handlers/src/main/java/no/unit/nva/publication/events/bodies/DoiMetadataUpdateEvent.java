package no.unit.nva.publication.events.bodies;

import static no.unit.nva.publication.events.handlers.tickets.DoiRequestEventProducer.NVA_API_DOMAIN;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;

public class DoiMetadataUpdateEvent {

    public static final String REQUEST_DRAFT_DOI_EVENT_TOPIC = "PublicationService.Doi.CreationRequest";
    public static final String UPDATE_DOI_EVENT_TOPIC = "PublicationService.Doi.UpdateRequest";
    public static final String DELETE_DRAFT_DOI_EVENT_TOPIC = "PublicationService.Doi.DeleteDraftRequest";
    public static final String TOPIC = "topic";
    public static final String PUBLICATION_ID = "publicationId";
    public static final String CUSTOMER_ID = "customerId";
    public static final String DOI = "doi";

    public static final String ITEM = "item";
    protected static final String EMPTY_EVENT_TOPIC = "empty";
    @JsonProperty(TOPIC)
    private final String topic;

    @JsonProperty(PUBLICATION_ID)
    private final URI publicationId;

    @JsonProperty(CUSTOMER_ID)
    private final URI customerId;

    @JsonProperty(DOI)
    private final URI doi;

    @Deprecated
    @JsonProperty(ITEM)
    private final Publication item;

    @JacocoGenerated
    @JsonCreator
    public DoiMetadataUpdateEvent(
        @JsonProperty(TOPIC) String type,
        @JsonProperty(PUBLICATION_ID) URI publicationId,
        @JsonProperty(CUSTOMER_ID) URI customerId,
        @JsonProperty(ITEM) Publication publication,
        @JsonProperty(DOI) URI doi) {
        this.topic = type;
        this.item = publication;
        this.publicationId = publicationId;
        this.customerId = customerId;
        this.doi = doi;
    }

    public static DoiMetadataUpdateEvent createUpdateDoiEvent(Publication newEntry) {
        URI publicationId = inferPublicationId(newEntry);
        URI customerId = extractCustomerId(newEntry);
        URI doi = extractDoi(newEntry);
        return new DoiMetadataUpdateEvent(UPDATE_DOI_EVENT_TOPIC, publicationId, customerId, newEntry, doi);
    }

    public static DoiMetadataUpdateEvent createDeleteDraftDoiEvent(DoiRequest doiRequest,
                                                                   ResourceService resourceService) {
        var publication = doiRequest.toPublication(resourceService);
        URI publicationId = inferPublicationId(publication);
        URI customerId = extractCustomerId(publication);
        URI doi = extractDoi(publication);
        return new DoiMetadataUpdateEvent(DELETE_DRAFT_DOI_EVENT_TOPIC, publicationId, customerId, publication, doi);
    }

    public static DoiMetadataUpdateEvent empty() {
        return new DoiMetadataUpdateEvent(EMPTY_EVENT_TOPIC, null, null, null, null);
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getTopic(), getPublicationId(), getCustomerId(), getDoi());
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DoiMetadataUpdateEvent)) {
            return false;
        }
        DoiMetadataUpdateEvent that = (DoiMetadataUpdateEvent) o;
        return Objects.equals(getTopic(), that.getTopic())
               && Objects.equals(getCustomerId(), that.getCustomerId())
               && Objects.equals(getDoi(), that.getDoi())
               && Objects.equals(getPublicationId(), that.getPublicationId());
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
    public Publication getItem() {
        return item;
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

    private static URI inferPublicationId(Publication newEntry) {
        return UriWrapper.fromUri(NVA_API_DOMAIN)
                   .addChild("publication")
                   .addChild(newEntry.getIdentifier().toString())
                   .getUri();
    }
}
