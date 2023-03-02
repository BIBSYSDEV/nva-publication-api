package no.unit.nva.publication.events.bodies;

import static no.unit.nva.publication.events.handlers.tickets.DoiRequestEventProducer.NVA_API_DOMAIN;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;
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
    protected static final String EMPTY_EVENT_TOPIC = "empty";
    @JsonProperty(TOPIC)
    private final String topic;

    @JsonProperty(PUBLICATION_ID)
    private final URI publicationId;

    @JsonProperty(CUSTOMER_ID)
    private final URI customerId;

    @JacocoGenerated
    @JsonCreator
    public DoiMetadataUpdateEvent(
        @JsonProperty(TOPIC) String type,
        @JsonProperty(PUBLICATION_ID) URI publicationId,
        @JsonProperty(CUSTOMER_ID) URI customerId) {
        this.topic = type;
        this.publicationId = publicationId;
        this.customerId = customerId;
    }

    public static DoiMetadataUpdateEvent createUpdateDoiEvent(Publication newEntry) {
        URI publicationId = inferPublicationId(newEntry);
        URI customerId = extractCustomerId(newEntry);
        return new DoiMetadataUpdateEvent(UPDATE_DOI_EVENT_TOPIC, publicationId, customerId);
    }

    public static DoiMetadataUpdateEvent createNewDoiEvent(DoiRequest newEntry, ResourceService resourceService) {
        var publication = newEntry.toPublication(resourceService);
        URI publicationId = inferPublicationId(publication);
        URI customerId = extractCustomerId(publication);
        return new DoiMetadataUpdateEvent(REQUEST_DRAFT_DOI_EVENT_TOPIC, publicationId, customerId);
    }

    public static DoiMetadataUpdateEvent createDeleteDraftDoiEvent(DoiRequest doiRequest,
                                                                   ResourceService resourceService) {
        var publication = doiRequest.toPublication(resourceService);
        URI publicationId = inferPublicationId(publication);
        URI customerId = extractCustomerId(publication);
        return new DoiMetadataUpdateEvent(DELETE_DRAFT_DOI_EVENT_TOPIC, publicationId, customerId);
    }

    public static DoiMetadataUpdateEvent empty() {
        return new DoiMetadataUpdateEvent(EMPTY_EVENT_TOPIC, null, null);
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getTopic(), getPublicationId(), getCustomerId());
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
