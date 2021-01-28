package no.unit.nva.doirequest.list;

import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.List;
import no.unit.nva.publication.service.impl.DoiRequestService;
import no.unit.nva.publication.storage.model.DoiRequest;
import no.unit.nva.publication.storage.model.UserInstance;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ListDoiRequestsHandler extends ApiGatewayHandler<Void, DoiRequest[]> {

    public static final String ROLE_QUERY_PARAMETER = "role";
    public static final String CURATOR_ROLE = "Curator";
    private static final Logger LOGGER = LoggerFactory.getLogger(ListDoiRequestsHandler.class);
    private final DoiRequestService doiRequestService;

    public ListDoiRequestsHandler(Environment environment,
                                  DoiRequestService doiRequestService) {
        super(Void.class, environment, LOGGER);
        this.doiRequestService = doiRequestService;
    }

    @Override
    protected DoiRequest[] processInput(Void input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        URI customerId = requestInfo.getCustomerId().map(URI::create).orElse(null);
        String role = requestInfo.getQueryParameter(ROLE_QUERY_PARAMETER);
        String userId = requestInfo.getFeideId().orElse(null);
        UserInstance userInstance = new UserInstance(userId, customerId);
        if (role.equals(CURATOR_ROLE)) {
            List<DoiRequest> doiRequests = doiRequestService.listDoiRequestsForPublishedPublications(userInstance);
            DoiRequest[] result = new DoiRequest[doiRequests.size()];

            doiRequests.toArray(result);
            return result;
        }
        return new DoiRequest[0];
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, DoiRequest[] output) {
        return HttpURLConnection.HTTP_OK;
    }
}
