package no.unit.nva.publication.storage.model.daos;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import java.net.URI;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.storage.model.UserInstance;
import org.junit.jupiter.api.Test;

public class DoiRequestDaoTest {
    
    public static final String SOME_USER = "someuser";
    private static final String ORG_IDENTIFIER = "someOrgIdentifier";
    public static final URI SOME_ORG = URI.create("https://someorg.example.com/" + ORG_IDENTIFIER);
    private final SortableIdentifier resourceIdentifier = SortableIdentifier.next();
    private final SortableIdentifier entryIdentifier = SortableIdentifier.next();
    
    @Test
    public void queryByResourceIdReturnObjectWithPartitionKeyContainingPublisherAndResourceIdentifier() {
        UserInstance userInstance = new UserInstance(SOME_USER, SOME_ORG);
        DoiRequestDao queryObject = DoiRequestDao.queryByResourceIdentifier(userInstance, resourceIdentifier);
        assertThat(queryObject.getByResourcePartitionKey(), containsString(SOME_USER));
        assertThat(queryObject.getByResourcePartitionKey(), containsString(resourceIdentifier.toString()));
    }
    
    @Test
    public void queryByResourceIdReturnObjectWithSortKeyContainingEntryType() {
        UserInstance userInstance = new UserInstance(SOME_USER, SOME_ORG);
        DoiRequestDao queryObject = DoiRequestDao.queryByResourceIdentifier(userInstance, resourceIdentifier);
        assertThat(queryObject.getByResourceSortKey(), containsString(DoiRequestDao.getContainedType()));
    }
}