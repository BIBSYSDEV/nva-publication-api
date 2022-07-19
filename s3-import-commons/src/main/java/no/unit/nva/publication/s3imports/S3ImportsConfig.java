package no.unit.nva.publication.s3imports;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.unit.nva.commons.json.JsonUtils;

public final class S3ImportsConfig {
    
    public static final ObjectMapper s3ImportsMapper = JsonUtils.dtoObjectMapper;
    
    private S3ImportsConfig() {
    
    }
}
