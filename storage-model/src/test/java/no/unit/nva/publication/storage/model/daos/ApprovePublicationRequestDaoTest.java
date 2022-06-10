package no.unit.nva.publication.storage.model.daos;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.storage.model.ApprovePublicationRequest;
import no.unit.nva.publication.storage.model.Resource;
import no.unit.nva.publication.storage.model.UserInstance;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Map;

import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static no.unit.nva.publication.storage.model.DatabaseConstants.CUSTOMER_INDEX_FIELD_PREFIX;
import static no.unit.nva.publication.storage.model.DatabaseConstants.KEY_FIELDS_DELIMITER;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCE_INDEX_FIELD_PREFIX;
import static no.unit.nva.publication.storage.model.daos.DynamoEntry.parseAttributeValuesMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

class ApprovePublicationRequestDaoTest {



    private static final String SAMPLE_USER = "some@onwer";
    private static final String SAMPLE_CUSTOMER_IDENTIFIER = "somePublsherId";
    private static final URI SAMPLE_CUSTOMER = URI.create("https://some.example.org/" + SAMPLE_CUSTOMER_IDENTIFIER);
    private static final UserInstance SAMPLE_USER_INSTANCE = UserInstance.create(SAMPLE_USER, SAMPLE_CUSTOMER);
    private static final SortableIdentifier SAMPLE_RESOURCE_IDENTIFIER = SortableIdentifier.next();

    @Test
    public void queryByCustomerAndResourceIdentifierReturnsObjectWithPartitionKeyContainingPublisherAndResourceId() {

        ApprovePublicationRequestDao queryObject =
                ApprovePublicationRequestDao.queryByCustomerAndResourceIdentifier(SAMPLE_USER_INSTANCE, SAMPLE_RESOURCE_IDENTIFIER);
        String expectedPartitionKey = CUSTOMER_INDEX_FIELD_PREFIX
                + KEY_FIELDS_DELIMITER
                + SAMPLE_CUSTOMER_IDENTIFIER
                + KEY_FIELDS_DELIMITER
                + RESOURCE_INDEX_FIELD_PREFIX
                + KEY_FIELDS_DELIMITER
                + SAMPLE_RESOURCE_IDENTIFIER;

        assertThat(queryObject.getByCustomerAndResourcePartitionKey(), is(equalTo(expectedPartitionKey)));
    }



    @Test
    public void queryObjectWithOwnerAndResourceReturnsQueryObjectEnablingRetrievalOfAllDoiRequestsOfUser() {
        ApprovePublicationRequestDao queryObject = ApprovePublicationRequestDao.queryObject(SAMPLE_CUSTOMER, SAMPLE_USER);
        String expectedPrimaryPartitionKey = expectedPublicationRequestPrimaryPartitionKey();
        assertThat(queryObject.getPrimaryKeyPartitionKey(), is(equalTo(expectedPrimaryPartitionKey)));
    }

    @Test
    public void queryObjectWithOwnerResourceAndEntryIdentifierReturnsQueryObjectWithCompletePrimaryKey() {
        SortableIdentifier sampleEntryIdentifier = SortableIdentifier.next();
        ApprovePublicationRequestDao queryObject = ApprovePublicationRequestDao.queryObject(SAMPLE_CUSTOMER, SAMPLE_USER, sampleEntryIdentifier);

        assertThat(queryObject.getPrimaryKeyPartitionKey(), is(equalTo(expectedPublicationRequestPrimaryPartitionKey())));
        assertThat(queryObject.getPrimaryKeySortKey(),
                is(equalTo(expectedPublicationRequestPrimarySortKey(sampleEntryIdentifier))));
    }

    @Test
    public void parseAttributeValuesMapCreatesDaoWithoutLossOfInformation() {
        ApprovePublicationRequestDao aprDao = sampleApprovePublicationRequestDao();
        assertThat(aprDao, doesNotHaveEmptyValues());
        Map<String, AttributeValue> dynamoMap = aprDao.toDynamoFormat();
        Dao<?> parsedDao = parseAttributeValuesMap(dynamoMap, aprDao.getClass());
        assertThat(parsedDao, is(equalTo(aprDao)));
    }




    private static ApprovePublicationRequestDao sampleApprovePublicationRequestDao() {
        ApprovePublicationRequest approvePublicationRequest = ApprovePublicationRequest.newApprovePublicationRequestResource(Resource.fromPublication(PublicationGenerator.randomPublication()));
        ApprovePublicationRequestDao approvePublicationRequestDao =  new ApprovePublicationRequestDao(approvePublicationRequest);
        return approvePublicationRequestDao;
    }





    private String expectedPublicationRequestPrimarySortKey(SortableIdentifier entryIdentifier) {
        return ApprovePublicationRequestDao.getContainedType()
                + KEY_FIELDS_DELIMITER
                + entryIdentifier.toString();
    }

    private String expectedPublicationRequestPrimaryPartitionKey() {
        return ApprovePublicationRequestDao.getContainedType()
                + KEY_FIELDS_DELIMITER
                + SAMPLE_CUSTOMER_IDENTIFIER
                + KEY_FIELDS_DELIMITER
                + SAMPLE_USER;
    }



}
