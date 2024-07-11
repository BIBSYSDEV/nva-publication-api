package no.unit.nva.api;

import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValuesIgnoringFields;
import static no.unit.nva.model.PublicationTest.ALLOWED_OPERATIONS_FIELD;
import static no.unit.nva.model.PublicationTest.BOOK_REVISION_FIELD;
import static no.unit.nva.model.PublicationTest.IMPORT_DETAILS_FIELD;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import java.util.List;
import java.util.Set;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationNote;
import no.unit.nva.model.testing.PublicationGenerator;
import org.hamcrest.core.Is;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

public class PublicationResponseElevatedUserTest {

    @Test
    void shouldReceivePublicationNotesWhenConvertingFromPublication() {
        var publication = randomPublication();
        publication.setPublicationNotes(List.of(new PublicationNote(randomString())));
        var publicationResponseForElevatedUsers = PublicationResponseElevatedUser.fromPublication(publication);
        assertThat(publicationResponseForElevatedUsers.getPublicationNotes(),
                   is(equalTo(publication.getPublicationNotes())));
    }

    @Test
    void staticConstructorShouldReturnPublicationResponseForElevatedUsersWithoutUnexpectedLossOfInformation() {
        Publication publication = randomPublication();
        assertThat(publication,
                   doesNotHaveEmptyValuesIgnoringFields(Set.of(BOOK_REVISION_FIELD, ALLOWED_OPERATIONS_FIELD,
                                                               IMPORT_DETAILS_FIELD)));
        var publicationResponse = PublicationResponseElevatedUser.fromPublication(publication);
        assertThat(publicationResponse,
                   doesNotHaveEmptyValuesIgnoringFields(Set.of(BOOK_REVISION_FIELD, ALLOWED_OPERATIONS_FIELD,
                                                               IMPORT_DETAILS_FIELD)));
    }

    @Test
    void staticConstructorWithAllowedOperationsShouldReturnPublicationResponseWithoutUnexpectedLossOfInformation() {
        Publication publication = randomPublication();
        var operation = PublicationOperation.UPDATE;
        PublicationResponseElevatedUser publicationResponse = PublicationResponseElevatedUser.fromPublicationWithAllowedOperations(
            publication,
            Set.of(operation));
        assertThat(publicationResponse.getAllowedOperations(), Is.is(IsEqual.equalTo(Set.of(operation))));
    }
}
