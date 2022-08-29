package no.unit.nva.publication.publishingrequest;

import static no.unit.nva.publication.PublicationServiceConfig.PUBLICATION_HOST_URI;
import static no.unit.nva.publication.PublicationServiceConfig.SUPPORT_CASE_PATH;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.Objects;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.TicketStatus;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;

public class PublishingRequestCaseDto implements PublishingRequestCaseDtoInterface {
    
    public static final String MESSAGES = "messages";
    public static final String ID = "id";
    public static final String JSON_LD_CONTEXT =
        "https://bibsysdev.github.io/src/publication-service/publishing-request-context.json";
    public static final String STATUS = "status";
    
    @JsonProperty(ID)
    private final URI id;
    @JsonProperty(STATUS)
    private final TicketStatus status;
    
    @JsonCreator
    public PublishingRequestCaseDto(@JsonProperty(ID) URI caseId,
                                    @JsonProperty(STATUS) TicketStatus status) {
        this.id = caseId;
        this.status = status;
    }
    
    public static PublishingRequestCaseDto createResponseObject(PublishingRequestCase request) {
        var id = createId(request);
        return new PublishingRequestCaseDto(id, request.getStatus());
    }
    
    public static URI calculateId(SortableIdentifier publicationIdentifier,
                                  SortableIdentifier publishingRequestIdentifier) {
        return UriWrapper.fromUri(PUBLICATION_HOST_URI)
            .addChild(publicationIdentifier.toString())
            .addChild(SUPPORT_CASE_PATH)
            .addChild(publishingRequestIdentifier.toString())
            .getUri();
    }
    
    @JsonProperty(MESSAGES)
    public URI getMessagesEndpoint() {
        return UriWrapper.fromUri(id).addChild(MESSAGES).getUri();
    }
    
    @JsonProperty("@context")
    public String getContext() {
        return JSON_LD_CONTEXT;
    }
    
    public URI getId() {
        return this.id;
    }
    
    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getId());
    }
    
    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PublishingRequestCaseDto)) {
            return false;
        }
        PublishingRequestCaseDto that = (PublishingRequestCaseDto) o;
        return Objects.equals(getId(), that.getId());
    }
    
    public TicketStatus getStatus() {
        return status;
    }
    
    private static URI createId(PublishingRequestCase request) {
        return calculateId(request.getResourceIdentifier(), request.getIdentifier());
    }
}
