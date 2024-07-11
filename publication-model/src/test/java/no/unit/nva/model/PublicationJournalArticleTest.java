package no.unit.nva.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.jsonldjava.utils.JsonUtils;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.model.testing.PublicationInstanceBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Stream;

import static no.unit.nva.DatamodelConfig.dataModelObjectMapper;
import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValuesIgnoringFields;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class PublicationJournalArticleTest extends PublicationTest {

    public static final String DOI_REQUEST = "doiRequest";

    /**
     * Constructor for PublicationJournalArticleTest.
     */
    public PublicationJournalArticleTest() {
        super();
    }

    public static Stream<Class<?>> journalArticleInstanceTypes() {
        return PublicationInstanceBuilder.journalArticleInstanceTypes();
    }

    @DisplayName("The serialized Publication class can be framed to match the RDF data model")
    @Test
    void objectMappingOfPublicationClassReturnsSerializedJsonWithJsonLdFrame() throws IOException {

        Publication publication = PublicationGenerator.randomPublication();
        var title = publication.getEntityDescription().getMainTitle();

        JsonNode publicationWithContext = toPublicationWithContext(publication);

        Object framedPublication = produceFramedPublication(publicationWithContext);

        Assertions.assertTrue(JsonUtils.toString(framedPublication).contains(title));
    }

    @DisplayName("Test publications can be serialized/deserialized")
    @ParameterizedTest(name = "Test Publication context Journal with Instance type {0} can be (de-)serialized")
    @MethodSource("journalArticleInstanceTypes")
    void publicationReturnsJsonWhenInputIsValid(Class<?> publicationInstanceClass) throws IOException {
        Publication publication = PublicationGenerator.randomPublication(publicationInstanceClass);
        JsonNode document = toPublicationWithContext(publication);
        String content = dataModelObjectMapper.writeValueAsString(document);
        Publication publicationFromJson = dataModelObjectMapper.readValue(content, Publication.class);
        assertThat(publicationFromJson, doesNotHaveEmptyValuesIgnoringFields(Set.of(DOI_REQUEST, BOOK_REVISION_FIELD,
                                                                                    IMPORT_DETAILS_FIELD)));
        assertThat(publication, is(equalTo(publicationFromJson)));
    }
}
