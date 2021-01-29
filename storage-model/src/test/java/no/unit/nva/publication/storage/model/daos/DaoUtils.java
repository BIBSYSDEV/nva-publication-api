package no.unit.nva.publication.storage.model.daos;

import static java.util.Objects.nonNull;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCES_TABLE_NAME;
import static nva.commons.core.JsonUtils.objectMapper;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemUtils;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import java.net.MalformedURLException;
import java.time.Clock;
import java.util.Map;
import java.util.stream.Stream;
import no.unit.nva.model.exceptions.InvalidIssnException;
import no.unit.nva.publication.storage.model.DoiRequest;
import no.unit.nva.publication.storage.model.Resource;
import no.unit.nva.publication.storage.model.ResourceTest;
import no.unit.nva.publication.storage.model.RowLevelSecurity;
import no.unit.nva.publication.storage.model.WithIdentifier;
import nva.commons.core.attempt.Try;

public final class DaoUtils {

    public static final String EMPTY_VALUE_ERROR = "ValueMap was either null or empty";
    public static ResourceTest resourceGenerator = new ResourceTest();

    public static Resource sampleResource() throws InvalidIssnException, MalformedURLException {
        return Resource.fromPublication(resourceGenerator.samplePublication(
            resourceGenerator.sampleJournalArticleReference()));
    }

    public static ResourceDao sampleResourceDao() throws InvalidIssnException, MalformedURLException {
        return Try.of(sampleResource())
            .map(ResourceDao::new)
            .orElseThrow();
    }

    public static <T> T parseAttributeValuesMap(Map<String, AttributeValue> valuesMap, Class<T> dataClass) {
        if (nonNull(valuesMap) && !valuesMap.isEmpty()) {
            Item item = ItemUtils.toItem(valuesMap);
            return attempt(() -> objectMapper.readValue(item.toJSON(), dataClass)).orElseThrow();
        } else {
            throw new RuntimeException(EMPTY_VALUE_ERROR);
        }
    }

    public static <T> Map<String, AttributeValue> toDynamoFormat(T element) {
        Item item = attempt(() -> Item.fromJSON(objectMapper.writeValueAsString(element))).orElseThrow();
        return ItemUtils.toAttributeValues(item);
    }

    public static Stream<Dao<?>> instanceProvider() throws InvalidIssnException, MalformedURLException {
        ResourceDao resourceDao = sampleResourceDao();
        DoiRequestDao doiRequestDao = doiRequestDao(resourceDao.getData());
        return Stream.of(resourceDao, doiRequestDao);
    }

    public static DoiRequestDao doiRequestDao(Resource resource) {
        return attempt(() -> DoiRequest.fromResource(resource, Clock.systemDefaultZone()))
            .map(DoiRequestDao::new)
            .orElseThrow();
    }

    protected static <R extends WithIdentifier & RowLevelSecurity> PutItemRequest toPutItemRequest(Dao<R> resource) {
        return new PutItemRequest().withTableName(RESOURCES_TABLE_NAME)
            .withItem(toDynamoFormat(resource));
    }
}
