package no.unit.nva.publication.storage.model.daos;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.exceptions.InvalidIssnException;
import no.unit.nva.publication.storage.model.UserInstance;
import nva.commons.core.JsonUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static no.unit.nva.publication.storage.model.DatabaseConstants.KEY_FIELDS_DELIMITER;
import static no.unit.nva.publication.storage.model.daos.DaoUtils.sampleResourceDao;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;

public class ResourceDaoTest {
    
    public static final String SAMPLE_USER = "some@owner";
    public static final SortableIdentifier SAMPLE_IDENTIFIER = SortableIdentifier.next();
    public static final String PUBLISHER_IDENTIFIER = "publisherIdentifier";
    public static final URI SAMPLE_PUBLISHER = URI.create("https://some.example.org/" + PUBLISHER_IDENTIFIER);
    public static final UserInstance SAMPLE_USER_INSTANCE = new UserInstance(SAMPLE_USER, SAMPLE_PUBLISHER);
    
    @Test
    public void queryObjectReturnsObjectWithNonNullPrimaryPartitionKey() {
        ResourceDao queryObject = ResourceDao.queryObject(SAMPLE_USER_INSTANCE, SAMPLE_IDENTIFIER);
        assertThat(queryObject.getPrimaryKeyPartitionKey(), containsString(PUBLISHER_IDENTIFIER));
        assertThat(queryObject.getPrimaryKeyPartitionKey(), containsString(SAMPLE_USER));
    }
    
    @Test
    public void queryObjectReturnsObjectWithNonNullPrimarySortKey() {
        ResourceDao queryObject = ResourceDao.queryObject(SAMPLE_USER_INSTANCE, SAMPLE_IDENTIFIER);
        assertThat(queryObject.getPrimaryKeySortKey(), containsString(SAMPLE_IDENTIFIER.toString()));
    }
    
    @Test
    public void constructPrimaryPartitionKeyReturnsStringContainingTypePublisherAndOwner() {
        String primaryPartitionKey = ResourceDao.constructPrimaryPartitionKey(SAMPLE_PUBLISHER, SAMPLE_USER);
        String expectedKey = ResourceDao.getContainedType()
                             + KEY_FIELDS_DELIMITER
                             + PUBLISHER_IDENTIFIER
                             + KEY_FIELDS_DELIMITER
                             + SAMPLE_USER;
        assertThat(primaryPartitionKey, is(equalTo(expectedKey)));
    }

    @Test
    public void getResourceByCristinIdPartitionKeyReturnsANullValueWhenObjectHasNoCristinIdentifier()
            throws MalformedURLException, InvalidIssnException {
        ResourceDao daoWithoutCristinId = WithCristinIdentifierTest.createResourceDaoWithoutCristinIdentifier();
        assertThat(daoWithoutCristinId.getResourceByCristinIdentifierPartitionKey(),
                is(equalTo(null)));
    }

    @Test
    public void resourceDaoOnlySerializesTypeDataPKAndSKFields() throws MalformedURLException, InvalidIssnException, JsonProcessingException {
        ResourceDao dao = sampleResourceDao();
        ObjectMapper mapper = JsonUtils.objectMapper.copy().configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, true);
        String stringValue = mapper.writeValueAsString(dao);
        ObjectNode jsonNode = (ObjectNode) mapper.readTree(stringValue);
        Iterator<String> fieldNames = jsonNode.fieldNames();
        List<String> fieldNamelist = new ArrayList<>();
        fieldNames.forEachRemaining(fieldNamelist::add);
        for (String field:fieldNamelist) {
            assertThat(StringUtils.startsWithAny(field, "PK", "SK", "data", "type"), is(true));
        }
    }
}