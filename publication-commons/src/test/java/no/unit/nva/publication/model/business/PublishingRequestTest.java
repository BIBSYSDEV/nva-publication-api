package no.unit.nva.publication.model.business;

import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.publication.model.business.StorageModelTestUtils.randomPublishingRequest;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import no.unit.nva.identifiers.SortableIdentifier;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class PublishingRequestTest {
    
    static SortableIdentifier randomIdentifier;
    private final PublishingRequestCase sampleRequest = randomPublishingRequest();
    
    @BeforeAll
    static void beforeAll() {
        randomIdentifier = SortableIdentifier.next();
    }
    
    @Test
    void shouldBePossibleToIndexByStatus() {
        var statusString = sampleRequest.getStatusString();
        assertThat(statusString, is(equalTo(sampleRequest.getStatus().toString())));
    }
    
    @Test
    void shouldReturnPublishingRequestWithAdequateInfoForCreatingEntryWhenSuppliedWithUserAndPublicationInfo() {
        var publication = randomPublication();
        var objectForCreatingNewEntry = PublishingRequestCase.createOpeningCaseObject(publication);
        var userInstance = UserInstance.fromPublication(publication);
        assertThat(objectForCreatingNewEntry.extractPublicationIdentifier(), is(equalTo(publication.getIdentifier())));
        assertThat(objectForCreatingNewEntry.getOwner(), is(equalTo(userInstance.getUser())));
        assertThat(objectForCreatingNewEntry.getCustomerId(), is(equalTo(userInstance.getOrganizationUri())));
    }
}
