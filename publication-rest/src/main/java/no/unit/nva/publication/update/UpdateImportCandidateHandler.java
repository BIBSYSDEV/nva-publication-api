package no.unit.nva.publication.update;

import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import no.unit.nva.publication.exception.NotAuthorizedException;
import no.unit.nva.publication.model.business.importcandidate.ImportCandidate;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.Failure;

public class UpdateImportCandidateHandler extends ApiGatewayHandler<ImportCandidate, ImportCandidate> {

    private static final String TABLE_NAME = new Environment().readEnv("TABLE_NAME");
    private final ResourceService importCandidateService;

    @JacocoGenerated
    public UpdateImportCandidateHandler() {
        this(ResourceService.defaultService(TABLE_NAME));
    }

    public UpdateImportCandidateHandler(ResourceService importCandidateService) {
        super(ImportCandidate.class);
        this.importCandidateService = importCandidateService;
    }

    @Override
    protected ImportCandidate processInput(ImportCandidate input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {

        return hasAccessRights(requestInfo)
                   ? attempt(input::getIdentifier)
                         .map(importCandidateService::getImportCandidateByIdentifier)
                         .map(importCandidate -> updateContributors(input, importCandidate))
                         .map(importCandidateService::updateImportCandidate)
                         .orElseThrow(UpdateImportCandidateHandler::getException)
                   : throwNotAuthorized();
    }

    @Override
    protected Integer getSuccessStatusCode(ImportCandidate input, ImportCandidate output) {
        return HttpURLConnection.HTTP_OK;
    }

    private static ApiGatewayException getException(Failure<ImportCandidate> failure) {
        return (ApiGatewayException) failure.getException();
    }

    private ImportCandidate throwNotAuthorized() throws NotAuthorizedException {
        throw new NotAuthorizedException();
    }

    private boolean hasAccessRights(RequestInfo requestInfo) {
        return requestInfo.userIsAuthorized(AccessRight.PROCESS_IMPORT_CANDIDATE.name());
    }

    private ImportCandidate updateContributors(ImportCandidate input, ImportCandidate importCandidate) {
        importCandidate.getEntityDescription().setContributors(input.getEntityDescription().getContributors());
        return importCandidate;
    }
}
