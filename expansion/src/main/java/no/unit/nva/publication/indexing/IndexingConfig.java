package no.unit.nva.publication.indexing;

import com.fasterxml.jackson.databind.ObjectMapper;
import nva.commons.core.Environment;
import nva.commons.core.JsonUtils;

public final class IndexingConfig {

    public static final Environment ENVIRONMENT = new Environment();
    public static final ObjectMapper indexingMapper = JsonUtils.dtoObjectMapper;
    private IndexingConfig() {

    }
}
