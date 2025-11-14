package no.unit.nva.publication.model.business;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import no.unit.nva.identifiers.SortableIdentifier;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public record ResourceRelationship(SortableIdentifier parentIdentifier, SortableIdentifier childIdentifier) {

}
