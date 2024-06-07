package no.unit.nva.publication.service.impl;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.apigateway.AccessRight.MANAGE_DOI;
import static nva.commons.apigateway.AccessRight.MANAGE_PUBLISHING_REQUESTS;
import static nva.commons.apigateway.AccessRight.MANAGE_RESOURCES_STANDARD;
import static nva.commons.apigateway.AccessRight.SUPPORT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.Period;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.TestingUtils;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.GeneralSupportRequest;
import no.unit.nva.publication.model.business.Message;
import no.unit.nva.publication.model.business.MessageStatus;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UnpublishRequest;
import no.unit.nva.publication.model.business.User;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.ticket.test.TicketTestUtils;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class MessageServiceTest extends ResourcesLocalTest {
    
    public static final Instant PUBLICATION_CREATION_TIME = Instant.parse("2007-12-03T10:15:30.00Z");
    public static final Instant MESSAGE_CREATION_TIME = PUBLICATION_CREATION_TIME.plus(Period.ofDays(2));
    public static final Instant SECOND_MESSAGE_CREATION_TIME = MESSAGE_CREATION_TIME.plus(Period.ofDays(2));
    public static final Instant THIRD_MESSAGE_CREATION_TIME = SECOND_MESSAGE_CREATION_TIME.plus(Period.ofDays(2));
    private MessageService messageService;
    private ResourceService resourceService;
    private UserInstance owner;
    private TicketService ticketService;

    @BeforeEach
    public void initialize() {
        super.init();
        var clock = mockClock();
        messageService = getMessageService();
        resourceService = getResourceServiceBuilder().withClock(clock).build();
        ticketService = getTicketService();
        owner = TestingUtils.randomUserInstance();
    }


    @DisplayName("should persist message with reference to a ticket")
    @ParameterizedTest
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldPersistMessageWithReferenceToATicket(Class<? extends TicketEntry> ticketType, PublicationStatus status)
        throws ApiGatewayException {
        var publication = TicketTestUtils.createPersistedPublicationWithOwner(status, owner, resourceService);
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        var message = publicationOwnerSendsMessage(ticket, randomString());
        var persistedMessage = messageService.getMessageByIdentifier(message.getIdentifier()).orElseThrow();
        assertThat(persistedMessage.getTicketIdentifier(), is(equalTo(ticket.getIdentifier())));
        assertThat(persistedMessage.getText(), is(equalTo(message.getText())));
    }

    @ParameterizedTest
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldMarkTicketAsUnreadForEveryoneExceptSenderWhenMessageIsCreated(Class<? extends TicketEntry> ticketType, PublicationStatus status) throws ApiGatewayException {
        var publication = TicketTestUtils.createPersistedPublicationWithOwner(status, owner, resourceService);
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        var persistedMessage = messageService.createMessage(ticket, owner, randomString());
        var retrievedMessage = messageService.getMessage(owner, persistedMessage.getIdentifier());
        assertThat(retrievedMessage.getSender(), is(equalTo(owner.getUser())));
    }

    @Test
    void shouldNotDeleteMessageForMessageOwnerWhenMessageOwnerIsNotMessageSender() throws ApiGatewayException {
        var publication = TicketTestUtils.createPersistedPublicationWithOwner(
            PublicationStatus.PUBLISHED, owner, resourceService);
        var ticket = TicketTestUtils.createPersistedTicket(publication, DoiRequest.class, ticketService);

        var sender = UserInstance.create(randomString(), owner.getCustomerId());
        var persistedMessage = messageService.createMessage(ticket, sender, randomString());

        assertThrows(UnauthorizedException.class, () -> messageService.deleteMessage(owner, persistedMessage));
    }

    @Test
    void shouldDeleteMessageForMessageSender() throws ApiGatewayException {
        var publication = TicketTestUtils.createPersistedPublicationWithOwner(
            PublicationStatus.PUBLISHED, owner, resourceService);
        var ticket = TicketTestUtils.createPersistedTicket(publication, DoiRequest.class, ticketService);

        var sender = UserInstance.create(randomString(), owner.getCustomerId());
        var persistedMessage = messageService.createMessage(ticket, sender, randomString());
        messageService.deleteMessage(sender, persistedMessage);
        var deletedMessage = messageService.getMessage(owner, persistedMessage.getIdentifier());

        assertThat(deletedMessage.getStatus(), is(equalTo(MessageStatus.DELETED)));
    }

    @Test
    void shouldRefreshMessageByUpdatingVersion()
        throws ApiGatewayException {
        var publication = TicketTestUtils.createPersistedPublicationWithOwner(
            PublicationStatus.PUBLISHED, owner, resourceService);
        var ticket = TicketTestUtils.createPersistedTicket(publication, DoiRequest.class, ticketService);
        var persistedMessage = messageService.createMessage(ticket, owner, randomString());
        var version = persistedMessage.toDao().getVersion();
        messageService.refresh(persistedMessage.getIdentifier());

        var updatedMessage = messageService.getMessageByIdentifier(persistedMessage.getIdentifier()).orElseThrow();
        var updatedVersion = updatedMessage.toDao().getVersion();

        assertThat(persistedMessage, is(equalTo(updatedMessage)));
        assertThat(updatedVersion, is(not(equalTo(version))));
    }

    @Test
    void shouldThrowUnauthorizedWhenAttemptingToDeleteMessageUserDoesNotOwn()
        throws ApiGatewayException {
        var publication = TicketTestUtils.createPersistedPublicationWithOwner(
            PublicationStatus.PUBLISHED, owner, resourceService);
        var ticket = TicketTestUtils.createPersistedTicket(publication, DoiRequest.class, ticketService);
        var persistedMessage = messageService.createMessage(ticket, owner, randomString());

        assertThrows(UnauthorizedException.class,
                     () -> messageService.deleteMessage(randomUserInstance(), persistedMessage));
    }

    @ParameterizedTest
    @MethodSource("ticketTypeAccessRightsProvider")
    void shouldAllowCuratorToDeleteMessageWhenTicketHasCorrectType(Class<? extends TicketEntry> ticketType, AccessRight accessRight) throws ApiGatewayException {
        var publication = TicketTestUtils.createPersistedPublicationWithOwner(
            PublicationStatus.PUBLISHED, owner, resourceService);
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        var persistedMessage = messageService.createMessage(ticket, owner, randomString());
        var curator = randomUserInstance(accessRight, owner.getCustomerId());

        assertDoesNotThrow(() -> messageService.deleteMessage(curator, persistedMessage));
    }

    @ParameterizedTest
    @MethodSource("ticketTypeAccessRightsProvider")
    void shouldNotAllowWrongTypeOfCuratorToDeleteMessage(Class<? extends TicketEntry> ticketType, AccessRight accessRight) throws ApiGatewayException {
        var publication = TicketTestUtils.createPersistedPublicationWithOwner(
            PublicationStatus.PUBLISHED, owner, resourceService);
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        var persistedMessage = messageService.createMessage(ticket, owner, randomString());

        var curatorAccessRights = Stream.of(MANAGE_DOI, MANAGE_PUBLISHING_REQUESTS, SUPPORT)
                                      .filter(ar -> !ar.equals(accessRight))
                                      .toList();

        for (AccessRight ar : curatorAccessRights) {
            var curator = randomUserInstance(ar, owner.getCustomerId());
            assertThrows(UnauthorizedException.class, () -> messageService.deleteMessage(curator, persistedMessage));
        }
    }

    @ParameterizedTest
    @MethodSource("ticketTypeAccessRightsProvider")
    void shouldNotAllowCuratorFromAnotherInstitutionToDeleteMessage(Class<? extends TicketEntry> ticketType, AccessRight accessRight) throws ApiGatewayException {
        var publication = TicketTestUtils.createPersistedPublicationWithOwner(
            PublicationStatus.PUBLISHED, owner, resourceService);
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        var persistedMessage = messageService.createMessage(ticket, owner, randomString());
        var curatorFromRandomInstitution = randomUserInstance(accessRight, randomUri());

        assertThrows(UnauthorizedException.class, () -> messageService.deleteMessage(curatorFromRandomInstitution, persistedMessage));
    }

    private UserInstance randomUserInstance() {
        return UserInstance.create(new User(randomString()), randomUri());
    }

    private UserInstance randomUserInstance(AccessRight accessRight) {
        return randomUserInstance(accessRight, randomUri());
    }

    private UserInstance randomUserInstance(AccessRight accessRight, URI customerId) {
        return UserInstance.create(randomString(), customerId, randomUri(), List.of(accessRight), randomUri());
    }

    private Message publicationOwnerSendsMessage(TicketEntry ticket, String messageText) {
        var userInfo = UserInstance.fromTicket(ticket);
        return messageService.createMessage(ticket, userInfo, messageText);
    }
    
    private Clock mockClock() {
        var clock = mock(Clock.class);
        when(clock.instant())
            .thenReturn(PUBLICATION_CREATION_TIME)
            .thenReturn(MESSAGE_CREATION_TIME)
            .thenReturn(SECOND_MESSAGE_CREATION_TIME)
            .thenReturn(THIRD_MESSAGE_CREATION_TIME);
        return clock;
    }

    public static Stream<Arguments> ticketTypeAccessRightsProvider() {
        return Stream.of(Arguments.of(DoiRequest.class, MANAGE_DOI),
                         Arguments.of(PublishingRequestCase.class, MANAGE_PUBLISHING_REQUESTS),
                         Arguments.of(UnpublishRequest.class, MANAGE_RESOURCES_STANDARD),
                         Arguments.of(GeneralSupportRequest.class, SUPPORT));
    }
}