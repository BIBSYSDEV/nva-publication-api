package no.unit.nva.publication.messages.update;

import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.publication.model.business.MessageType.SUPPORT;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import java.io.InputStream;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.messages.MessagesTest;
import no.unit.nva.publication.model.business.Message;
import no.unit.nva.publication.model.business.MessageStatus;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zalando.problem.Problem;

class UpdateMessageHandlerTest extends MessagesTest {
    
    private UpdateMessageHandler handler;
    
    @BeforeEach
    public void setup() {
        super.setup();
        this.handler = new UpdateMessageHandler(messageService);
    }
    
    @Test
    void shouldMarkMessageAsReadWhenSenderIsOwnerATheCuratorReceivesTheMessage()
        throws ApiGatewayException, IOException {
        var publication = createPublication();
        var messageIdentifier = ownerCreatesMessage(publication);
        var originalMessage = messageService.getMessageByIdentifier(messageIdentifier).orElseThrow();
        
        var request = curatorMarksMessageAsRead(publication, messageIdentifier);
        handler.handleRequest(request, output, context);
        var updatedMessage = messageService.getMessageByIdentifier(messageIdentifier).orElseThrow();
        var expectedMessage = constructExpectedMessage(originalMessage, updatedMessage);
        assertThat(updatedMessage, is(equalTo(expectedMessage)));
    }
    
    @Test
    void shouldMarkMessageAsMarkedWhenSenderIsCuratorAndTheOwnerReceivesTheMessage()
        throws ApiGatewayException, IOException {
        var publication = createPublication();
        var messageIdentifier = curatorCreatesMessage(publication);
        var originalMessage = messageService.getMessageByIdentifier(messageIdentifier).orElseThrow();
        var request = ownerMarksMessageAsRead(publication, messageIdentifier);
        handler.handleRequest(request, output, context);
        var updatedMessage = messageService.getMessageByIdentifier(messageIdentifier).orElseThrow();
        var expectedMessage = constructExpectedMessage(originalMessage, updatedMessage);
        assertThat(updatedMessage, is(equalTo(expectedMessage)));
    }
    
    @Test
    void shouldReturnForbiddenWhenOwnerTriesToMarkTheirMessageAsRead()
        throws ApiGatewayException, IOException {
        var publication = createPublication();
        var messageIdentifier = ownerCreatesMessage(publication);
        messageService.getMessageByIdentifier(messageIdentifier).orElseThrow();
        
        var request = ownerMarksMessageAsRead(publication, messageIdentifier);
        handler.handleRequest(request, output, context);
        var response = GatewayResponse.fromOutputStream(output, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_FORBIDDEN)));
    }
    
    @Test
    void shouldReturnForbiddenWhenSomeCuratorSendsTheMessageAndTheSameOrOtherCuratorTriesToMarkTheirMessageAsRead()
        throws ApiGatewayException, IOException {
        var publication = createPublication();
        var messageIdentifier = curatorCreatesMessage(publication);
        messageService.getMessageByIdentifier(messageIdentifier).orElseThrow();
        
        var request = curatorMarksMessageAsRead(publication, messageIdentifier);
        handler.handleRequest(request, output, context);
        var response = GatewayResponse.fromOutputStream(output, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_FORBIDDEN)));
    }
    
    @Test
    void shouldReturnNotFoundWhenTryingToUpdateNonExistingMessage() throws IOException {
        var request = ownerMarksMessageAsRead(randomPublication(), SortableIdentifier.next());
        handler.handleRequest(request, output, context);
        var response = GatewayResponse.fromOutputStream(output, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_NOT_FOUND)));
    }
    
    private InputStream ownerMarksMessageAsRead(Publication publication, SortableIdentifier messageIdentifier)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<UpdateMessageRequest>(JsonUtils.dtoObjectMapper)
            .withNvaUsername(publication.getResourceOwner().getOwner())
            .withBody(new MarkMessageAsReadRequest())
            .withCustomerId(publication.getPublisher().getId())
            .withPathParameters(messagePathParameter(messageIdentifier.toString()))
            .build();
    }
    
    private Message constructExpectedMessage(Message originalMessage, Message updatedMessage) {
        var copy = originalMessage.copy();
        copy.setStatus(MessageStatus.READ);
        copy.setModifiedDate(updatedMessage.getModifiedDate());
        copy.setVersion(updatedMessage.getVersion());
        return copy;
    }
    
    private InputStream curatorMarksMessageAsRead(Publication publication, SortableIdentifier messageIdentifier)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<UpdateMessageRequest>(JsonUtils.dtoObjectMapper)
            .withNvaUsername(randomString())
            .withBody(new MarkMessageAsReadRequest())
            .withAccessRights(publication.getPublisher().getId(), AccessRight.EDIT_OWN_INSTITUTION_RESOURCES.toString())
            .withCustomerId(publication.getPublisher().getId())
            .withPathParameters(messagePathParameter(messageIdentifier.toString()))
            .build();
    }
    
    private SortableIdentifier curatorCreatesMessage(Publication publication) {
        var curator = UserInstance.create(randomString(), publication.getPublisher().getId());
        return messageService.createMessage(curator, publication, randomString(), SUPPORT);
    }
    
    private SortableIdentifier ownerCreatesMessage(Publication publication) {
        return messageService.createMessage(UserInstance.fromPublication(publication), publication, randomString(),
            SUPPORT);
    }
}