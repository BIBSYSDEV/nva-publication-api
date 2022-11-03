package no.unit.nva.schemaorg;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.model.Publication;
import no.unit.nva.model.instancetypes.book.BookMonograph;
import no.unit.nva.model.instancetypes.journal.JournalArticle;
import org.junit.jupiter.api.Test;

import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
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
        var publication = randomPublication(JournalArticle.class);
        var actual = SchemaOrgDocument.fromPublication(publication);
        var jsonNode = attempt(() -> MAPPER.readTree(actual)).orElseThrow();
        assertThatBasicDataIsInPlace(publication, jsonNode);
    }


    @Test
    void shouldReturnEmptyJsonObjectWhenTypeIsNotJournalArticle() {
        var publication = randomPublication(BookMonograph.class);
        var actual = SchemaOrgDocument.fromPublication(publication);
        var jsonNode = attempt(() -> MAPPER.readTree(actual)).orElseThrow();
        assertThat(jsonNode.isObject(), is(true));
        assertThat(jsonNode.isEmpty(), is(true));
    }

    private static void assertThatBasicDataIsInPlace(Publication publication, JsonNode jsonNode) {
        var id = System.getenv("ID_NAMESPACE") + "/" + publication.getIdentifier().toString();
        assertThat(jsonNode.get("@id").textValue(), is(equalTo(id)));
        assertThat(jsonNode.get("@type").textValue(), is(equalTo("ScholarlyArticle")));
        assertThat(jsonNode.get("name").textValue(), is(not(nullValue())));
        assertThat(jsonNode.get("provider"), is(not(nullValue())));
    }
}
