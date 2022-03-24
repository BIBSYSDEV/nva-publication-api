package no.unit.nva.publication.messages;

import static no.unit.nva.publication.PublicationServiceConfig.EXTERNAL_SERVICES_HTTP_CLIENT;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import com.google.common.net.HttpHeaders;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Clock;
import java.util.Map;
import java.util.Optional;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.exception.BadRequestException;
import no.unit.nva.publication.exception.InvalidInputException;
import no.unit.nva.publication.exception.TransactionFailedException;
import no.unit.nva.publication.model.MessageDto;
import no.unit.nva.publication.service.impl.MessageService;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.storage.model.MessageType;
import no.unit.nva.publication.storage.model.UserInstance;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public class CreateMessageHandler extends ApiGatewayHandler<CreateMessageRequest, Void> {

    private final MessageService messageService;
    private final ResourceService resourceService;

    @JacocoGenerated
    public CreateMessageHandler() {
        this(defaultClient(), new Environment());
    }

    public CreateMessageHandler(AmazonDynamoDB client, Environment environment) {
        this(environment, defaultMessageService(client), defaultResourceService(client));
    }

    public CreateMessageHandler(Environment environment,
                                MessageService messageService,
                                ResourceService resourceService) {
        super(CreateMessageRequest.class, environment);
        this.messageService = messageService;
        this.resourceService = resourceService;
    }

    @Override
    protected Void processInput(CreateMessageRequest input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        Publication publication = fetchExistingPublication(input);
        UserInstance sender = createSender(requestInfo);

        URI messageId = sendMessage(input, sender, publication);
        addAdditionalHeaders(() -> locationHeader(messageId.toString()));

        return null;
    }

    @Override
    protected Integer getSuccessStatusCode(CreateMessageRequest input, Void output) {
        return HttpURLConnection.HTTP_CREATED;
    }

    @JacocoGenerated
    private static AmazonDynamoDB defaultClient() {
        return AmazonDynamoDBClientBuilder.defaultClient();
    }

    private static ResourceService defaultResourceService(AmazonDynamoDB client) {
        return new ResourceService(client,  Clock.systemDefaultZone());
    }

    private static MessageService defaultMessageService(AmazonDynamoDB client) {
        return new MessageService(client, Clock.systemDefaultZone());
    }

    private Publication fetchExistingPublication(CreateMessageRequest input) throws BadRequestException {
        try {
            return resourceService.getPublicationByIdentifier(input.getPublicationIdentifier());
        } catch (NotFoundException e) {
            throw new BadRequestException(e.getMessage(), e);
        }
    }

    private URI sendMessage(CreateMessageRequest input, UserInstance sender, Publication publication)
        throws TransactionFailedException, BadRequestException {
        try {
            var messageIdentifier = trySendMessage(input, sender, publication);
            return MessageDto.constructMessageId(messageIdentifier);
        } catch (InvalidInputException exception) {
            throw handleBadRequests(exception);
        }
    }

    private SortableIdentifier trySendMessage(CreateMessageRequest input, UserInstance sender, Publication publication)
        throws TransactionFailedException {
        MessageType messageType = parseMessageType(input);
        return MessageType.DOI_REQUEST.equals(messageType)
                   ? messageService.createDoiRequestMessage(sender, publication, input.getMessage())
                   : messageService.createSimpleMessage(sender, publication, input.getMessage());
    }

    private MessageType parseMessageType(CreateMessageRequest input) {
        return Optional.ofNullable(input.getMessageType())
            .map(MessageType::parse)
            .orElse(MessageType.SUPPORT);
    }

    private BadRequestException handleBadRequests(InvalidInputException exception) {
        return new BadRequestException(exception.getMessage(), exception);
    }

    private UserInstance createSender(RequestInfo requestInfo) {
        String loggedInUser = requestInfo.getNvaUsername();
        URI orgUri = requestInfo.getCustomerId().map(URI::create).orElseThrow();
        return UserInstance.create(loggedInUser, orgUri);
    }

    private Map<String, String> locationHeader(String messageIdentifier) {
        return Map.of(HttpHeaders.LOCATION, messageIdentifier);
    }
}
