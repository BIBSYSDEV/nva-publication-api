package no.unit.nva.publication.storage.model.daos;

import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.storage.model.UserInstance;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static no.unit.nva.publication.storage.model.DatabaseConstants.KEY_FIELDS_DELIMITER;
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
