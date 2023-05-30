package no.unit.nva.publication.fetch;

import static java.net.HttpURLConnection.HTTP_OK;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import java.time.Clock;
import java.util.function.Function;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.business.ImportCandidate;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.Failure;

public class FetchImportCandidateHandler extends ApiGatewayHandler<Void, ImportCandidate> {

    public static final String IMPORT_CANDIDATE_IDENTIFIER = "importCandidateIdentifier";
    public static final String IMPORT_CANDIDATE_NOT_FOUND_MESSAGE = "Import candidate not found: ";
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
        var identifier = getIdentifier(requestInfo);
        return attempt(() -> resourceService.getImportCandidateByIdentifier(identifier))
                   .orElseThrow(importCandidateNotFound(identifier));
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, ImportCandidate output) {
        return HTTP_OK;
    }

    private static SortableIdentifier getIdentifier(RequestInfo requestInfo) {
        return new SortableIdentifier(requestInfo.getPathParameters().get(IMPORT_CANDIDATE_IDENTIFIER));
    }

    private static Function<Failure<ImportCandidate>, NotFoundException> importCandidateNotFound(
        SortableIdentifier identifier) {
        return failure -> new NotFoundException(IMPORT_CANDIDATE_NOT_FOUND_MESSAGE + identifier);
    }

    @JacocoGenerated
    private static ResourceService defaultResourceService() {
        return new ResourceService(AmazonDynamoDBClientBuilder.defaultClient(), Clock.systemDefaultZone());
    }
}
