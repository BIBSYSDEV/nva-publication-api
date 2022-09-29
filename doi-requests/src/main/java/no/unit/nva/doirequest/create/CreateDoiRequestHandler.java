package no.unit.nva.doirequest.create;

import static java.util.Objects.isNull;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.exception.InternalErrorException;
import no.unit.nva.publication.exception.TransactionFailedException;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.MessageType;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.impl.MessageService;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.StringUtils;
import nva.commons.core.attempt.Failure;

public class CreateDoiRequestHandler extends ApiGatewayHandler<CreateDoiRequest, Void> {
    
    public static final String USER_IS_NOT_OWNER_ERROR = "User does not own the specific publication";
    
    private static final Clock CLOCK = Clock.systemDefaultZone();
    public static final String PUBLICATION_HAS_DOI_ALREADY = "Publication has already been assigned a DOI";
    public static final String RECENT_DOI_REQUEST_ERROR = "A new DoiRequest was created recently";
    private final TicketService ticketService;
    private final MessageService messageService;
    private final ResourceService resourceService;
    
    @JacocoGenerated
    public CreateDoiRequestHandler() {
        this(AmazonDynamoDBClientBuilder.defaultClient(), CLOCK);
    }
    
    @JacocoGenerated
    private CreateDoiRequestHandler(AmazonDynamoDB client, Clock clock) {
        this(
            new ResourceService(client, clock),
            new TicketService(client),
            new MessageService(client, clock),
            new Environment());
    }
    
    public CreateDoiRequestHandler(ResourceService resourceService,
                                   TicketService requestService,
                                   MessageService messageService,
                                   Environment environment) {
        super(CreateDoiRequest.class, environment);
        this.resourceService = resourceService;
        this.messageService = messageService;
        this.ticketService = requestService;
    }
    
    @Override
    protected Void processInput(CreateDoiRequest input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        UserInstance owner = extractUserInstance(requestInfo);
        Publication publication = fetchPublication(input, owner);
        var createdTicket = createDoiRequest(publication);
        sendMessage(input, owner, publication);
        addAdditionalHeaders(() -> additionalHeaders(createdTicket.getIdentifier()));
        return null;
    }
    
    @Override
    protected Integer getSuccessStatusCode(CreateDoiRequest input, Void output) {
        return HttpURLConnection.HTTP_CREATED;
    }
    
    private UserInstance extractUserInstance(RequestInfo requestInfo) throws UnauthorizedException {
        URI customerId = requestInfo.getCurrentCustomer();
        String user = requestInfo.getNvaUsername();
        return UserInstance.create(user, customerId);
    }
    
    private Publication fetchPublication(CreateDoiRequest input, UserInstance owner) throws ApiGatewayException {
        try {
            return resourceService.getPublication(owner, input.getResourceIdentifier());
        } catch (NotFoundException notFoundException) {
            throw new BadRequestException(USER_IS_NOT_OWNER_ERROR);
        }
    }
    
    private void sendMessage(CreateDoiRequest input, UserInstance owner, Publication publication) {
        String message = input.getMessage();
        if (StringUtils.isNotBlank(message)) {
            messageService.createMessage(owner, publication, message, MessageType.DOI_REQUEST);
        }
    }
    
    private DoiRequest createDoiRequest(Publication publication) throws ApiGatewayException {
        return attempt(() -> DoiRequest.fromPublication(publication))
                   .map(createTicketRequest -> ticketService.createTicket(createTicketRequest, DoiRequest.class))
                   .or(() -> handleAlreadyExistingDoiRequestError(publication))
                   .orElseThrow(this::handleError);
    }
    
    private ApiGatewayException handleError(Failure<DoiRequest> fail) {
        Exception exception = fail.getException();
        if (exception instanceof TransactionFailedException) {
            throw new RuntimeException(exception);
        } else if (exception instanceof ApiGatewayException) {
            return (ApiGatewayException) fail.getException();
        } else {
            return new InternalErrorException(fail.getException());
        }
    }
    
    private DoiRequest handleAlreadyExistingDoiRequestError(Publication publication) throws BadRequestException {
        if (publicationDoesNotHaveADoi(publication)) {
            var customerId = publication.getPublisher().getId();
            var identifier = publication.getIdentifier();
            return (DoiRequest) refreshExistingExistingTicket(customerId, identifier);
        }
        throw new BadRequestException(PUBLICATION_HAS_DOI_ALREADY);
    }
    
    private boolean publicationDoesNotHaveADoi(Publication publication) {
        return isNull(publication.getDoi());
    }
    
    private TicketEntry refreshExistingExistingTicket(URI customerId, SortableIdentifier identifier)
        throws BadRequestException {
        var ticket = ticketService.fetchTicketByResourceIdentifier(customerId, identifier, DoiRequest.class)
                         .orElseThrow();
        if (doiRequestSatisfiesBasicRuleProtectingUsFromCreatingLargeAmountsOfHangingDraftDoiRequests(ticket)) {
            return ticketService.refreshTicket(ticket);
        }
        throw new BadRequestException(RECENT_DOI_REQUEST_ERROR);
    }
    
    private static boolean doiRequestSatisfiesBasicRuleProtectingUsFromCreatingLargeAmountsOfHangingDraftDoiRequests(
        DoiRequest ticket) {
        //TODO replace with Duration.ofDays(1)
        return ticket.getModifiedDate().isBefore(Instant.now().minus(Duration.ofMinutes(1)));
    }
    
    private Map<String, String> additionalHeaders(SortableIdentifier doiRequestIdentifier) {
        return Map.of(
            "Location", "doi-request/" + doiRequestIdentifier.toString()
        );
    }
}
