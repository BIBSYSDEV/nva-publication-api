package no.unit.nva.publication.fetch;

import static java.net.HttpURLConnection.HTTP_OK;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import java.time.Clock;
import no.unit.nva.publication.RequestUtil;
import no.unit.nva.publication.model.business.ImportCandidate;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.JacocoGenerated;

public class FetchImportCandidateHandler extends ApiGatewayHandler<Void, ImportCandidate> {

    private final ResourceService resourceService;

    @JacocoGenerated
    public FetchImportCandidateHandler() {
        this(defaultResourceService());
    }

    public FetchImportCandidateHandler(ResourceService resourceService) {
        super(Void.class);
        this.resourceService = resourceService;
    }

    @Override
    protected ImportCandidate processInput(Void input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        var identifier = RequestUtil.getIdentifier(requestInfo);
        return resourceService.getImportCandidateByIdentifier(identifier);
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, ImportCandidate output) {
        return HTTP_OK;
    }

    @JacocoGenerated
    private static ResourceService defaultResourceService() {
        return new ResourceService(AmazonDynamoDBClientBuilder.defaultClient(), Clock.systemDefaultZone());
    }
}
