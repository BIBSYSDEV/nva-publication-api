package no.unit.nva.publication.storage.model.daos;

import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static no.unit.nva.publication.storage.model.DatabaseConstants.CUSTOMER_INDEX_FIELD_PREFIX;
import static no.unit.nva.publication.storage.model.DatabaseConstants.KEY_FIELDS_DELIMITER;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCE_INDEX_FIELD_PREFIX;
import static no.unit.nva.publication.storage.model.daos.DynamoEntry.parseAttributeValuesMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import java.net.URI;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.storage.model.PublishingRequest;
import no.unit.nva.publication.storage.model.Resource;
import no.unit.nva.publication.storage.model.UserInstance;
import org.junit.jupiter.api.Test;

class PublishingRequestDaoTest {

    private static final String SAMPLE_USER = "some@onwer";
    private static final String SAMPLE_CUSTOMER_IDENTIFIER = "somePublsherId";
    private static final URI SAMPLE_CUSTOMER = URI.create("https://some.example.org/" + SAMPLE_CUSTOMER_IDENTIFIER);
    private static final UserInstance SAMPLE_USER_INSTANCE = UserInstance.create(SAMPLE_USER, SAMPLE_CUSTOMER);
    private static final SortableIdentifier SAMPLE_RESOURCE_IDENTIFIER = SortableIdentifier.next();

    @Test
    public void shouldReturnObjectWithPartitionKeyContainingPublisherAndResourceId() {

        var queryObject =
            PublishingRequestDao.queryByCustomerAndResourceIdentifier(SAMPLE_USER_INSTANCE, SAMPLE_RESOURCE_IDENTIFIER);
        var expectedPartitionKey = CUSTOMER_INDEX_FIELD_PREFIX
                                   + KEY_FIELDS_DELIMITER
                                   + SAMPLE_CUSTOMER_IDENTIFIER
                                   + KEY_FIELDS_DELIMITER
                                   + RESOURCE_INDEX_FIELD_PREFIX
                                   + KEY_FIELDS_DELIMITER
                                   + SAMPLE_RESOURCE_IDENTIFIER;

        assertThat(queryObject.getByCustomerAndResourcePartitionKey(), is(equalTo(expectedPartitionKey)));
    }

    @Test
    public void shouldReturnQueryObjectWithCompletePrimaryKey() {
        var sampleEntryIdentifier = SortableIdentifier.next();
        var queryObject = PublishingRequest.createQuery(UserInstance.create(SAMPLE_USER, SAMPLE_CUSTOMER), null,
                                                        sampleEntryIdentifier);
        var queryDao = PublishingRequestDao.queryObject(queryObject);

        assertThat(queryDao.getPrimaryKeyPartitionKey(), is(equalTo(expectedPublicationRequestPrimaryPartitionKey())));
        assertThat(queryDao.getPrimaryKeySortKey(),
                   is(equalTo(expectedPublicationRequestPrimarySortKey(sampleEntryIdentifier))));
    }

    @Test
    public void shouldCreateDaoWithoutLossOfInformation() {
        var aprDao = sampleApprovePublicationRequestDao();
        assertThat(aprDao, doesNotHaveEmptyValues());
        var dynamoMap = aprDao.toDynamoFormat();
        var parsedDao = parseAttributeValuesMap(dynamoMap, aprDao.getClass());
        assertThat(parsedDao, is(equalTo(aprDao)));
    }

    private static PublishingRequestDao sampleApprovePublicationRequestDao() {
        var publishingRequest = PublishingRequest.newPublishingRequestResource(
            Resource.fromPublication(PublicationGenerator.randomPublication()));
        return new PublishingRequestDao(publishingRequest);
    }

    private String expectedPublicationRequestPrimarySortKey(SortableIdentifier entryIdentifier) {
        return PublishingRequestDao.getContainedType()
               + KEY_FIELDS_DELIMITER
               + entryIdentifier.toString();
    }

    private String expectedPublicationRequestPrimaryPartitionKey() {
        return PublishingRequestDao.getContainedType()
               + KEY_FIELDS_DELIMITER
               + SAMPLE_CUSTOMER_IDENTIFIER
               + KEY_FIELDS_DELIMITER
               + SAMPLE_USER;
    }
}
