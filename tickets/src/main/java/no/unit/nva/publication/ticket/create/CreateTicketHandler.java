package no.unit.nva.publication.ticket.create;

import static no.unit.nva.publication.PublicationServiceConfig.API_HOST;
import static no.unit.nva.publication.PublicationServiceConfig.ENVIRONMENT;
import static no.unit.nva.publication.PublicationServiceConfig.PUBLICATION_PATH;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.PublicationServiceConfig;
import no.unit.nva.publication.exception.TransactionFailedException;
import no.unit.nva.publication.external.services.AuthorizedBackendUriRetriever;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.publication.ticket.TicketDto;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.ForbiddenException;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateTicketHandler extends ApiGatewayHandler<TicketDto, Void> {

    public static final String BACKEND_CLIENT_SECRET_NAME = ENVIRONMENT.readEnv("BACKEND_CLIENT_SECRET_NAME");
    public static final String BACKEND_CLIENT_AUTH_URL = ENVIRONMENT.readEnv("BACKEND_CLIENT_AUTH_URL");
    public static final String LOCATION_HEADER = "Location";
    public static final String PUBLICATION_IDENTIFIER = "publicationIdentifier";
    private final Logger logger = LoggerFactory.getLogger(CreateTicketHandler.class);
    private final TicketService ticketService;
    private final ResourceService resourceService;
    private final PublishingRequestResolver publishingRequestResolver;

    @JacocoGenerated
    public CreateTicketHandler() {
        this(TicketService.defaultService(), ResourceService.defaultService(),
             new PublishingRequestResolver(ResourceService.defaultService(), TicketService.defaultService(),
                                           new AuthorizedBackendUriRetriever(BACKEND_CLIENT_AUTH_URL, BACKEND_CLIENT_SECRET_NAME)));
    }

    public CreateTicketHandler(TicketService ticketService, ResourceService resourceService,
                               PublishingRequestResolver publishingRequestResolver) {
        super(TicketDto.class);
        this.ticketService = ticketService;
        this.resourceService = resourceService;
        this.publishingRequestResolver = publishingRequestResolver;
    }

    @Override
    protected Void processInput(TicketDto input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        var publicationIdentifier = new SortableIdentifier(requestInfo.getPathParameter(PUBLICATION_IDENTIFIER));
        var publication = fetchPublication(publicationIdentifier, getUser(requestInfo), requestInfo);
        var ticketType = input.ticketType();
        var ticket = persistTicket(TicketEntry.requestNewTicket(publication, ticketType));
        var customer = requestInfo.getCurrentCustomer();
        if (isPublishingRequest(ticketType)) {
            publishingRequestResolver.resolve(ticket, publication, customer);
        }
        var ticketLocation = createTicketLocation(publicationIdentifier, ticket);
        addAdditionalHeaders(() -> Map.of(LOCATION_HEADER, ticketLocation));

        return null;
    }

    private static boolean isPublishingRequest(Class<? extends TicketEntry> ticketType) {
        return PublishingRequestCase.class.equals(ticketType);
    }

    @Override
    protected Integer getSuccessStatusCode(TicketDto input, Void output) {
        return HttpURLConnection.HTTP_CREATED;
    }

    private static UserInstance getUser(RequestInfo requestInfo) throws UnauthorizedException {
        return UserInstance.fromRequestInfo(requestInfo);
    }

    private static String createTicketLocation(SortableIdentifier publicationIdentifier, TicketEntry createdTicket) {
        return UriWrapper.fromHost(API_HOST)
                   .addChild(PUBLICATION_PATH)
                   .addChild(publicationIdentifier.toString())
                   .addChild(PublicationServiceConfig.TICKET_PATH)
                   .addChild(createdTicket.getIdentifier().toString())
                   .getUri()
                   .toString();
    }

    private static URI getPublisherId(Publication publication) {
        return Optional.ofNullable(publication.getPublisher()).map(Organization::getId).orElse(null);
    }

    private static boolean hasValidAccessRights(RequestInfo requestInfo) {
        return requestInfo.userIsAuthorized(AccessRight.APPROVE_DOI_REQUEST.toString())
               || requestInfo.userIsAuthorized(AccessRight.REJECT_DOI_REQUEST.toString());
    }

    private TicketEntry persistTicket(TicketEntry newTicket) throws ApiGatewayException {
        return attempt(() -> newTicket.persistNewTicket(ticketService))
                   .orElse(fail -> handleCreationException(fail.getException(), newTicket));
    }

    private TicketEntry handleCreationException(Exception exception, TicketEntry newTicket) throws ApiGatewayException {
        if (exception instanceof TransactionFailedException) {
            return updateAlreadyExistingTicket(newTicket);
        }
        if (exception instanceof RuntimeException) {
            throw (RuntimeException) exception;
        }
        if (exception instanceof ApiGatewayException) {
            throw (ApiGatewayException) exception;
        }
        throw new RuntimeException(exception);
    }

    private TicketEntry updateAlreadyExistingTicket(TicketEntry newTicket) {
        var customerId = newTicket.getCustomerId();
        var resourceIdentifier = newTicket.extractPublicationIdentifier();
        return ticketService.fetchTicketByResourceIdentifier(customerId, resourceIdentifier, newTicket.getClass())
                   .map(this::updateTicket)
                   .orElseThrow();
    }

    private TicketEntry updateTicket(TicketEntry ticket) {
        ticket.persistUpdate(ticketService);
        return ticket;
    }

    private Publication fetchPublication(SortableIdentifier publicationIdentifier, UserInstance user,
                                         RequestInfo requestInfo)
        throws ApiGatewayException {
        return attempt(() -> resourceService.getPublication(user, publicationIdentifier))
                   .or(() -> fetchPublicationForPrivilegedUser(publicationIdentifier, requestInfo))
                   .orElseThrow(fail -> loggingFailureReporter(fail.getException()));
    }

    private boolean userIsAuthorized(RequestInfo requestInfo, Publication publication) throws UnauthorizedException {
        return hasValidAccessRights(requestInfo) && matchingCustomer(requestInfo, publication);
    }

    private boolean matchingCustomer(RequestInfo requestInfo, Publication publication)
        throws UnauthorizedException {
        return Optional.ofNullable(requestInfo.getCurrentCustomer())
                   .map(customer -> customer.equals(getPublisherId(publication)))
                   .orElse(false);
    }

    private Publication fetchPublicationForPrivilegedUser(SortableIdentifier publicationIdentifier,
                                                          RequestInfo requestInfo) throws ApiGatewayException {
        var publication = resourceService.getPublicationByIdentifier(publicationIdentifier);
        if (!userIsAuthorized(requestInfo, publication)) {
            throw new ForbiddenException();
        }
        return publication;
    }

    private ApiGatewayException loggingFailureReporter(Exception exception) {
        logger.error("Request failed: {}", Arrays.toString(exception.getStackTrace()));
        return new ForbiddenException();
    }
}
