package no.unit.nva.publication.download;

import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.Objects.nonNull;
import static no.unit.nva.publication.RequestUtil.getFileIdentifier;
import static nva.commons.apigateway.AccessRight.MANAGE_DEGREE_EMBARGO;
import static nva.commons.apigateway.AccessRight.MANAGE_RESOURCES_STANDARD;
import static nva.commons.core.attempt.Try.attempt;
import java.net.http.HttpClient;
import no.unit.nva.clients.IdentityServiceClient;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.publication.download.exception.S3ServiceException;
import no.unit.nva.publication.exception.NotAuthorizedException;
import no.unit.nva.publication.permission.strategy.PublicationPermissionStrategy;
import nva.commons.apigateway.exceptions.NotFoundException;
import com.amazonaws.services.lambda.runtime.Context;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.publication.RequestUtil;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.services.UriShortener;
import no.unit.nva.publication.services.UriShortenerImpl;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

public class CreatePresignedDownloadUrlHandler extends ApiGatewayHandler<Void, PresignedUriResponse> {

    public static final int DEFAULT_EXPIRATION_SECONDS = 180;
    public static final String REQUESTED_RESOURCE_NOT_FOUND = "Requested resource \"%s/files/%s\" was found";
    private final S3Presigner s3Presigner;
    private final UriShortener uriShortener;
    private final ResourceService resourceService;
    public static final String BUCKET_NAME_ENV = "NVA_PERSISTED_STORAGE_BUCKET_NAME";
    private final IdentityServiceClient identityServiceClient;

    /**
     * Constructor for CreatePresignedDownloadUrlHandler.
     */
    public CreatePresignedDownloadUrlHandler(ResourceService resourceService,
                                             S3Presigner s3Presigner,
                                             Environment environment,
                                             UriShortener uriShortener,
                                             HttpClient httpClient,
                                             IdentityServiceClient identityServiceClient) {
        super(Void.class, environment, httpClient);
        this.resourceService = resourceService;
        this.s3Presigner = s3Presigner;
        this.uriShortener = uriShortener;
        this.identityServiceClient = identityServiceClient;
    }

    /**
     * Default constructor for CreatePresignedDownloadUrlHandler.
     */
    @JacocoGenerated
    public CreatePresignedDownloadUrlHandler() {
        this(ResourceService.defaultService(),
             defaultS3Presigner(),
             new Environment(),
             UriShortenerImpl.createDefault(),
             HttpClient.newHttpClient(),
             IdentityServiceClient.prepare());
    }

    @Override
    protected void validateRequest(Void unused, RequestInfo requestInfo, Context context) throws ApiGatewayException {
        //Do nothing
    }

    @Override
    protected PresignedUriResponse processInput(Void input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        var publication = resourceService.getPublicationByIdentifier(RequestUtil.getIdentifier(requestInfo));
        var file = getFileInformation(publication, requestInfo);

        if (!isFileVisible(publication, file, requestInfo)) {
            if (PublicationStatus.DRAFT.equals(publication.getStatus())) {
                throw new NotFoundException(
                    String.format(REQUESTED_RESOURCE_NOT_FOUND, publication.getIdentifier(), file.getIdentifier()));
            }
            throw new NotAuthorizedException();
        }

        return getPresignedUriResponse(file);
    }

    private PresignedUriResponse getPresignedUriResponse(File file) throws S3ServiceException {
        var expiration = defaultExpiration();
        var preSignedUriLong = getPresignedDownloadUrl(file, expiration);
        var shortenedPresignUri = uriShortener.shorten(preSignedUriLong.signedUri(), expiration);
        return new PresignedUriResponse(preSignedUriLong.signedUri(), expiration, shortenedPresignUri);
    }

    private boolean isFileVisible(Publication publication, File file, RequestInfo requestInfo) {
        var userInstance = attempt(
            () -> RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient)).or(() -> null).get();

        var permissionStrategy = userInstance!= null ? PublicationPermissionStrategy.create(publication, userInstance,
                                                                                            resourceService) : null;

