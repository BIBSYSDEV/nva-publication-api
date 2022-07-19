package no.unit.nva.doirequest.list;

import static no.unit.nva.doirequest.DoiRequestRelatedAccessRights.APPROVE_DOI_REQUEST;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import java.net.URI;
import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.model.DoiRequestMessage;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.model.MessageDto;
import no.unit.nva.publication.model.ResourceConversation;
import no.unit.nva.publication.service.impl.DoiRequestService;
import no.unit.nva.publication.service.impl.MessageService;
import no.unit.nva.publication.storage.model.DoiRequest;
import no.unit.nva.publication.storage.model.MessageType;
import no.unit.nva.publication.storage.model.UserInstance;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public class ListDoiRequestsHandler extends ApiGatewayHandler<Void, Publication[]> {
    
    public static final String ROLE_QUERY_PARAMETER = "role";
    public static final String CURATOR_ROLE = "Curator";
    public static final String CREATOR_ROLE = "Creator";
    
    private final DoiRequestService doiRequestService;
    private final MessageService messageService;
    
    @JacocoGenerated
    public ListDoiRequestsHandler() {
        this(AmazonDynamoDBClientBuilder.defaultClient(), Clock.systemDefaultZone());
    }
    
    @JacocoGenerated
    private ListDoiRequestsHandler(AmazonDynamoDB client, Clock clock) {
        this(new Environment(), defaultDoiRequestService(client, clock), defaultMessageService(client, clock));
    }
    
    public ListDoiRequestsHandler(Environment environment,
                                  DoiRequestService doiRequestService,
                                  MessageService messageService) {
        super(Void.class, environment);
        this.doiRequestService = doiRequestService;
        this.messageService = messageService;
    }
    
    @Override
    protected Publication[] processInput(Void input, RequestInfo requestInfo, Context context)
        throws BadRequestException, UnauthorizedException {
        URI customerId = requestInfo.getCurrentCustomer();
        String requestedRole = requestInfo.getQueryParameter(ROLE_QUERY_PARAMETER);
        String userId = requestInfo.getNvaUsername();
        
        UserInstance userInstance = UserInstance.create(userId, customerId);
        if (userIsACurator(requestedRole, requestInfo)) {
            return fetchDoiRequestsForCurator(userInstance);
        } else if (userIsACreator(requestedRole)) {
            return fetchDoiRequestsForUser(userInstance);
        }
        
        return emptyResult();
    }
    
    @Override
    protected Integer getSuccessStatusCode(Void input, Publication[] output) {
        return HttpURLConnection.HTTP_OK;
    }
    
    @JacocoGenerated
    private static MessageService defaultMessageService(AmazonDynamoDB client, Clock clock) {
        return new MessageService(client, clock);
    }
    
    @JacocoGenerated
    private static DoiRequestService defaultDoiRequestService(AmazonDynamoDB client, Clock clock) {
        return new DoiRequestService(client, clock);
    }
    
    private Publication[] emptyResult() {
        return new Publication[0];
    }
    
    private boolean userIsACreator(String requestedRole) {
        return CREATOR_ROLE.equals(requestedRole);
    }
    
    private boolean userIsACurator(String requestedRole, RequestInfo requestInfo) {
        return CURATOR_ROLE.equals(requestedRole)
               && requestInfo.userIsAuthorized(APPROVE_DOI_REQUEST.toString());
    }
    
    private Publication[] fetchDoiRequestsForUser(UserInstance userInstance) {
        List<DoiRequest> doiRequests = doiRequestService.listDoiRequestsForUser(userInstance);
        return addDoiRequestMessagesToDoiRequests(doiRequests);
    }
    
    private Publication[] fetchDoiRequestsForCurator(UserInstance userInstance) {
        List<DoiRequest> doiRequests = doiRequestService.listDoiRequestsForPublishedPublications(userInstance);
        return addDoiRequestMessagesToDoiRequests(doiRequests);
    }
    
    private Publication[] addDoiRequestMessagesToDoiRequests(List<DoiRequest> doiRequests) {
        List<Publication> publicationDtos = convertDoiRequestsToPublicationDtos(doiRequests);
        List<Publication> enrichedWithMessages = enrichPublicationDtosWithDoiRequestMessages(publicationDtos);
        return publicationListToPublicationArray(enrichedWithMessages);
    }
    
    private List<Publication> enrichPublicationDtosWithDoiRequestMessages(List<Publication> dtos) {
        return dtos.stream()
            .map(this::addMessagesToPublicationDto)
            .sorted(sortByOldestDoiRequestFirst())
            .collect(Collectors.toList());
    }
    
    private Comparator<Publication> sortByOldestDoiRequestFirst() {
        return Comparator.comparing(p -> p.getDoiRequest().getCreatedDate());
    }
    
    private Publication addMessagesToPublicationDto(Publication dto) {
        Stream<ResourceConversation> messages =
            messageService.getMessagesForResource(UserInstance.fromPublication(dto), dto.getIdentifier()).stream();
        List<DoiRequestMessage> doiRequestMessages = transformToLegacyDoiRequestMessagesDto(messages);
        dto.getDoiRequest().setMessages(doiRequestMessages);
        return dto;
    }
    
    private List<DoiRequestMessage> transformToLegacyDoiRequestMessagesDto(
        Stream<ResourceConversation> resourceConversations) {
        return resourceConversations
            .map(conversations -> conversations.getMessageCollectionOfType(MessageType.DOI_REQUEST))
            .flatMap(messageCollection -> messageCollection.getMessages().stream())
            .map(this::toDoiRequestMessage)
            .sorted(sortByTimeOldestFirst())
            .collect(Collectors.toList());
    }
    
    private Comparator<DoiRequestMessage> sortByTimeOldestFirst() {
        return Comparator.comparing(DoiRequestMessage::getTimestamp);
    }
    
    private DoiRequestMessage toDoiRequestMessage(MessageDto message) {
        return new DoiRequestMessage.Builder()
            .withText(message.getText())
            .withTimestamp(message.getDate())
            .withAuthor(message.getSenderIdentifier())
            .build();
    }
    
    private Publication[] publicationListToPublicationArray(List<Publication> dtos) {
        return dtos.toArray(Publication[]::new);
    }
    
    private List<Publication> convertDoiRequestsToPublicationDtos(List<DoiRequest> doiRequests) {
        return doiRequests.stream()
            .map(DoiRequest::toPublication)
            .collect(Collectors.toList());
    }
}
