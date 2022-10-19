package no.unit.nva.publication.model.storage;

import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCES_TABLE_NAME;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.model.business.Entity;
import no.unit.nva.publication.model.business.Message;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.testing.TypeProvider;
import nva.commons.core.attempt.Try;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class WithPrimaryKeyTest extends ResourcesLocalTest {
    
    @BeforeEach
    public void init() {
        super.init();
    }
    
    @ParameterizedTest(name = "The partition key of the primary key should allow the retrieval of "
                              + "all entries of a specific type for a specific owner")
    @MethodSource("createUnpersistedEntities")
    void primaryKeyPartitionKeyConditionReturnsQueryConditionForListingAllEntriesOfSpecificTypeForASingleUser(
        List<Entity> entities) {
        var daos = entities.stream().map(Entity::toDao).collect(Collectors.toList());
        daos.forEach(this::insertToDb);
        WithPrimaryKey queryObject = daos.get(0);
        
        var result = performQuery(queryObject);
        var expectedItems = daos.toArray(Object[]::new);
        assertThat(result, containsInAnyOrder(expectedItems));
    }
    
    private static Stream<List<?>> createUnpersistedEntities() {
        List<Resource> resources = sampleResourcesOfSameOwner();
        List<TicketEntry> tickets = sampleTickets();
        List<Message> messages = sampleMessages();
        return Stream.of(resources, tickets, messages);
    }
    
    private static List<Message> sampleMessages() {
        return sampleTickets().stream().flatMap(WithPrimaryKeyTest::randomMessages).collect(Collectors.toList());
    }
    
    private static Stream<Message> randomMessages(TicketEntry ticket) {
        return Stream.of(randomMessage(ticket), randomMessage(ticket));
    }
    
    private static Message randomMessage(TicketEntry ticket) {
        return Message.create(ticket, UserInstance.fromTicket(ticket), randomString());
    }
    
    private static List<Resource> sampleResourcesOfSameOwner() {
        ResourceOwner commonOwner = new ResourceOwner(randomString(), null);
        Organization commonPublisher = new Organization.Builder().withId(randomUri()).build();
        return Stream.of(draftPublicationWithoutDoi(), draftPublicationWithoutDoi())
            
                   .map(publication -> publication.copy().withResourceOwner(commonOwner).build())
                   .map(publication -> publication.copy().withPublisher(commonPublisher).build())
                   .map(Resource::fromPublication)
                   .collect(Collectors.toList());
    }
    
    private static Publication draftPublicationWithoutDoi() {
        return randomPublication().copy().withDoi(null).withStatus(PublicationStatus.DRAFT).build();
    }
    
    private static List<TicketEntry> sampleTickets() {
        var resources = sampleResourcesOfSameOwner();
        return sampleTickets(resources);
    }
    
    private static List<TicketEntry> sampleTickets(List<Resource> resources) {
        var result = new ArrayList<TicketEntry>();
        for (var resource : resources) {
            var tickets = createUnpersistedTicketEntries(resource);
            result.addAll(tickets);
        }
        return result;
    }
    
    private static List<TicketEntry> createUnpersistedTicketEntries(Resource resource) {
        return TypeProvider.listSubTypes(TicketEntry.class)
                   .map(ticketType -> (Class<TicketEntry>) ticketType)
                   .map(attempt(ticketType -> TicketEntry.createNewTicket(resource.toPublication(), ticketType,
                       SortableIdentifier::next)))
                   .map(Try::orElseThrow)
                   .collect(Collectors.toList());
    }
    
    private static Publication randomPublication() {
        return PublicationGenerator.randomPublication();
    }
    
    private List<? extends WithPrimaryKey> performQuery(WithPrimaryKey queryObject) {
        QueryRequest query = createQuery(queryObject);
        return sendQueryAndParseResponse(query);
    }
    
    private List<? extends WithPrimaryKey> sendQueryAndParseResponse(QueryRequest query) {
        return client.query(query)
                   .getItems()
                   .stream()
                   .map(item -> DynamoEntry.parseAttributeValuesMap(item, Dao.class))
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