        return file.isVisibleForNonOwner() ||
               (permissionStrategy != null && permissionStrategy.allowsAction(PublicationOperation.UPDATE));
    }

    @JacocoGenerated
    public static S3Presigner defaultS3Presigner() {
        return S3Presigner.builder()
                   .region(new Environment().readEnvOpt("AWS_REGION").map(Region::of).orElse(Region.EU_WEST_1))
                   .credentialsProvider(DefaultCredentialsProvider.create())
                   .build();
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, PresignedUriResponse output) {
        return HTTP_OK;
    }

    private File getFileInformation(Publication publication, RequestInfo requestInfo)
        throws ApiGatewayException {

        var fileIdentifier = getFileIdentifier(requestInfo);
        if (publication.getAssociatedArtifacts().isEmpty()) {
            throw new NotFoundException(
                String.format(REQUESTED_RESOURCE_NOT_FOUND, publication.getIdentifier(), fileIdentifier));
        }

        return publication.getAssociatedArtifacts().stream()
                   .filter(File.class::isInstance)
                   .map(File.class::cast)
                   .filter(element -> findByIdentifier(fileIdentifier, element))
                   .map(element -> getFile(element, publication, requestInfo))
                   .filter(Optional::isPresent)
                   .map(Optional::get)
                   .findFirst()
                   .orElseThrow(() -> new NotFoundException(
                       String.format(REQUESTED_RESOURCE_NOT_FOUND, publication.getIdentifier(), fileIdentifier)));
    }

    private boolean findByIdentifier(UUID fileIdentifier, File element) {
        return fileIdentifier.equals(element.getIdentifier());
    }

    private boolean hasReadAccess(File file, Publication publication, RequestInfo requestInfo) {
        if (isFilePublic(file, publication)) {
            return true;
        }

        var user = attempt(() -> RequestUtil.createUserInstanceFromRequest(requestInfo, null)).or(() -> null).get();

        var isThesisAndEmbargoThesisReader =
            isThesis(publication) && requestInfo.userIsAuthorized(MANAGE_DEGREE_EMBARGO);
        var isOwner = nonNull(user) && publication.getResourceOwner().getOwner().getValue().equals(user.getUsername());
        var hasActiveEmbargo = !file.fileDoesNotHaveActiveEmbargo();

        if (hasActiveEmbargo) {
            return isOwner || isThesisAndEmbargoThesisReader;
        }

        var isEditor = requestInfo.userIsAuthorized(MANAGE_RESOURCES_STANDARD);

        return isOwner || isEditor;
    }

    private static boolean isFilePublic(File file, Publication publication) {
        var isPublished = PublicationStatus.PUBLISHED.equals(publication.getStatus());
        return isPublished && file.isVisibleForNonOwner();
    }

    private Optional<File> getFile(File file, Publication publication, RequestInfo requestInfo) {
        return hasReadAccess(file, publication, requestInfo)
                   ? Optional.of(file)
                   : Optional.empty();
    }

    private boolean isThesis(Publication publication) {
        var kind = attempt(() -> publication
                                     .getEntityDescription()
                                     .getReference()
                                     .getPublicationInstance()).toOptional();
        return kind.isPresent() && ("DegreeBachelor".equals(kind.get().getInstanceType())
                                    ||
                                    "DegreeMaster".equals(kind.get().getInstanceType())
                                    ||
                                    "DegreePhd".equals(kind.get().getInstanceType()));
    }

    private PresignedUri getPresignedDownloadUrl(File file, Instant expiration) throws S3ServiceException {
        return PresignedUri.builder()
                   .withFileIdentifier(file.getIdentifier())
                   .withBucket(environment.readEnv(BUCKET_NAME_ENV))
                   .withMime(file.getMimeType())
                   .withExpiration(expiration)
                   .build()
                   .create(s3Presigner);
    }

    private Instant defaultExpiration() {
        return Instant.now().plus(DEFAULT_EXPIRATION_SECONDS, ChronoUnit.SECONDS);
    }
}