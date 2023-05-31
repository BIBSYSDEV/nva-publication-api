package no.unit.nva.publication.create;

import static java.net.HttpURLConnection.HTTP_OK;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.api.PublicationResponse;
import no.unit.nva.publication.exception.NotAuthorizedException;
import no.unit.nva.publication.model.business.ImportCandidate;
import no.unit.nva.publication.model.business.ImportStatus;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadGatewayException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public class CreatePublicationFromImportCandidateHandler extends ApiGatewayHandler<ImportCandidate,
                                                                                      PublicationResponse> {

    public static final String IMPORT_CANDIDATES_TABLE = new Environment().readEnv("IMPORT_CANDIDATES_TABLE");
    public static final String PUBLICATIONS_TABLE = new Environment().readEnv("TABLE_NAME");
    public static final String ROLLBACK_WENT_WRONG_MESSAGE = "Rollback went wrong";
    public static final String IMPORT_PROCESS_WENT_WRONG = "Import process went wrong";
    private final ResourceService importCandidatesService;
    private final ResourceService publicationService;

    @JacocoGenerated
    public CreatePublicationFromImportCandidateHandler() {
        this(ResourceService.defaultService(), ResourceService.defaultService(PUBLICATIONS_TABLE));
    }

    public CreatePublicationFromImportCandidateHandler(ResourceService importCandidateService,
                                                       ResourceService publicationService) {
        super(ImportCandidate.class);
        this.importCandidatesService = importCandidateService;
        this.publicationService = publicationService;
    }

    @Override
    protected PublicationResponse processInput(ImportCandidate input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        validateAccessRight(requestInfo);
        var identifier = input.getIdentifier();
        return attempt(() -> importCandidatesService.updateImportStatus(identifier, ImportStatus.IMPORTED))
                   .map(ImportCandidate::toPublication)
                   .map(publicationService::autoImportPublication)
                   .map(PublicationResponse::fromPublication)
                   .orElseThrow(failure -> attempt(() -> rollbackAllUpdates(input)).orElse(fail -> rollbackWentWrong()));
    }

    @Override
    protected Integer getSuccessStatusCode(ImportCandidate input, PublicationResponse output) {
        return HTTP_OK;
    }

    private static boolean notAuthorizedToProcessImportCandidates(RequestInfo requestInfo) {
        return !requestInfo.userIsAuthorized(AccessRight.PROCESS_IMPORT_CANDIDATE.name());
    }

    private BadGatewayException rollbackWentWrong() {
        return new BadGatewayException(ROLLBACK_WENT_WRONG_MESSAGE);
    }

    private void validateAccessRight(RequestInfo requestInfo) throws NotAuthorizedException {
        if (notAuthorizedToProcessImportCandidates(requestInfo)) {
            throw new NotAuthorizedException();
        }
    }

    private BadGatewayException rollbackAllUpdates(ImportCandidate importCandidate)
        throws NotFoundException {
        importCandidatesService.updateImportStatus(importCandidate.getIdentifier(), ImportStatus.NOT_IMPORTED);
        return new BadGatewayException(IMPORT_PROCESS_WENT_WRONG);
    }
}
