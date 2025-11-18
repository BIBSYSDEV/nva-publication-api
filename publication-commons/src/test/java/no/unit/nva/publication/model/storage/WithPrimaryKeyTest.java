package no.unit.nva.publication.model.storage;

import static no.unit.nva.model.PublicationStatus.PUBLISHED;
import static no.unit.nva.model.testing.PublicationGenerator.randomDegreePublication;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
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
import java.util.stream.Stream;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.Username;
import no.unit.nva.publication.model.business.Entity;
import no.unit.nva.publication.model.business.Message;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.testing.TypeProvider;
import nva.commons.core.attempt.Try;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class WithPrimaryKeyTest extends ResourcesLocalTest {

    @Override
    @BeforeEach
    public void init() {
        super.init();
    }
    
    @ParameterizedTest(name = "The partition key of the primary key should allow the retrieval of "
                              + "all entries of a specific type for a specific owner")
    @MethodSource("createUnpersistedEntities")
    void primaryKeyPartitionKeyConditionReturnsQueryConditionForListingAllEntriesOfSpecificTypeForASingleUser(
        List<Entity> entities) {
        var daos = entities.stream().map(Entity::toDao).toList();
        daos.forEach(this::insertToDb);
        WithPrimaryKey queryObject = daos.getFirst();
        
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
        return sampleTickets().stream().flatMap(WithPrimaryKeyTest::randomMessages).toList();
    }
    
    private static Stream<Message> randomMessages(TicketEntry ticket) {
        return Stream.of(randomMessage(ticket), randomMessage(ticket));
    }
    
    private static Message randomMessage(TicketEntry ticket) {
        return Message.create(ticket, UserInstance.fromTicket(ticket), randomString());
    }
    
    private static List<Resource> sampleResourcesOfSameOwner() {
        ResourceOwner commonOwner = new ResourceOwner(randomUsername(), null);
        Organization commonPublisher = new Organization.Builder().withId(randomUri()).build();
        return Stream.of(publishedPublicationWithoutDoi(), publishedPublicationWithoutDoi())
                   .map(Publication::copy)
                   .map(publication -> publication.withResourceOwner(commonOwner))
                   .map(publication -> publication.withPublisher(commonPublisher))
                   .map(Publication.Builder::build)
                   .map(Resource::fromPublication)
                   .toList();
    }

    private static Username randomUsername() {
        return new Username(randomString());
    }

    private static Publication publishedPublicationWithoutDoi() {
        return randomDegreePublication().copy().withDoi(null).withStatus(PUBLISHED).build();
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
                   .map(Named::getPayload)
                   .map(ticketType -> (Class<TicketEntry>) ticketType)
                   .map(attempt(ticketType -> TicketEntry.createNewTicket(resource.toPublication(), ticketType,
                       SortableIdentifier::next)))
                   .map(Try::orElseThrow)
                   .map(ticketEntry -> ticketEntry.withOwner(UserInstance.fromPublication(resource.toPublication()).getUsername()))
                   .toList();
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
                   .toList();
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