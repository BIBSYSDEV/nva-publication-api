package no.unit.nva.doirequest.create;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import java.net.URI;
import java.time.Clock;
import java.util.Map;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.service.impl.DoiRequestService;
import no.unit.nva.publication.service.impl.UserInstance;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateDoiRequestHandler extends ApiGatewayHandler<CreateDoiRequest, Void> {

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
        SortableIdentifier doiRequestIdentifier = doiRequestService.createDoiRequest(
            userInstance,
            input.getResourceIdentifier()
        );
        setAdditionalHeadersSupplier(() -> additionalHeaders(doiRequestIdentifier));
        return null;
    }

    @Override
    protected Integer getSuccessStatusCode(CreateDoiRequest input, Void output) {
        return HttpURLConnection.HTTP_CREATED;
    }

    private Map<String, String> additionalHeaders(SortableIdentifier doiRequestIdentifier) {
        return Map.of(
            "Location", "doi-request/" + doiRequestIdentifier.toString()
        );
    }
}
