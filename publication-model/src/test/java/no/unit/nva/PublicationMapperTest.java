package no.unit.nva;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import no.unit.nva.api.PublicationResponse;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.model.Publication;
import no.unit.nva.model.testing.PublicationGenerator;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static no.unit.nva.DatamodelConfig.dataModelObjectMapper;
import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValuesIgnoringFields;
import static no.unit.nva.model.PublicationTest.BOOK_REVISION_FIELD;
import static no.unit.nva.model.PublicationTest.DOI_REQUEST_FIELD;
import static no.unit.nva.model.PublicationTest.IMPORT_DETAILS_FIELD;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PublicationMapperTest {

    public static final ObjectNode SOME_CONTEXT = dataModelObjectMapper.createObjectNode();

    @Test
    void canMapPublicationAndContextToPublicationResponse() throws Exception {
        var publication = PublicationGenerator.randomPublication();
        var json = JsonUtils.dtoObjectMapper.writeValueAsString(publication);
        var deserializedPublication = JsonUtils.dtoObjectMapper.readValue(json, Publication.class);

        assertThat(deserializedPublication,
                   doesNotHaveEmptyValuesIgnoringFields(Set.of(DOI_REQUEST_FIELD, BOOK_REVISION_FIELD,
                                                               IMPORT_DETAILS_FIELD)));

        var response = PublicationMapper
                           .convertValue(publication, SOME_CONTEXT, PublicationResponse.class);

        assertNotNull(response);
        assertThat(response.getId(), notNullValue());
        assertThatIdIsPresent(response);
    }

    @Test
    void canMapPublicationToPublicationResponse() {
        var publication = PublicationGenerator.randomPublication();

        var response = PublicationMapper
                           .convertValue(publication, PublicationResponse.class);

        assertNotNull(response);
        assertThat(response.getId(), notNullValue());
    }

    private void assertThatIdIsPresent(PublicationResponse response) throws JsonProcessingException {
        var jsonString = dataModelObjectMapper.writeValueAsString(response);
        var json = (ObjectNode) dataModelObjectMapper.readTree(jsonString);

        assertThat(json.has("id"), is(equalTo(true)));
    }
}

