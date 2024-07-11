package no.unit.nva.api;

import static no.unit.nva.DatamodelConfig.dataModelObjectMapper;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.databind.JsonNode;
import no.unit.nva.model.Publication;

public class PublicationContext {

    public static JsonNode getContext(Publication publication) {
        return attempt(() -> dataModelObjectMapper.readTree(publication.getJsonLdContext())).orElseThrow();
    }
}
