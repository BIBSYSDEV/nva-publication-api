package no.unit.nva.doirequest.create;

import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import java.net.URI;
import java.time.Clock;
import java.util.Map;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.service.impl.DoiRequestService;
import no.unit.nva.publication.service.impl.UserInstance;
import no.unit.nva.publication.service.impl.exceptions.BadRequestException;
import no.unit.nva.publication.service.impl.exceptions.InternalServerErrorException;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.core.Environment;
import nva.commons.core.attempt.Failure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateDoiRequestHandler extends ApiGatewayHandler<CreateDoiRequest, Void> {

    public static final String DOI_ALREADY_EXISTS_ERROR = "A Doi request already exists";
    private static final Logger LOGGER = LoggerFactory.getLogger(CreateDoiRequestHandler.class);
    private final DoiRequestService doiRequestService;

    public CreateDoiRequestHandler() {
        this(new DoiRequestService(AmazonDynamoDBClientBuilder.defaultClient(),
                Clock.systemDefaultZone()),
            new Environment());
    }

    public CreateDoiRequestHandler(DoiRequestService requestService, Environment environment) {
        super(CreateDoiRequest.class, environment, LOGGER);
        this.doiRequestService = requestService;
    }

    @Override
    protected Void processInput(CreateDoiRequest input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        URI customerId = requestInfo.getCustomerId().map(URI::create).orElse(null);
        String user = requestInfo.getFeideId().orElse(null);
        UserInstance userInstance = new UserInstance(user, customerId);
        SortableIdentifier doiRequestIdentifier = createDoiRequest(input, userInstance);
        setAdditionalHeadersSupplier(() -> additionalHeaders(doiRequestIdentifier));
        return null;
    }

    @Override
    protected Integer getSuccessStatusCode(CreateDoiRequest input, Void output) {
        return HttpURLConnection.HTTP_CREATED;
    }

    private SortableIdentifier createDoiRequest(CreateDoiRequest input, UserInstance userInstance)
        throws ApiGatewayException {
        return attempt(input::getResourceIdentifier)
            .map(identifier -> doiRequestService.createDoiRequest(userInstance, identifier))
            .orElseThrow(this::handleError);
    }

    private ApiGatewayException handleError(Failure<SortableIdentifier> fail) {
        Exception exception = fail.getException();
        if (exception instanceof ConflictException) {
            return new BadRequestException(DOI_ALREADY_EXISTS_ERROR);
        } else if (exception instanceof ApiGatewayException) {
            return (ApiGatewayException) fail.getException();
        } else {
            return new InternalServerErrorException(fail.getException());
        }
    }

    private Map<String, String> additionalHeaders(SortableIdentifier doiRequestIdentifier) {
        return Map.of(
            "Location", "doi-request/" + doiRequestIdentifier.toString()
        );
    }
}
