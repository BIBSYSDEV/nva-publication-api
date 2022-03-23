package no.unit.nva.publication.service.impl;

import static java.util.Objects.nonNull;
import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemUtils;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import no.unit.nva.model.contexttypes.UnconfirmedPublisher;

public final class CorrectParsingErrors {

    public static final String DATA_ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_CONTEXT =
        "/data/entityDescription/reference/publicationContext";
    public static final String PUBLISHER = "publisher";
    public static final String TYPE = "type";
    public static final String NAME = "name";

    private CorrectParsingErrors() {

    }

    public static Map<String, AttributeValue> apply(Map<String, AttributeValue> item) {
        var json = ItemUtils.toItem(item).toJSON();
        var objectNode = (ObjectNode) attempt(() -> dtoObjectMapper.readTree(json)).orElseThrow();
        var newObjectNode = correctParsingErrors(objectNode);
        var jsonAgain = attempt(() -> dtoObjectMapper.writeValueAsString(newObjectNode)).orElseThrow();
        return ItemUtils.toAttributeValues(Item.fromJSON(jsonAgain));
    }

    public static ObjectNode correctParsingErrors(ObjectNode objectNode) {
        replacePublisherStringWithPublisherObject(objectNode);

        return objectNode;
    }

    private static void replacePublisherStringWithPublisherObject(ObjectNode objectNode) {
        var publicationContextObjectNode = objectNode.at(DATA_ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_CONTEXT);
        if (!publicationContextObjectNode.isMissingNode()) {
            var publisherNode = publicationContextObjectNode.get(PUBLISHER);
            if (nonNull(publisherNode) && publisherNode.isTextual()) {
                ObjectNode publisherObjectNode = dtoObjectMapper.createObjectNode();
                publisherObjectNode.put(TYPE, UnconfirmedPublisher.class.getSimpleName());
                publisherObjectNode.put(NAME, publisherNode.textValue());

                ((ObjectNode) publicationContextObjectNode).replace(PUBLISHER, publisherObjectNode);
            }
        }
    }
}
