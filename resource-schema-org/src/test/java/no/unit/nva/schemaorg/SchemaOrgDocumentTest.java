package no.unit.nva.schemaorg;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.expansion.model.ExpandedResource;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;


class SchemaOrgDocumentTest {

    public static final ObjectMapper MAPPER = JsonUtils.dtoObjectMapper;

    @Test
    void shouldReturnSchemaOrgDocumentWithTypeWhenInputIsExpandedResource() {
        var expandedResource = new ExpandedResource();
        var actual = SchemaOrgDocument.fromExpandedResource(expandedResource);
        assertThat(actual.getType(), is(equalTo(SchemaOrgType.SCHOLARLY_ARTICLE)));
    }
}
