package no.unit.nva.publication.service.impl;

import static no.unit.nva.model.PublicationStatus.DRAFT;
import static no.unit.nva.model.PublicationStatus.PUBLISHED;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator.randomOpenFile;
import static no.unit.nva.publication.TestingUtils.randomUserInstance;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCES_TABLE_NAME;
import static no.unit.nva.publication.ticket.test.TicketTestUtils.createPersistedPublicationWithOwner;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import java.util.Map;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.Entity;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.GeneralSupportRequest;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.model.storage.Dao;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.ticket.test.TicketTestUtils;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class VersionRefreshServiceTest extends ResourcesLocalTest {

  private VersionRefreshService versionRefreshService;
  private ResourceService resourceService;
  private TicketService ticketService;
  private MessageService messageService;

  @BeforeEach
  void setUp() {
    super.init();
    resourceService = getResourceService(client);
    ticketService = getTicketService();
    messageService = getMessageService();
    versionRefreshService = new VersionRefreshService(client, RESOURCES_TABLE_NAME);
  }

  @Test
  void shouldUpdateVersionWhenRefreshingResource() {
    var resource = Resource.fromPublication(randomPublication());
    persistResource(resource);

    var versionBefore = fetchVersion(resource);
    versionRefreshService.refresh(resource);
    var versionAfter = fetchVersion(resource);

    assertNotEquals(versionBefore, versionAfter);
  }

  @Test
  void shouldPreserveResourceDataWhenRefreshingResource() throws NotFoundException {
    var resource = super.persistResource(Resource.fromPublication(randomPublication()));

    versionRefreshService.refresh(resource);

    var refreshedPublication =
        resourceService.getResourceByIdentifier(resource.getIdentifier()).toPublication();

    assertEquals(resource.toPublication(), refreshedPublication);
  }

  @Test
  void shouldUpdateVersionWhenRefreshingTicket() throws ApiGatewayException {
    var owner = randomUserInstance();
    var publication = createPersistedPublicationWithOwner(DRAFT, owner, resourceService);
    var ticket =
        TicketTestUtils.createPersistedTicket(
            publication, GeneralSupportRequest.class, ticketService);

    var versionBefore = fetchVersion(ticket);
    versionRefreshService.refresh(ticket);
    var versionAfter = fetchVersion(ticket);

    assertNotEquals(versionBefore, versionAfter);
  }

  @Test
  void shouldPreserveTicketDataWhenRefreshingTicket() throws ApiGatewayException {
    var owner = randomUserInstance();
    var publication = createPersistedPublicationWithOwner(DRAFT, owner, resourceService);
    var ticket =
        TicketTestUtils.createPersistedTicket(
            publication, GeneralSupportRequest.class, ticketService);

    versionRefreshService.refresh(ticket);

    var refreshedTicket = ticket.fetch(ticketService);

    assertEquals(ticket, refreshedTicket);
  }

  @Test
  void shouldUpdateVersionWhenRefreshingMessage() throws ApiGatewayException {
    var publication =
        createPersistedPublicationWithOwner(PUBLISHED, randomUserInstance(), resourceService);
    var ticket =
        TicketTestUtils.createPersistedTicket(publication, DoiRequest.class, ticketService);
    var message = messageService.createMessage(ticket, randomUserInstance(), randomString());

    var versionBefore = fetchVersion(message);
    versionRefreshService.refresh(message);
    var versionAfter = fetchVersion(message);

    assertNotEquals(versionBefore, versionAfter);
  }

  @Test
  void shouldPreserveMessageDataWhenRefreshingMessage() throws ApiGatewayException {
    var publication =
        createPersistedPublicationWithOwner(PUBLISHED, randomUserInstance(), resourceService);
    var ticket =
        TicketTestUtils.createPersistedTicket(publication, DoiRequest.class, ticketService);
    var message = messageService.createMessage(ticket, randomUserInstance(), randomString());

    versionRefreshService.refresh(message);

    var refreshedMessage =
        messageService.getMessageByIdentifier(message.getIdentifier()).orElseThrow();

    assertEquals(message, refreshedMessage);
  }

  @Test
  void shouldUpdateVersionWhenRefreshingFileEntry() throws ApiGatewayException {
    var publication =
        TicketTestUtils.createPersistedPublicationWithOwner(
            DRAFT, randomUserInstance(), resourceService);
    var userInstance = UserInstance.fromPublication(publication);
    var fileEntry = FileEntry.create(randomOpenFile(), publication.getIdentifier(), userInstance);
    fileEntry.persist(resourceService, userInstance);

    var versionBefore = fetchVersion(fileEntry);
    versionRefreshService.refresh(fileEntry);
    var versionAfter = fetchVersion(fileEntry);

    assertNotEquals(versionBefore, versionAfter);
  }

  @Test
  void shouldPreserveFileDataWhenRefreshingFileEntry() throws ApiGatewayException {
    var publication =
        TicketTestUtils.createPersistedPublicationWithOwner(
            DRAFT, randomUserInstance(), resourceService);
    var userInstance = UserInstance.fromPublication(publication);
    var fileEntry = FileEntry.create(randomOpenFile(), publication.getIdentifier(), userInstance);
    fileEntry.persist(resourceService, userInstance);

    versionRefreshService.refresh(fileEntry);

    var refreshedFile =
        FileEntry.queryObject(fileEntry.getIdentifier()).fetch(resourceService).orElseThrow();

    assertEquals(fileEntry, refreshedFile);
  }

  private String fetchVersion(Entity entity) {
    return fetchRawItem(entity).get(Dao.VERSION_FIELD).getS();
  }

  private Map<String, AttributeValue> fetchRawItem(Entity entity) {
    return client
        .getItem(
            new GetItemRequest()
                .withTableName(RESOURCES_TABLE_NAME)
                .withKey(entity.toDao().primaryKey()))
        .getItem();
  }
}
