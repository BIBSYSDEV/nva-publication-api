package no.unit.nva.publication.storage.model.daos;

import static no.unit.nva.publication.StorageModelTestUtils.randomString;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCES_TABLE_NAME;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import java.net.MalformedURLException;
import java.time.Clock;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.exceptions.InvalidIssnException;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.storage.model.DoiRequest;
import no.unit.nva.publication.storage.model.Message;
import no.unit.nva.publication.storage.model.Resource;
import no.unit.nva.publication.storage.model.UserInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class WithPrimaryKeyTest extends ResourcesLocalTest {

    private static final Clock CLOCK = Clock.systemDefaultZone();

    @BeforeEach
    public void init() {
        super.init();
    }

    @ParameterizedTest
    @MethodSource("withPrimaryKeyInstancesProvider")
    public void primaryKeyPartitionKeyConditionReturnsQueryConditionForListingAllEntriesOfSpecificTypeForUser(
        List<?> daos) {
        daos.forEach(this::insertToDb);
        WithPrimaryKey queryObject = (WithPrimaryKey) daos.get(0);

        var result = performQuery(queryObject);
        var expectedItems = daos.toArray(Object[]::new);
        assertThat(result, containsInAnyOrder(expectedItems));
    }

    private static Stream<List<?>> withPrimaryKeyInstancesProvider()
        throws InvalidIssnException, MalformedURLException {
        List<ResourceDao> resources = sampleResources();
        List<DoiRequestDao> doiRequests = sampleDoiRequests();
        List<MessageDao> messages = sampleMessages();
        return Stream.of(resources, doiRequests, messages);
    }

    private static List<MessageDao> sampleMessages() {

        List<ResourceDao> resources = sampleResources();
        return resources.stream()
                   .map(WithPrimaryKeyTest::randomMessage)
                   .map(MessageDao::new)
                   .collect(Collectors.toList());
    }

    private static Message randomMessage(ResourceDao res) {
        UserInstance sampleSender = createSampleUser(res);
        Message message = Message.supportMessage(sampleSender, res.getData().toPublication(), randomString(), CLOCK);
        message.setIdentifier(SortableIdentifier.next());
        return message;
    }

    private static UserInstance createSampleUser(ResourceDao resource) {
        return new UserInstance(resource.getOwner(), resource.getCustomerId());
    }

    private static List<ResourceDao> sampleResources() {
        return List.of(randomPublication(), randomPublication())
                   .stream()
                   .map(Resource::fromPublication)
                   .map(ResourceDao::new)
                   .collect(Collectors.toList());
    }

    private static List<DoiRequestDao> sampleDoiRequests() {
        List<ResourceDao> resources = sampleResources();
        return sampleDoiRequests(resources);
    }

    private static List<DoiRequestDao> sampleDoiRequests(List<ResourceDao> publications) {
        return publications.stream()
                   .map(ResourceDao::getData)
                   .map(DoiRequest::newDoiRequestForResource)
                   .map(DoiRequestDao::new)
                   .collect(Collectors.toList());
    }

    private static Publication randomPublication() {
        return PublicationGenerator.publicationWithIdentifier();
    }

    private List<? extends WithPrimaryKey> performQuery(WithPrimaryKey queryObject) {
        QueryRequest query = createQuery(queryObject);
        return sendQueryAndParseResponse(queryObject, query);
    }

    private List<? extends WithPrimaryKey> sendQueryAndParseResponse(WithPrimaryKey queryObject, QueryRequest query) {
        return client.query(query)
                   .getItems()
                   .stream()
                   .map(item -> DynamoEntry.parseAttributeValuesMap(item, queryObject.getClass()))
                   .collect(Collectors.toList());
    }

    private QueryRequest createQuery(WithPrimaryKey queryObject) {
        return new QueryRequest()
                   .withTableName(RESOURCES_TABLE_NAME)
                   .withKeyConditions(queryObject.primaryKeyPartitionKeyCondition());
    }

    private void insertToDb(Object dao) {
        DynamoEntry dynamoEntry = (DynamoEntry) dao;
        PutItemRequest putItemRequest = new PutItemRequest()
                                            .withTableName(RESOURCES_TABLE_NAME)
                                            .withItem(dynamoEntry.toDynamoFormat());
        client.putItem(putItemRequest);
    }
}