package no.unit.nva.publication.events.handlers.expandresources;

import static no.unit.nva.publication.events.handlers.PublicationEventsConfig.dynamoImageSerializerRemovingEmptyFields;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.unit.nva.expansion.model.ExpandedDoiRequest;
import no.unit.nva.expansion.model.ExpandedMessage;
import no.unit.nva.expansion.model.ExpandedResource;
import no.unit.nva.expansion.model.ExpandedResourceUpdate;
import no.unit.nva.identifiers.SortableIdentifier;
import nva.commons.core.JsonSerializable;

public class IndexDocument implements JsonSerializable {

    public static final String TYPE = "type";
    public static final String IDENTIFIER = "identifier";
    public static final String BODY = "body";
    public static final String RESOURCE_UPDATE = "resources";
    public static final String MESSAGE_UPDATE = "message";
    public static final String DOI_REQUEST_UPDATE = "doirequest";
    @JsonProperty(TYPE)
    private final String type;
    @JsonProperty(IDENTIFIER)
    private final SortableIdentifier identifier;
    @JsonProperty(BODY)
    private final ExpandedResourceUpdate body;

    public IndexDocument(String type,
                         SortableIdentifier identifier,
                         ExpandedResourceUpdate body) {

        this.type = type;
        this.identifier = identifier;
        this.body = body;
    }

    public static IndexDocument createIndexDocument(ExpandedResourceUpdate expandedResourceUpdate) {
        IndexDocument output = null;
        if (expandedResourceUpdate instanceof ExpandedResource) {
            output = createResourceUpdate((ExpandedResource) expandedResourceUpdate);
        } else if (expandedResourceUpdate instanceof ExpandedDoiRequest) {
            output = createDoiRequestUpdate((ExpandedDoiRequest) expandedResourceUpdate);
        } else if (expandedResourceUpdate instanceof ExpandedMessage) {
            output = createMessageUpdate((ExpandedMessage) expandedResourceUpdate);
        }
        return output;
    }

    public static IndexDocument createResourceUpdate(ExpandedResource expandedResourceUpdate) {
        SortableIdentifier identifier = SortableIdentifier.fromUri(expandedResourceUpdate.getId());
        return new IndexDocument(RESOURCE_UPDATE, identifier, expandedResourceUpdate);
    }

    public static IndexDocument createDoiRequestUpdate(ExpandedDoiRequest doiRequest) {
        SortableIdentifier identifier = doiRequest.getIdentifier();
        return new IndexDocument(DOI_REQUEST_UPDATE, identifier, doiRequest);
    }

    public static IndexDocument createMessageUpdate(ExpandedMessage message) {
        SortableIdentifier identifier = message.getIdentifier();
        return new IndexDocument(MESSAGE_UPDATE, identifier, message);
    }

    public String getType() {
        return type;
    }

    public SortableIdentifier getIdentifier() {
        return identifier;
    }

    public ExpandedResourceUpdate getBody() {
        return body;
    }

    @Override
    public String toJsonString() {
        return attempt(() -> dynamoImageSerializerRemovingEmptyFields.writeValueAsString(this)).orElseThrow();
    }
}
