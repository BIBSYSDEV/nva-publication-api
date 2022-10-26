package no.unit.nva.schemaorg;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.unit.nva.commons.json.JsonUtils;
import org.junit.jupiter.api.Test;

import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;


class SchemaOrgDocumentTest {

    public static final ObjectMapper MAPPER = JsonUtils.dtoObjectMapper;

    @Test
    void shouldReturnSchemaOrgDocumentWithTypeWhenInputIsExpandedJournalArticleResource() {
        var expandedResource = ExpandedResourceGenerator.generateJournalArticle();
        var actual = SchemaOrgDocument.fromExpandedResource(expandedResource);
        var jsonNode = attempt(() -> MAPPER.readTree(actual)).orElseThrow();
        assertThat(jsonNode.get("@id").textValue(), is(equalTo(expandedResource.getAllFields().get("id"))));
        assertThat(jsonNode.get("@type").textValue(), is(equalTo("ScholarlyArticle")));
        assertThat(jsonNode.get("name").textValue(), is(not(nullValue())));
    }

    @Test
    void shouldReturnEmptyJsonObjectWhenTypeIsNotJournalArticle() {
        var expandedResource = ExpandedResourceGenerator.generateAnyNonJournalArticleType();
        var actual = SchemaOrgDocument.fromExpandedResource(expandedResource);
        var jsonNode = attempt(() -> MAPPER.readTree(actual)).orElseThrow();
        assertThat(jsonNode.isObject(), is(true));
        assertThat(jsonNode.isEmpty(), is(true));
    }
}
