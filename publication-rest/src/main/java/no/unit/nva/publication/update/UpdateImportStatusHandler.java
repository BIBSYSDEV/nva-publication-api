package no.unit.nva.publication.update;

import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import java.time.Instant;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Username;
import no.unit.nva.publication.ImportStatusDto;
import no.unit.nva.publication.exception.NotAuthorizedException;
import no.unit.nva.publication.model.business.importcandidate.ImportCandidate;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public class UpdateImportStatusHandler extends ApiGatewayHandler<ImportStatusDto, ImportCandidate> {

    public static final String IMPORT_CANDIDATE_IDENTIFIER_PATH_PARAMETER = "importCandidateIdentifier";
    public static final String TABLE_NAME = new Environment().readEnv("TABLE_NAME");
    private final ResourceService importCandidateService;

    @JacocoGenerated
    public UpdateImportStatusHandler() {
        this(ResourceService.defaultService(TABLE_NAME), new Environment());
    }

    public UpdateImportStatusHandler(ResourceService importCandidateService, Environment environment) {
        super(ImportStatusDto.class, environment);
        this.importCandidateService = importCandidateService;
    }

    @Override
    protected void validateRequest(ImportStatusDto importStatusDto, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        validateAccessRights(requestInfo);
    }

    @Override
    protected ImportCandidate processInput(ImportStatusDto input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        var identifier = getIdentifier(requestInfo);
        var importStatus = input.toImportStatus().copy()
                               .withSetBy(new Username(requestInfo.getUserName()))
                               .withModifiedDate(Instant.now())
                               .build();
        return importCandidateService.updateImportStatus(identifier, importStatus);
    }

    @Override
    protected Integer getSuccessStatusCode(ImportStatusDto input, ImportCandidate output) {
        return HttpURLConnection.HTTP_OK;
    }

    private SortableIdentifier getIdentifier(RequestInfo requestInfo) {
        var identifier = requestInfo.getPathParameters().get(IMPORT_CANDIDATE_IDENTIFIER_PATH_PARAMETER);
        return new SortableIdentifier(identifier);
    }

    private boolean isNotAuthorized(RequestInfo requestInfo) {
        return !requestInfo.userIsAuthorized(AccessRight.MANAGE_IMPORT);
    }

    private void validateAccessRights(RequestInfo requestInfo) throws NotAuthorizedException {
        if (isNotAuthorized(requestInfo)) {
            throw new NotAuthorizedException();
        }
    }
}
