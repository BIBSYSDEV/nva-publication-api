package no.unit.nva.publication.storage.model;

import static no.unit.nva.publication.storage.model.StorageModelConfig.dynamoDbObjectMapper;
import static no.unit.nva.testutils.RandomDataGenerator.randomElement;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import com.fasterxml.jackson.databind.JsonNode;
import no.unit.nva.identifiers.SortableIdentifier;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class PublishingRequestTest {

    public static final String TYPE_FIELD = "type";
    static SortableIdentifier randomIdentifier;
    private final PublishingRequest sampleRequest = samplePublishingRequest();

    @BeforeAll
    static void beforeAll() {
        randomIdentifier = SortableIdentifier.next();
    }

    @Test
    void publishingRequestHasTypePublishingRequest() {
        var json = dynamoDbObjectMapper.convertValue(sampleRequest, JsonNode.class);
        assertThat(json.get(TYPE_FIELD), is(not(nullValue())));
        assertThat(json.get(TYPE_FIELD).textValue(), is(equalTo(PublishingRequest.TYPE)));
    }

    @Test
    void publishingRequestHasReferenceToResource() {
        assertThat(sampleRequest.getResourceIdentifier(), is(notNullValue()));
    }

    @Test
    void shouldBeConvertableToPublicationObject() {
        var publication= sampleRequest.toPublication();
        assertThat(sampleRequest.getResourceIdentifier(),is(equalTo(publication.getIdentifier())));
        assertThat(sampleRequest.getOwner(),is(equalTo(publication.getResourceOwner().getOwner())));
        assertThat(sampleRequest.getCustomerId(),is(equalTo(publication.getPublisher().getId())));
    }

    @Test
    void shouldBeStorableInDynamoDb() {
        assertThat(sampleRequest.toDao().getData(), is(equalTo(sampleRequest)));
    }

    @Test
    void shouldBePossibleToIndexByStatus() {
        var statusString = sampleRequest.getStatusString();
        assertThat(statusString,is(equalTo(sampleRequest.getStatus().toString())));
    }

    @Test
    void shouldReturnPublishingRequestWithAdequateInfoForCreatingEntryWhenSuppliedWithUserAndPublicationInfo(){
        var userInstance = UserInstance.create(randomString(),randomUri());
        var publicationIdentifier = SortableIdentifier.next();
        var objectForCreatingNewEntry= PublishingRequest.create(userInstance,publicationIdentifier);
        assertThat(objectForCreatingNewEntry.getResourceIdentifier(),is(equalTo(publicationIdentifier)));
        assertThat(objectForCreatingNewEntry.getOwner(),is(equalTo(userInstance.getUserIdentifier())));
        assertThat(objectForCreatingNewEntry.getCustomerId(),is(equalTo(userInstance.getOrganizationUri())));
    }

    private PublishingRequest samplePublishingRequest() {
        var userInstance = UserInstance.create(randomString(),randomUri());
        return PublishingRequest.create(userInstance,
                                        SortableIdentifier.next(),
                                        SortableIdentifier.next(),
                                        randomElement(PublishingRequestStatus.values()));
    }
}
