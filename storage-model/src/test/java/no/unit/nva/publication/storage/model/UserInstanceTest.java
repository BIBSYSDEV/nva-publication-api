package no.unit.nva.publication.storage.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.testing.PublicationGenerator;
import org.junit.jupiter.api.Test;

class UserInstanceTest {

    @Test
    void shouldReturnUserInstanceFromPublication() {
        Publication publication = PublicationGenerator.randomPublication();
        var userInstance = UserInstance.fromPublication(publication);
        assertThat(userInstance.getUserIdentifier(), is(equalTo(publication.getResourceOwner().getOwner())));
        assertThat(userInstance.getOrganizationUri(), is(equalTo(publication.getPublisher().getId())));
    }

    @Test
    void shouldReturnUserInstanceFromDoiRequest() {
        Publication publication = PublicationGenerator.randomPublication();
        var doiRequest = DoiRequest.fromPublication(publication, SortableIdentifier.next());
        var userInstance = UserInstance.fromDoiRequest(doiRequest);
        assertThat(userInstance.getUserIdentifier(), is(equalTo(publication.getResourceOwner().getOwner())));
        assertThat(userInstance.getOrganizationUri(), is(equalTo(publication.getPublisher().getId())));
    }
}