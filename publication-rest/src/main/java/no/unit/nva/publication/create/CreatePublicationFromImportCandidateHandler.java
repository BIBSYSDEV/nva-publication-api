package no.unit.nva.publication.create;

import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.util.Objects.isNull;
import static nva.commons.core.attempt.Try.attempt;

import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.api.PublicationResponse;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.publication.exception.NotAuthorizedException;
import no.unit.nva.publication.model.business.importcandidate.ImportCandidate;
import no.unit.nva.publication.model.business.importcandidate.Imported;
import no.unit.nva.publication.model.business.importcandidate.NotImported;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

import java.time.Instant;

public class CreatePublicationFromImportCandidateHandler extends ApiGatewayHandler<ImportCandidate,
        PublicationResponse> {

    public static final String IMPORT_CANDIDATES_TABLE = new Environment().readEnv("IMPORT_CANDIDATES_TABLE_NAME");
    public static final String PUBLICATIONS_TABLE = new Environment().readEnv("RESOURCE_TABLE_NAME");
    public static final String SCOPUS_IDENTIFIER = "scopusIdentifier";
    public static final String ROLLBACK_WENT_WRONG_MESSAGE = "Rollback went wrong";
    public static final String IMPORT_PROCESS_WENT_WRONG = "Import process went wrong";
    public static final String RESOURCE_HAS_ALREADY_BEEN_IMPORTED_ERROR_MESSAGE = "Resource has already been imported";
    public static final String RESOURCE_IS_MISSING_SCOPUS_IDENTIFIER_ERROR_MESSAGE = "Resource is missing scopus "
            + "identifier";
    private final ResourceService candidateService;
    private final ResourceService publicationService;

    @JacocoGenerated
    public CreatePublicationFromImportCandidateHandler() {
        this(ResourceService.defaultService(IMPORT_CANDIDATES_TABLE), ResourceService.defaultService(PUBLICATIONS_TABLE));
    }

    public CreatePublicationFromImportCandidateHandler(ResourceService importCandidateService,
                                                       ResourceService publicationService) {
        super(ImportCandidate.class);
        this.candidateService = importCandidateService;
        this.publicationService = publicationService;
    }

    private static boolean notAuthorizedToProcessImportCandidates(RequestInfo requestInfo) {
        return !requestInfo.userIsAuthorized(AccessRight.PROCESS_IMPORT_CANDIDATE.name());
    }

    @Override
    protected PublicationResponse processInput(ImportCandidate input, RequestInfo requestInfo, Context context)
            throws ApiGatewayException {
        validateAccessRight(requestInfo);
        validateImportCandidate(input);
        var identifier = input.getIdentifier();
        //TODO: refactor this
        return attempt(() -> candidateService.updateImportStatus(identifier, new Imported(Instant.now(), null, null)))
                .map(publicationService::autoImportPublication)
                .map(PublicationResponse::fromPublication)
                .orElseThrow(failure -> rollbackAndThrowException(input));
    }

    @Override
    protected Integer getSuccessStatusCode(ImportCandidate input, PublicationResponse output) {
        return HTTP_CREATED;
    }

    private void validateImportCandidate(ImportCandidate importCandidate) throws BadRequestException {
        if (importCandidate.getImportStatus() instanceof Imported) {
            throw new BadRequestException(RESOURCE_HAS_ALREADY_BEEN_IMPORTED_ERROR_MESSAGE);
        }
        if (isNull(getScopusIdentifier(importCandidate))) {
            throw new BadRequestException(RESOURCE_IS_MISSING_SCOPUS_IDENTIFIER_ERROR_MESSAGE);
        }
    }

    private BadGatewayException rollbackAndThrowException(ImportCandidate input) {
        return attempt(() -> rollbackImportStatusUpdate(input))
                .orElse(fail -> new BadGatewayException(ROLLBACK_WENT_WRONG_MESSAGE));
    }

    private void validateAccessRight(RequestInfo requestInfo) throws NotAuthorizedException {
        if (notAuthorizedToProcessImportCandidates(requestInfo)) {
            throw new NotAuthorizedException();
        }
    }

    private String getScopusIdentifier(ImportCandidate importCandidate) {
        return importCandidate.getAdditionalIdentifiers()
                .stream()
                .filter(this::isScopusIdentifier)
                .map(AdditionalIdentifier::getValue)
                .findFirst()
                .orElse(null);
    }

    private boolean isScopusIdentifier(AdditionalIdentifier identifier) {
        return SCOPUS_IDENTIFIER.equals(identifier.getSource());
    }

    private BadGatewayException rollbackImportStatusUpdate(ImportCandidate importCandidate)
            throws NotFoundException {
        candidateService.updateImportStatus(importCandidate.getIdentifier(), new NotImported());
        return new BadGatewayException(IMPORT_PROCESS_WENT_WRONG);
    }
}
