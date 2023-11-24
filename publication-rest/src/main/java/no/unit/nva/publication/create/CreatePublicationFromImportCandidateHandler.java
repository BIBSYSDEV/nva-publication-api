package no.unit.nva.publication.create;

import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.util.Objects.isNull;
import static no.unit.nva.publication.PublicationServiceConfig.DEFAULT_S3_CLIENT;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.URI;
import no.unit.nva.api.PublicationResponse;
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
    public static final String API_HOST = new Environment().readEnv("API_HOST");
    public static final String SCOPUS_IDENTIFIER = "Scopus";
    public static final String ROLLBACK_WENT_WRONG_MESSAGE = "Rollback went wrong";
    public static final String IMPORT_PROCESS_WENT_WRONG = "Import process went wrong";
    public static final String RESOURCE_HAS_ALREADY_BEEN_IMPORTED_ERROR_MESSAGE = "Resource has already been imported";
    public static final String RESOURCE_IS_MISSING_SCOPUS_IDENTIFIER_ERROR_MESSAGE =
        "Resource is missing scopus identifier";
    public static final String PUBLICATION = "publication";
    private final String persistedStorageBucket;
    private final String importCandidateStorageBucket;
    private final ResourceService candidateService;
    private final ResourceService publicationService;
    private final S3Client ss3Client;

    @JacocoGenerated
    public CreatePublicationFromImportCandidateHandler() {
        this(ResourceService.defaultService(IMPORT_CANDIDATES_TABLE),
             ResourceService.defaultService(PUBLICATIONS_TABLE),
             DEFAULT_S3_CLIENT);
    }

    public CreatePublicationFromImportCandidateHandler(ResourceService importCandidateService,
                                                       ResourceService publicationService,
                                                       S3Client s3Client) {
        super(ImportCandidate.class);
        this.candidateService = importCandidateService;
        this.publicationService = publicationService;
        this.ss3Client = s3Client;
        this.persistedStorageBucket = environment.readEnv("NVA_PERSISTED_STORAGE_BUCKET_NAME");
        this.importCandidateStorageBucket = environment.readEnv("IMPORT_CANDIDATES_STORAGE_BUCKET");
    }

    @Override
    protected PublicationResponse processInput(ImportCandidate input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {

        validateAccessRight(requestInfo);
        validateImportCandidate(input);

        try {
            var rawImportCandidate = candidateService.getImportCandidateByIdentifier(input.getIdentifier());
            var inputWithOwner = injectOrganizationAndOwner(requestInfo, input);
            var result = publicationService.autoImportPublication(inputWithOwner);
            copyArtifacts(result, rawImportCandidate);
            var nvaPublicationUri = toPublicationUriIdentifier(result);
            candidateService.updateImportStatus(inputWithOwner.getIdentifier(), toImportStatus(requestInfo,
                                                                                               nvaPublicationUri));
            var endResult = publicationService.getPublicationByIdentifier(
                result.getIdentifier());
            return PublicationResponse.fromPublication(endResult);
        } catch (Exception e) {
            throw rollbackAndThrowException(input);
        }
    }

    @Override
    protected Integer getSuccessStatusCode(ImportCandidate input, PublicationResponse output) {
        return HTTP_CREATED;
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

    private static boolean notAuthorizedToProcessImportCandidates(RequestInfo requestInfo) {
        return !requestInfo.userIsAuthorized(AccessRight.PROCESS_IMPORT_CANDIDATE.name());
    }

    private void copyArtifacts(Publication publication, ImportCandidate importCandidate) {
        importCandidate.getAssociatedArtifacts().stream()
            .filter(File.class::isInstance)
            .filter(file -> wasKeptByImporter((File) file, publication))
            .map(File.class::cast)
            .forEach(
                a -> copyS3file(importCandidateStorageBucket, persistedStorageBucket, a.getIdentifier().toString()));
    }

    private boolean wasKeptByImporter(File file, Publication publication) {
        return publication.getAssociatedArtifacts().stream()
                   .filter(File.class::isInstance)
                   .anyMatch(publicationFile -> isSameFile((File) publicationFile, file));
    }

    private boolean isSameFile(File a, File b) {
        return a.getIdentifier().equals(b.getIdentifier());
    }

    private void copyS3file(String source, String destination, String key) {
        ss3Client.copyObject(CopyObjectRequest.builder()
                                 .sourceBucket(source)
                                 .sourceKey(key)
                                 .destinationBucket(destination)
                                 .destinationKey(key)
                                 .build());
    }

    private ImportCandidate injectOrganizationAndOwner(RequestInfo requestInfo, ImportCandidate importCandidate)
        throws UnauthorizedException {
        var organization = new Builder().withId(requestInfo.getCurrentCustomer()).build();
        return importCandidate.copyImportCandidate()
                   .withPublisher(organization)
                   .withResourceOwner(new ResourceOwner(new Username(requestInfo.getUserName()), organization.getId()))
                   .build();
    }

    private void validateImportCandidate(ImportCandidate importCandidate) throws BadRequestException,
                                                                                 NotFoundException {
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
