package no.unit.nva.publication.model.storage;

import static no.unit.nva.publication.model.storage.DynamoEntry.parseAttributeValuesMap;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCES_TABLE_NAME;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import java.net.URI;
import java.time.Clock;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Organization.Builder;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.publication.model.business.Message;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.MessageService;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import publication.test.TicketTestUtils;

class MessageDaoTest extends ResourcesLocalTest {
    
    public static final URI SAMPLE_ORG = URI.create("https://example.org/123");
    public static final String SAMPLE_OWNER_USERNAME = "some@owner";
    public static final UserInstance SAMPLE_OWNER = UserInstance.create(SAMPLE_OWNER_USERNAME, SAMPLE_ORG);
    public static final ResourceOwner RANDOM_RESOURCE_OWNER = new ResourceOwner(SAMPLE_OWNER.getUsername(),
        SAMPLE_OWNER.getOrganizationUri());
    private MessageService messageService;
    private TicketService ticketService;
    private ResourceService resourceService;
    
    @BeforeEach
    public void initialize() {
        super.init();
        this.resourceService = new ResourceService(client, Clock.systemDefaultZone());
        this.messageService = new MessageService(client);
        this.ticketService = new TicketService(client);
    }

    @ParameterizedTest
    @MethodSource("publication.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldBeRetrievableByIdentifier(Class<? extends TicketEntry> ticketType, PublicationStatus status) throws ApiGatewayException {
        var message = insertSampleMessageInDatabase(ticketType, status);
        var retrievedMessage = messageService.getMessageByIdentifier(message.getIdentifier()).orElseThrow();
        assertThat(retrievedMessage, is(equalTo(message)));
    }

    @ParameterizedTest
    @MethodSource("publication.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void queryObjectCreatesObjectForRetrievingMessageByPrimaryKey(Class<? extends TicketEntry> ticketType, PublicationStatus status) throws ApiGatewayException {
        var message = insertSampleMessageInDatabase(ticketType, status);
        var queryObject = MessageDao.queryObject(SAMPLE_OWNER, message.getIdentifier());
        var retrievedMessage = fetchMessageFromDatabase(queryObject);
        
        assertThat(retrievedMessage, is(equalTo(message)));
    }
    
    @Test
    void listMessagesAndResourcesForUserReturnsDaoWithOwnerAndPublisher() {
        var actualMessage = MessageDao.listMessagesAndResourcesForUser(SAMPLE_OWNER);
        assertThat(actualMessage.getOwner(), is(equalTo(SAMPLE_OWNER.getUser())));
        assertThat(actualMessage.getCustomerId(), is(equalTo(SAMPLE_OWNER.getOrganizationUri())));
    }
    
    private Message fetchMessageFromDatabase(MessageDao queryObject) {
        return attempt(() -> client.getItem(RESOURCES_TABLE_NAME, queryObject.primaryKey()))
                   .map(GetItemResult::getItem)
                   .map(item -> parseAttributeValuesMap(item, MessageDao.class))
                   .map(MessageDao::getMessage)
                   .orElseThrow();
    }
    
    private Message insertSampleMessageInDatabase(Class<? extends TicketEntry> ticketType, PublicationStatus status)
        throws ApiGatewayException {
        Organization publisher = new Builder().withId(SAMPLE_OWNER.getOrganizationUri()).build();
        var publication = TicketTestUtils.createPersistedPublicationWithOwner(status, UserInstance.create(RANDOM_RESOURCE_OWNER,
                                                                                                          publisher.getId()), resourceService);
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        return messageService.createMessage(ticket, UserInstance.fromTicket(ticket), randomString());
    }
}