package no.unit.nva.doirequest.create;

import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import java.net.URI;
import java.time.Clock;
import java.util.Map;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.exception.BadRequestException;
import no.unit.nva.publication.exception.InternalErrorException;
import no.unit.nva.publication.exception.TransactionFailedException;
import no.unit.nva.publication.service.impl.DoiRequestService;
import no.unit.nva.publication.service.impl.MessageService;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.storage.model.UserInstance;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.StringUtils;
import nva.commons.core.attempt.Failure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateDoiRequestHandler extends ApiGatewayHandler<CreateDoiRequest, Void> {

    public static final String DOI_ALREADY_EXISTS_ERROR = "A DOI request already exists";
    public static final String USER_IS_NOT_OWNER_ERROR = "User does not own the specific publication";
    private static final Logger LOGGER = LoggerFactory.getLogger(CreateDoiRequestHandler.class);
    private final DoiRequestService doiRequestService;
    private final MessageService messageService;
    private final ResourceService resourceService;

    @JacocoGenerated
    public CreateDoiRequestHandler() {
        this(AmazonDynamoDBClientBuilder.defaultClient(), Clock.systemDefaultZone());
    }

    @JacocoGenerated
    private CreateDoiRequestHandler(AmazonDynamoDB client, Clock clock) {
        this(
            new ResourceService(client, clock),
            new DoiRequestService(client, clock),
            new MessageService(client, clock),
            new Environment());
    }

    public CreateDoiRequestHandler(ResourceService resourceService,
                                   DoiRequestService requestService,
                                   MessageService messageService,
                                   Environment environment) {
        super(CreateDoiRequest.class, environment, LOGGER);
        this.resourceService = resourceService;
        this.messageService = messageService;
        this.doiRequestService = requestService;
    }

    @Override
    protected Void processInput(CreateDoiRequest input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        URI customerId = requestInfo.getCustomerId().map(URI::create).orElse(null);
        String user = requestInfo.getFeideId().orElse(null);
        UserInstance owner = new UserInstance(user, customerId);
        Publication publication = fetchPublication(input, owner);
        SortableIdentifier doiRequestIdentifier = createDoiRequest(publication);
        sendMessage(input, owner, publication);
        addAdditionalHeaders(() -> additionalHeaders(doiRequestIdentifier));
        return null;
    }

    @Override
    protected Integer getSuccessStatusCode(CreateDoiRequest input, Void output) {
        return HttpURLConnection.HTTP_CREATED;
    }

    private Publication fetchPublication(CreateDoiRequest input, UserInstance owner) throws ApiGatewayException {
        try {
            return resourceService.getPublication(owner, input.getResourceIdentifier());
        } catch (NotFoundException notFoundException) {
            throw new BadRequestException(USER_IS_NOT_OWNER_ERROR);
        }
    }

    private void sendMessage(CreateDoiRequest input, UserInstance owner, Publication publication)
        throws TransactionFailedException {
        String message = input.getMessage();
        if (StringUtils.isNotBlank(message)) {
            messageService.createDoiRequestMessage(owner, publication, message);
        }
    }

    private SortableIdentifier createDoiRequest(Publication publication)
        throws ApiGatewayException {
        return attempt(() -> doiRequestService.createDoiRequest(publication))
                   .orElseThrow(this::handleError);
    }

    private ApiGatewayException handleError(Failure<SortableIdentifier> fail) {
        Exception exception = fail.getException();
        if (exception instanceof TransactionFailedException) {
            return new BadRequestException(DOI_ALREADY_EXISTS_ERROR);
        } else if (exception instanceof ApiGatewayException) {
            return (ApiGatewayException) fail.getException();
        } else {
            return new InternalErrorException(fail.getException());
        }
    }

    private Map<String, String> additionalHeaders(SortableIdentifier doiRequestIdentifier) {
        return Map.of(
            "Location", "doi-request/" + doiRequestIdentifier.toString()
        );
    }
}
