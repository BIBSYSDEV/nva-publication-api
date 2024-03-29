package no.unit.nva.publication;

import static no.unit.nva.publication.PublicationServiceConfig.dtoObjectMapper;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Optional;
import no.unit.nva.publication.utils.JsonLdContextUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class JsonLdContextUtilTest {
    
    public static final String MISSING_FILE_JSON = "missing_file.json";
    public static final String PUBLICATION_CONTEXT_JSON = "publicationContext.json";
    
    private JsonLdContextUtil contextUtil;
    
    @BeforeEach
    public void setUp() {
        contextUtil = new JsonLdContextUtil(dtoObjectMapper);
    }
    
    @Test
    @DisplayName("reading Existing Publication File Returns Data")
    public void readingExistingPublicationFileReturnsData() {
        Optional<JsonNode> publicationContext = contextUtil
                                                    .getPublicationContext(PUBLICATION_CONTEXT_JSON);
        assertTrue(publicationContext.isPresent());
    }
    
    @Test
    @DisplayName("reading Missing File Returns Empty Data")
    public void readingMissingFileReturnsEmptyData() {
        Optional<JsonNode> publicationContext = contextUtil
                                                    .getPublicationContext(MISSING_FILE_JSON);
        assertTrue(publicationContext.isEmpty());
    }
}
