package no.unit.nva.publication.messages.delete;

import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.publication.RequestUtil.PUBLICATION_IDENTIFIER;
import static no.unit.nva.publication.RequestUtil.TICKET_IDENTIFIER;
import static no.unit.nva.publication.messages.MessageApiConfig.MESSAGE_IDENTIFIER_PATH_PARAMETER;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.GeneralSupportRequest;
import no.unit.nva.publication.model.business.Message;
import no.unit.nva.publication.model.business.MessageStatus;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.TicketStatus;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.MessageService;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.zalando.problem.Problem;

class DeleteMessageHandlerTest extends ResourcesLocalTest {

    private static final FakeContext CONTEXT = new FakeContext();
    private DeleteMessageHandler handler;
    private ByteArrayOutputStream output;
    private ResourceService resourceService;
    private TicketService ticketService;
    private MessageService messageService;

    @BeforeEach
    public void setup() {
        super.init();
        this.resourceService = getResourceServiceBuilder().build();
        this.ticketService = getTicketService();
        this.messageService = getMessageService();
        this.handler = new DeleteMessageHandler(messageService, ticketService, resourceService, new Environment());
        this.output = new ByteArrayOutputStream();
    }

    @Test
    void shouldReturnNotFoundWhenMessageToDeleteDoesNotExist() throws IOException {
        handler.handleRequest(deleteNonExistentMessageRequest(), output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Problem.class);

        assertThat(response.getStatusCode(), is(equalTo(HTTP_NOT_FOUND)));
    }

    @ParameterizedTest
    @ValueSource(classes = {DoiRequest.class, PublishingRequestCase.class, GeneralSupportRequest.class})
    void shouldReturnSuccessWhenTicketIsSuccessfullyDeleted(Class<? extends TicketEntry> ticketType)
        throws ApiGatewayException, IOException {
        var publication = randomPersistedPublication();
        var ticket = createTicket(publication, ticketType);
        var message = messageService.createMessage(ticket, UserInstance.fromTicket(ticket), randomString());

        handler.handleRequest(deleteMessageRequest(publication, ticket, message), output, CONTEXT);

        var deletedMessage = messageService.getMessage(UserInstance.fromMessage(message), message.getIdentifier());

        assertThat(deletedMessage.getStatus(), is(equalTo(MessageStatus.DELETED)));
        assertThat(GatewayResponse.fromOutputStream(output, Void.class).getStatusCode(), is(equalTo(HTTP_OK)));
    }

    @ParameterizedTest
    @ValueSource(classes = {
        DoiRequest.class,
        PublishingRequestCase.class,
        GeneralSupportRequest.class})
    void shouldReturnUnauthorizedWhenUserIsAttemptingToDeleteTicketUserDoesNotOwn(
        Class<? extends TicketEntry> ticketType)
        throws ApiGatewayException, IOException {
        var publication = randomPersistedPublication();
        var ticket = createTicket(publication, ticketType);
        var message = messageService.createMessage(ticket, UserInstance.fromTicket(ticket), randomString());

        handler.handleRequest(deleteForeignMessageRequest(publication, ticket, message), output, CONTEXT);

        assertThat(GatewayResponse.fromOutputStream(output, Void.class).getStatusCode(),
                   is(equalTo(HTTP_UNAUTHORIZED)));
    }

    @Test
    void shouldCompleteGeneralSupportRequestWhenAllMessagesAreDeleted()
        throws ApiGatewayException, IOException {
        var publication = randomPersistedPublication();
        var ticket = createTicket(publication, GeneralSupportRequest.class);
        var message = messageService.createMessage(ticket, UserInstance.fromTicket(ticket), randomString());

        handler.handleRequest(deleteMessageRequest(publication, ticket, message), output, CONTEXT);

        var completedTicket = ticket.fetch(ticketService);

        assertThat(completedTicket.getStatus(), is(equalTo(TicketStatus.COMPLETED)));
        assertThat(GatewayResponse.fromOutputStream(output, Void.class).getStatusCode(), is(equalTo(HTTP_OK)));
    }

    private Publication randomPersistedPublication() throws BadRequestException {
        var publication = randomPublication();
        return Resource.fromPublication(publication)
                              .persistNew(resourceService, UserInstance.fromPublication(publication));
    }

    private TicketEntry createTicket(Publication publication, Class<? extends TicketEntry> ticketType) throws ApiGatewayException {
        return TicketEntry.createNewTicket(publication, ticketType, SortableIdentifier::next)
                   .withOwner(randomString())
                   .persistNewTicket(ticketService);
    }

    private TicketEntry persistRandomTicket(Class<? extends TicketEntry> ticketType) throws ApiGatewayException {
        var randomPublication = randomPublication();
        var publication = Resource.fromPublication(randomPublication)
                              .persistNew(resourceService, UserInstance.fromPublication(randomPublication));
        return TicketEntry.createNewTicket(publication, ticketType, SortableIdentifier::next)
                   .withOwner(randomString())
                   .persistNewTicket(ticketService);
    }

    private InputStream deleteMessageRequest(Publication publication, TicketEntry ticketEntry, Message message)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper)
                   .withPathParameters(pathParams(publication.getIdentifier(), ticketEntry.getIdentifier(),
                                                  message.getIdentifier()))
                   .withUserName(message.getOwner().toString())
                   .withCurrentCustomer(message.getCustomerId())
                   .build();
    }

    private InputStream deleteForeignMessageRequest(Publication publication, TicketEntry ticketEntry, Message message)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper)
                   .withPathParameters(pathParams(publication.getIdentifier(), ticketEntry.getIdentifier(),
                                                  message.getIdentifier()))
                   .withUserName(randomString())
                   .withCurrentCustomer(randomUri())
                   .build();
    }

    private InputStream deleteNonExistentMessageRequest()
        throws JsonProcessingException {
        return new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper)
                   .withPathParameters(pathParams(SortableIdentifier.next(), SortableIdentifier.next(),
                                                  SortableIdentifier.next()))
                   .withUserName(randomString())
                   .withCurrentCustomer(randomUri())
                   .build();
    }

    private static Map<String, String> pathParams(SortableIdentifier publicationIdentifier,
                                                  SortableIdentifier ticketIdentifier,
                                                  SortableIdentifier messageIdentifier) {
        return Map.of(PUBLICATION_IDENTIFIER, publicationIdentifier.toString(),
                      TICKET_IDENTIFIER, ticketIdentifier.toString(),
                      MESSAGE_IDENTIFIER_PATH_PARAMETER, messageIdentifier.toString());
    }
}