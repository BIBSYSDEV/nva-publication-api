package no.unit.nva.publication.delete;

import com.fasterxml.jackson.databind.ObjectMapper;
import nva.commons.core.JsonUtils;

public final class DeleteDraftPublicationTestsConfig {

    public static final ObjectMapper objectMapper = JsonUtils.dtoObjectMapper;

    private DeleteDraftPublicationTestsConfig(){

    }

}
