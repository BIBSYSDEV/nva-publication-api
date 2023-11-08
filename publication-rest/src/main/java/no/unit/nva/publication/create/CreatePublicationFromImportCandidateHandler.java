package no.unit.nva.publication.create;

import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.util.Objects.isNull;
import static no.unit.nva.publication.PublicationServiceConfig.DEFAULT_S3_CLIENT;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.URI;
import no.unit.nva.api.PublicationResponse;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.Organization.Builder;
import no.unit.nva.model.Publication;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.Username;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.publication.exception.NotAuthorizedException;
import no.unit.nva.publication.model.business.importcandidate.CandidateStatus;
import no.unit.nva.publication.model.business.importcandidate.ImportCandidate;
import no.unit.nva.publication.model.business.importcandidate.ImportStatus;
import no.unit.nva.publication.model.business.importcandidate.ImportStatusFactory;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;

public class CreatePublicationFromImportCandidateHandler extends ApiGatewayHandler<ImportCandidate,
                                                                                      PublicationResponse> {

    public static final String IMPORT_CANDIDATES_TABLE = new Environment().readEnv("IMPORT_CANDIDATES_TABLE_NAME");
    public static final String PUBLICATIONS_TABLE = new Environment().readEnv("RESOURCE_TABLE_NAME");
    private final String persistedStorageBucket;
    private final String importCandidateStorageBucket;
    public static final String API_HOST = new Environment().readEnv("API_HOST");
    public static final String SCOPUS_IDENTIFIER = "scopusIdentifier";
    public static final String ROLLBACK_WENT_WRONG_MESSAGE = "Rollback went wrong";
    public static final String IMPORT_PROCESS_WENT_WRONG = "Import process went wrong";
    public static final String RESOURCE_HAS_ALREADY_BEEN_IMPORTED_ERROR_MESSAGE = "Resource has already been imported";
    public static final String RESOURCE_IS_MISSING_SCOPUS_IDENTIFIER_ERROR_MESSAGE =
        "Resource is missing scopus identifier";
    public static final String PUBLICATION = "publication";
    private final ResourceService candidateService;
    private final ResourceService publicationService;
    private final S3Client s3;

    @JacocoGenerated
    public CreatePublicationFromImportCandidateHandler() {
        this(ResourceService.defaultService(IMPORT_CANDIDATES_TABLE),
             ResourceService.defaultService(PUBLICATIONS_TABLE),
             DEFAULT_S3_CLIENT);
    }

    public CreatePublicationFromImportCandidateHandler(ResourceService importCandidateService,
                                                       ResourceService publicationService,
                                                       S3Client s3) {
        super(ImportCandidate.class);
        this.candidateService = importCandidateService;
        this.publicationService = publicationService;
        this.s3 = s3;
        this.persistedStorageBucket = environment.readEnv("NVA_PERSISTED_STORAGE_BUCKET_NAME");
        this.importCandidateStorageBucket = environment.readEnv("IMPORT_CANDIDATES_STORAGE_BUCKET");
    }

    @Override
    protected PublicationResponse processInput(ImportCandidate input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {

        validateAccessRight(requestInfo);
        validateImportCandidate(input);

        return attempt(input::getIdentifier)
                   .map(candidateService::getImportCandidateByIdentifier)
                   .map(candidate -> injectOrganizationAndOwner(requestInfo, candidate))
                   .map(publicationService::autoImportPublication)
                   .map(this::copyArtifacts)
                   .map(CreatePublicationFromImportCandidateHandler::toPublicationUriIdentifier)
                   .map(identifier -> candidateService.updateImportStatus(input.getIdentifier(),
                                                                          toImportStatus(requestInfo, identifier)))
                   .map(importCandidate -> publicationService.getPublicationByIdentifier(
                       extractPublicationId(importCandidate)))
                   .map(PublicationResponse::fromPublication)
                   .orElseThrow(failure -> rollbackAndThrowException(input));
    }

    private Publication copyArtifacts(Publication publication) {
        publication.getAssociatedArtifacts().stream()
            .filter(File.class::isInstance)
            .forEach(a -> s3.copyObject(CopyObjectRequest.builder()
                                            .sourceBucket(importCandidateStorageBucket)
                                            .sourceKey(((File) a).getIdentifier().toString())
                                            .destinationBucket(persistedStorageBucket)
                                            .destinationKey(((File) a).getIdentifier().toString())
                                            .build()));
        return publication;
    }

    private static SortableIdentifier extractPublicationId(ImportCandidate importCandidate) {
        var identifier = UriWrapper.fromUri(importCandidate.getImportStatus().nvaPublicationId())
                             .getLastPathElement();
        return new SortableIdentifier(identifier);
    }

    private ImportCandidate injectOrganizationAndOwner(RequestInfo requestInfo, ImportCandidate importCandidate)
        throws UnauthorizedException {
        var organization = new Builder().withId(requestInfo.getCurrentCustomer()).build();
        return importCandidate.copyImportCandidate()
                   .withPublisher(organization)
                   .withResourceOwner(new ResourceOwner(new Username(requestInfo.getUserName()), organization.getId()))
                   .build();
    }

    private static ImportStatus toImportStatus(RequestInfo requestInfo, URI uri) throws UnauthorizedException {
        return ImportStatusFactory.createImported(new Username(requestInfo.getUserName()), uri);
    }

    private static URI toPublicationUriIdentifier(Publication publication) {
        return UriWrapper.fromHost(API_HOST)
                   .addChild(PUBLICATION)
                   .addChild(publication.getIdentifier().toString())
                   .getUri();
    }

    @Override
    protected Integer getSuccessStatusCode(ImportCandidate input, PublicationResponse output) {
        return HTTP_CREATED;
    }

    private static boolean notAuthorizedToProcessImportCandidates(RequestInfo requestInfo) {
        return !requestInfo.userIsAuthorized(AccessRight.PROCESS_IMPORT_CANDIDATE.name());
    }

    private void validateImportCandidate(ImportCandidate importCandidate) throws BadRequestException {
        if (CandidateStatus.IMPORTED.equals(importCandidate.getImportStatus().candidateStatus())) {
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
        return SCOPUS_IDENTIFIER.equals(identifier.getSourceName());
    }

    private BadGatewayException rollbackImportStatusUpdate(ImportCandidate importCandidate)
        throws NotFoundException {
        candidateService.updateImportStatus(importCandidate.getIdentifier(), ImportStatusFactory.createNotImported());
        return new BadGatewayException(IMPORT_PROCESS_WENT_WRONG);
    }
}
