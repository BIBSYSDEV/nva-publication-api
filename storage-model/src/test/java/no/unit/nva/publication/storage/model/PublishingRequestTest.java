package no.unit.nva.publication.storage.model;

import static no.unit.nva.testutils.RandomDataGenerator.randomElement;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import no.unit.nva.identifiers.SortableIdentifier;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class PublishingRequestTest {

    static SortableIdentifier randomIdentifier;
    private final PublishingRequestCase sampleRequest = samplePublishingRequest();

    @BeforeAll
    static void beforeAll() {
        randomIdentifier = SortableIdentifier.next();
    }

    @Test
    void shouldBeConvertableToPublicationObject() {
        var publication = sampleRequest.toPublication();
        assertThat(sampleRequest.getResourceIdentifier(), is(equalTo(publication.getIdentifier())));
        assertThat(sampleRequest.getOwner(), is(equalTo(publication.getResourceOwner().getOwner())));
        assertThat(sampleRequest.getCustomerId(), is(equalTo(publication.getPublisher().getId())));
    }

    @Test
    void shouldBePossibleToIndexByStatus() {
        var statusString = sampleRequest.getStatusString();
        assertThat(statusString, is(equalTo(sampleRequest.getStatus().toString())));
    }

    @Test
    void shouldReturnPublishingRequestWithAdequateInfoForCreatingEntryWhenSuppliedWithUserAndPublicationInfo() {
        var userInstance = UserInstance.create(randomString(), randomUri());
        var publicationIdentifier = SortableIdentifier.next();
        var objectForCreatingNewEntry = PublishingRequestCase.createOpeningCaseObject(userInstance,
                                                                                      publicationIdentifier);
        assertThat(objectForCreatingNewEntry.getResourceIdentifier(), is(equalTo(publicationIdentifier)));
        assertThat(objectForCreatingNewEntry.getOwner(), is(equalTo(userInstance.getUserIdentifier())));
        assertThat(objectForCreatingNewEntry.getCustomerId(), is(equalTo(userInstance.getOrganizationUri())));
    }

    private PublishingRequestCase samplePublishingRequest() {
        var userInstance = UserInstance.create(randomString(), randomUri());
        return PublishingRequestCase.createStatusUpdate(userInstance,
                                                        SortableIdentifier.next(),
                                                        SortableIdentifier.next(),
                                                        randomElement(PublishingRequestStatus.values()));
    }
}
