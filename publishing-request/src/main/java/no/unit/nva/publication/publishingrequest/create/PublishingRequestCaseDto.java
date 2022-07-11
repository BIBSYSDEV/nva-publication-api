package no.unit.nva.publication.publishingrequest.create;

import static no.unit.nva.publication.PublicationServiceConfig.API_HOST;
import static no.unit.nva.publication.PublicationServiceConfig.PUBLICATION_PATH;
import static no.unit.nva.publication.PublicationServiceConfig.SUPPORT_CASE_PATH;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import java.util.Objects;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.storage.model.PublishingRequestCase;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonTypeName(PublishingRequestCaseDto.TYPE)
public class PublishingRequestCaseDto {

    public static final String MESSAGES = "messages";
    public static final String TYPE = "PublishingRequestCase";
    public static final String ID = "id";
    public static final String JSON_LD_CONTEXT =
        "https://bibsysdev.github.io/src/publication-service/publishing-request-context.json";

    @JsonProperty(ID)
    private final URI id;

    @JsonCreator
    public PublishingRequestCaseDto(@JsonProperty(ID) URI caseId) {
        this.id = caseId;
    }

    public static PublishingRequestCaseDto create(PublishingRequestCase request) {
        return create(request.getIdentifier(), request.getResourceIdentifier());
    }

    public static PublishingRequestCaseDto create(SortableIdentifier requestIdentifier,
                                                  SortableIdentifier publicationIdentifier) {
        var id = UriWrapper.fromUri(API_HOST)
            .addChild(PUBLICATION_PATH)
            .addChild(publicationIdentifier.toString())
            .addChild(SUPPORT_CASE_PATH)
            .addChild(requestIdentifier.toString())
            .getUri();
        return new PublishingRequestCaseDto(id);
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

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getId());
    }
}
