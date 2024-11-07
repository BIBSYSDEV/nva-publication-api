package no.unit.nva.api;

import static no.unit.nva.DatamodelConfig.dataModelObjectMapper;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.databind.JsonNode;
import no.unit.nva.model.Publication;
import nva.commons.core.Environment;
import nva.commons.core.paths.UriWrapper;

public class PublicationContext {

    public static JsonNode getContext(Publication publication) {
        return attempt(() -> dataModelObjectMapper.readTree(
            Publication.getJsonLdContext(UriWrapper.fromHost(new Environment().readEnv("API_HOST")).getUri()))).orElseThrow();
    }
}
