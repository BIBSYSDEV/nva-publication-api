package no.unit.nva.publication.fetch;

import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValuesIgnoringFields;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Set;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.model.Publication;
import org.junit.jupiter.api.Test;

public class DeletedPublicationResponseTest {

    @Test
    void deletePublicationResponseShouldPreserveAllFieldsExceptAssociatedArtifacts() throws JsonProcessingException {
        var publication = randomPublication();
        assertThat(publication, doesNotHaveEmptyValuesIgnoringFields(Set.of("entityDescription.reference")));
        var deletedPublicationResponseJson = DeletedPublicationResponse.craftDeletedPublicationResponse(publication);
        var deletedPublicationResponse = JsonUtils.dtoObjectMapper.convertValue(deletedPublicationResponseJson,
                                                                                Publication.class);

        var expectedPublication = publication.copy().withAssociatedArtifacts(null).build();
        assertThat(deletedPublicationResponse, is(equalTo(expectedPublication)));
    }

}
