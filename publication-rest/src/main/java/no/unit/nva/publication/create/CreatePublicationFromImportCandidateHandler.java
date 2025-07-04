package no.unit.nva.publication.create;

import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.util.Objects.isNull;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import no.unit.nva.api.PublicationResponse;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.ImportSource;
import no.unit.nva.model.ImportSource.Source;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.Username;
import no.unit.nva.model.additionalidentifiers.AdditionalIdentifierBase;
import no.unit.nva.model.additionalidentifiers.ScopusIdentifier;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.publication.create.pia.PiaClient;
import no.unit.nva.publication.exception.NotAuthorizedException;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
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
import nva.commons.core.attempt.Failure;
import nva.commons.core.paths.UriWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;

public class CreatePublicationFromImportCandidateHandler extends ApiGatewayHandler<ImportCandidate,
                                                                                      PublicationResponse> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreatePublicationFromImportCandidateHandler.class);
    public static final String API_HOST = new Environment().readEnv("API_HOST");
    public static final String SCOPUS_IDENTIFIER = "Scopus";
    public static final String ROLLBACK_WENT_WRONG_MESSAGE = "Rollback went wrong";
    public static final String IMPORT_PROCESS_WENT_WRONG = "Import process went wrong";
    public static final String RESOURCE_HAS_ALREADY_BEEN_IMPORTED_ERROR_MESSAGE = "Resource has already been imported";
    public static final String RESOURCE_IS_MISSING_SCOPUS_IDENTIFIER_ERROR_MESSAGE =
        "Resource is missing scopus identifier";
    public static final String PUBLICATION = "publication";
    public static final String RESOURCE_IS_NOT_PUBLISHABLE = "Resource is not publishable";

    private final String persistedStorageBucket;
    private final String importCandidateStorageBucket;
    private final ResourceService candidateService;
    private final ResourceService publicationService;
    private final S3Client ss3Client;
    private final PiaClient piaClient;

    @JacocoGenerated
    public CreatePublicationFromImportCandidateHandler() {
        this(ImportCandidateHandlerConfigs.getDefaultsConfigs(), new Environment());
    }

    public CreatePublicationFromImportCandidateHandler(
        ImportCandidateHandlerConfigs configs, Environment environment) {
        super(ImportCandidate.class, environment);
        this.candidateService = configs.importCandidateService();
        this.publicationService = configs.publicationService();
        this.ss3Client = configs.s3Client();
        this.persistedStorageBucket = configs.persistedStorageBucket();
        this.importCandidateStorageBucket = configs.importCandidateStorageBucket();
        this.piaClient = new PiaClient(configs.piaClientConfig());
    }

    @Override
    protected void validateRequest(ImportCandidate importCandidate, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        validateAccessRight(requestInfo);
        validateImportCandidate(importCandidate);
    }

    @Override
    protected PublicationResponse processInput(ImportCandidate input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        return attempt(() -> importCandidate(input, requestInfo))
                   .map(PublicationResponse::fromPublication)
                   .orElseThrow(fail -> rollbackAndThrowException(fail, input));
    }

    @Override
    protected Integer getSuccessStatusCode(ImportCandidate input, PublicationResponse output) {
        return HTTP_CREATED;
    }

    private static ImportStatus toImportStatus(RequestInfo requestInfo, URI uri) throws UnauthorizedException {
        return ImportStatusFactory.createImported(new Username(requestInfo.getUserName()), uri);
    }

    private static boolean notAuthorizedToProcessImportCandidates(RequestInfo requestInfo) {
        return !requestInfo.userIsAuthorized(AccessRight.MANAGE_IMPORT);
    }

    private static boolean hasDifferentCristinId(Contributor rawContributor, Contributor contributor) {
        return !Objects.equals(rawContributor.getIdentity().getId(), contributor.getIdentity().getId());
    }

    private URI toPublicationUriIdentifier(Publication publication) {
        return UriWrapper.fromHost(API_HOST)
                   .addChild(PUBLICATION)
                   .addChild(publication.getIdentifier().toString())
                   .getUri();
    }

    private Publication importCandidate(ImportCandidate input, RequestInfo requestInfo)
        throws NotFoundException, UnauthorizedException {
        var nvaPublication = createNvaPublicationFromImportCandidateAndUserInput(input, requestInfo);
        updateImportCandidate(input, requestInfo, nvaPublication);
        return publicationService.getPublicationByIdentifier(nvaPublication.getIdentifier());
    }

    private void updateImportCandidate(ImportCandidate input, RequestInfo requestInfo, Publication nvaPublication)
        throws NotFoundException, UnauthorizedException {
        var nvaPublicationUri = toPublicationUriIdentifier(nvaPublication);
        candidateService.updateImportStatus(input.getIdentifier(), toImportStatus(requestInfo,
                                                                                  nvaPublicationUri));
    }

    private Publication createNvaPublicationFromImportCandidateAndUserInput(ImportCandidate importCandidate,
                                                                            RequestInfo requestInfo)
        throws NotFoundException, UnauthorizedException {
        var rawImportCandidate = candidateService.getImportCandidateByIdentifier(importCandidate.getIdentifier());
        var inputWithOwner = injectOrganizationAndOwner(requestInfo, importCandidate, rawImportCandidate);
        var publication = Resource.fromImportCandidate(inputWithOwner)
                              .importResource(publicationService, ImportSource.fromSource(Source.SCOPUS))
                              .toPublication();
        copyArtifacts(publication, rawImportCandidate);
        updatePiaContributors(importCandidate, rawImportCandidate);
        return publication;
    }

    private void updatePiaContributors(ImportCandidate input, ImportCandidate rawImportCandidate) {
        var rawImportCandidateContributors = rawImportCandidate.getEntityDescription().getContributors();
        var contributorsNeedingUpdate =
            input.getEntityDescription()
                .getContributors()
                .stream()
                .filter(contributor -> changeInCristinIdentifier(contributor, rawImportCandidateContributors))
                .toList();
        piaClient.updateContributor(contributorsNeedingUpdate, getScopusIdentifier(rawImportCandidate));
    }

    private boolean changeInCristinIdentifier(Contributor contributor,
                                              List<Contributor> rawImportCandidateContributors) {
        return rawImportCandidateContributors.stream()
                   .anyMatch(rawContributor -> hasSameAuidButDifferentCristinId(rawContributor,
                                                                                contributor));
    }

    private boolean hasSameAuidButDifferentCristinId(Contributor rawContributor, Contributor contributor) {
        return hasDifferentCristinId(rawContributor, contributor)
               && hasSameAuid(rawContributor, contributor);
    }

    private boolean hasSameAuid(Contributor rawContributor, Contributor contributor) {
        var userAuid = extractAuid(contributor);
        var rawAuid = extractAuid(rawContributor);
        return Objects.equals(userAuid, rawAuid);
    }

    private AdditionalIdentifierBase extractAuid(Contributor contributor) {
        return contributor.getIdentity().getAdditionalIdentifiers().stream().filter(
            additionalIdentifier -> "scopus-auid".equals(additionalIdentifier.sourceName())
        ).findFirst().orElse(null);
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
                   .map(File.class::cast)
                   .anyMatch(publicationFile -> isSameFile(publicationFile, file));
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

    private ImportCandidate injectOrganizationAndOwner(RequestInfo requestInfo,
                                                       ImportCandidate userInput,
                                                       ImportCandidate databaseVersion)
        throws UnauthorizedException {
        var userInstance = UserInstance.fromRequestInfo(requestInfo);
        return databaseVersion.copyImportCandidate()
                   .withEntityDescription(userInput.getEntityDescription())
                   .withAssociatedArtifacts(userInput.getAssociatedArtifacts())
                   .withDoi(userInput.getDoi())
                   .withAdditionalIdentifiers(userInput.getAdditionalIdentifiers())
                   .withProjects(userInput.getProjects())
                   .withSubjects(userInput.getSubjects())
                   .withFundings(userInput.getFundings())
                   .withRightsHolder(userInput.getRightsHolder())
                   .withHandle(userInput.getHandle())
                   .withLink(userInput.getLink())
                   .withPublisher(Organization.fromUri(userInstance.getCustomerId()))
                   .withResourceOwner(new ResourceOwner(new Username(userInstance.getUsername()),
                                                        userInstance.getTopLevelOrgCristinId()))
                   .build();
    }

    private void validateImportCandidate(ImportCandidate importCandidate) throws BadRequestException {
        if (CandidateStatus.IMPORTED.equals(importCandidate.getImportStatus().candidateStatus())) {
            throw new BadRequestException(RESOURCE_HAS_ALREADY_BEEN_IMPORTED_ERROR_MESSAGE);
        }
        if (isNull(getScopusIdentifier(importCandidate))) {
            throw new BadRequestException(RESOURCE_IS_MISSING_SCOPUS_IDENTIFIER_ERROR_MESSAGE);
        }
        if (!importCandidate.isPublishable()) {
            throw new BadRequestException(RESOURCE_IS_NOT_PUBLISHABLE);
        }
    }

    private BadGatewayException rollbackAndThrowException(Failure<PublicationResponse> failure, ImportCandidate input) {
        LOGGER.error("Import failed with exception: {}", failure.getException().getMessage());
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
                   .filter(ScopusIdentifier.class::isInstance)
                   .map(ScopusIdentifier.class::cast)
                   .map(ScopusIdentifier::value)
                   .findFirst()
                   .orElse(null);
    }

    private BadGatewayException rollbackImportStatusUpdate(ImportCandidate importCandidate)
        throws NotFoundException {
        candidateService.updateImportStatus(importCandidate.getIdentifier(), ImportStatusFactory.createNotImported());
        return new BadGatewayException(IMPORT_PROCESS_WENT_WRONG);
    }
}
