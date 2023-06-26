package no.unit.nva.publication.update;

import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.exception.NotAuthorizedException;
import no.unit.nva.publication.model.business.importcandidate.ImportCandidate;
import no.unit.nva.publication.model.business.importcandidate.ImportStatus;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public class UpdateImportStatusHandler extends ApiGatewayHandler<ImportStatus, ImportCandidate> {

    public static final String IMPORT_CANDIDATE_IDENTIFIER_PATH_PARAMETER = "importCandidateIdentifier";
    public static final String TABLE_NAME = new Environment().readEnv("TABLE_NAME");
    private final ResourceService importCandidateService;

    @JacocoGenerated
    public UpdateImportStatusHandler() {
        this(ResourceService.defaultService(TABLE_NAME));
    }

    public UpdateImportStatusHandler(ResourceService importCandidateService) {
        super(ImportStatus.class);
        this.importCandidateService = importCandidateService;
    }

    @Override
    protected ImportCandidate processInput(ImportStatus input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        validateAccessRights(requestInfo);
        var identifier = getIdentifier(requestInfo);
        return importCandidateService.updateImportStatus(identifier, input);
    }

    @Override
    protected Integer getSuccessStatusCode(ImportStatus input, ImportCandidate output) {
        return HttpURLConnection.HTTP_OK;
    }

    private SortableIdentifier getIdentifier(RequestInfo requestInfo) {
        var identifier = requestInfo.getPathParameters().get(IMPORT_CANDIDATE_IDENTIFIER_PATH_PARAMETER);
        return new SortableIdentifier(identifier);
    }

    private boolean isNotAuthorized(RequestInfo requestInfo) {
        return !requestInfo.userIsAuthorized(AccessRight.PROCESS_IMPORT_CANDIDATE.name());
    }

    private void validateAccessRights(RequestInfo requestInfo) throws NotAuthorizedException {
        if (isNotAuthorized(requestInfo)) {
            throw new NotAuthorizedException();
        }
    }
}
