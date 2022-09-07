package no.unit.nva.publication.messages.create;

import static java.util.Objects.isNull;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import com.google.common.net.HttpHeaders;
import java.net.HttpURLConnection;
import java.net.URI;
import java.time.Clock;
import java.util.Map;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.exception.InvalidInputException;
import no.unit.nva.publication.model.MessageDto;
import no.unit.nva.publication.model.business.MessageType;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.impl.MessageService;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public class CreateMessageHandler extends ApiGatewayHandler<CreateMessageRequest, MessageDto> {
    
    public static final String INVALID_MESSAGE_TYPE_ERROR_INFO =
        "messageType may not be null: Allowed values are: " + MessageType.allowedValuesString();
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
    protected MessageDto processInput(CreateMessageRequest input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        Publication publication = fetchExistingPublication(input);
        UserInstance sender = createSender(requestInfo);
        
        URI messageId = sendMessage(input, sender, publication);
        addAdditionalHeaders(() -> locationHeader(messageId.toString()));
        return createDto(messageId);
    }
    
    private MessageDto createDto(URI messageId) {
        var messageDto = new MessageDto();
        messageDto.setMessageId(messageId);
        return messageDto;
    }
    
    @Override
    protected Integer getSuccessStatusCode(CreateMessageRequest input, MessageDto output) {
        return HttpURLConnection.HTTP_CREATED;
    }
    
    @JacocoGenerated
    private static AmazonDynamoDB defaultClient() {
        return AmazonDynamoDBClientBuilder.defaultClient();
    }
    
    private static ResourceService defaultResourceService(AmazonDynamoDB client) {
        return new ResourceService(client, Clock.systemDefaultZone());
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
        throws BadRequestException {
        try {
            validateInput(input);
            var messageIdentifier = trySendMessage(input, sender, publication);
            return MessageDto.constructMessageId(messageIdentifier);
        } catch (InvalidInputException exception) {
            throw handleBadRequests(exception);
        }
    }
    
    private void validateInput(CreateMessageRequest input) {
        if (isNull(input.getMessageType())) {
            throw new InvalidInputException(INVALID_MESSAGE_TYPE_ERROR_INFO);
        }
    }
    
    private SortableIdentifier trySendMessage(CreateMessageRequest input,
                                              UserInstance sender,
                                              Publication publication) {
        return messageService.createMessage(sender, publication, input.getMessage(), input.getMessageType());
    }
    
    private BadRequestException handleBadRequests(InvalidInputException exception) {
        return new BadRequestException(exception.getMessage(), exception);
    }
    
    private UserInstance createSender(RequestInfo requestInfo) throws UnauthorizedException {
        String loggedInUser = requestInfo.getNvaUsername();
        URI orgUri = requestInfo.getCurrentCustomer();
        return UserInstance.create(loggedInUser, orgUri);
    }
    
    private Map<String, String> locationHeader(String messageIdentifier) {
        return Map.of(HttpHeaders.LOCATION, messageIdentifier);
    }
}
