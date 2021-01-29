package no.unit.nva.doirequest.list;

import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.service.impl.DoiRequestService;
import no.unit.nva.publication.storage.model.DoiRequest;
import no.unit.nva.publication.storage.model.UserInstance;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.core.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ListDoiRequestsHandler extends ApiGatewayHandler<Void, Publication[]> {

    public static final String ROLE_QUERY_PARAMETER = "role";
    public static final String CURATOR_ROLE = "Curator";
    public static final String CREATOR_ROLE = "Creator";
    private static final Logger LOGGER = LoggerFactory.getLogger(ListDoiRequestsHandler.class);
    private final DoiRequestService doiRequestService;

    public ListDoiRequestsHandler(Environment environment,
                                  DoiRequestService doiRequestService) {
        super(Void.class, environment, LOGGER);
        this.doiRequestService = doiRequestService;
    }

    @Override
    protected Publication[] processInput(Void input, RequestInfo requestInfo, Context context) {
        URI customerId = requestInfo.getCustomerId().map(URI::create).orElse(null);
        String role = requestInfo.getQueryParameter(ROLE_QUERY_PARAMETER);
        String userId = requestInfo.getFeideId().orElse(null);
        UserInstance userInstance = new UserInstance(userId, customerId);
        if (role.equals(CURATOR_ROLE)) {
            return fetchDoiRequestsForCurator(userInstance);
        } else if (CREATOR_ROLE.equals(role)) {
            return fetchDoiRequestsForUser(userInstance);
        }
        return new Publication[0];
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, Publication[] output) {
        return HttpURLConnection.HTTP_OK;
    }

    private Publication[] fetchDoiRequestsForUser(UserInstance userInstance) {
        List<DoiRequest> doiRequests = doiRequestService.listDoiRequestsForUser(userInstance);
        return convertInternalObjectsToDtos(doiRequests);
    }

    private Publication[] fetchDoiRequestsForCurator(UserInstance userInstance) {
        List<DoiRequest> doiRequests = doiRequestService.listDoiRequestsForPublishedPublications(userInstance);

        return convertInternalObjectsToDtos(doiRequests);
    }

    private Publication[] convertInternalObjectsToDtos(List<DoiRequest> doiRequests) {
        List<Publication> dtos = doiRequests.stream()
            .map(DoiRequest::toPublication)
            .collect(Collectors.toList());
        Publication[] result = new Publication[doiRequests.size()];

        dtos.toArray(result);
        return result;
    }
}
