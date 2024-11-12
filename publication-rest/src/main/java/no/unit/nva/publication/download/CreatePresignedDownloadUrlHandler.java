package no.unit.nva.publication.download;

import static java.net.HttpURLConnection.HTTP_OK;
import static nva.commons.core.attempt.Try.attempt;
import java.net.http.HttpClient;
import no.unit.nva.clients.IdentityServiceClient;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.publication.download.exception.S3ServiceException;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permission.strategy.PublicationPermissionStrategy;
import nva.commons.apigateway.exceptions.NotFoundException;
import com.amazonaws.services.lambda.runtime.Context;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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

    private static final int DEFAULT_EXPIRATION_SECONDS = 180;
    private static final String REQUESTED_RESOURCE_NOT_FOUND = "Requested resource \"%s/files/%s\" was found";
    private static final String API_HOST_ENV = "API_HOST";
    private final S3Presigner s3Presigner;
    private final UriShortener uriShortener;
    private final ResourceService resourceService;
    private static final String BUCKET_NAME_ENV = "NVA_PERSISTED_STORAGE_BUCKET_NAME";
    private final IdentityServiceClient identityServiceClient;
    private static final String CUSTOM_DOMAIN_BASE_PATH_ENV = "CUSTOM_DOMAIN_BASE_PATH";
    private final String basePath;

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
        this.basePath = environment.readEnv(CUSTOM_DOMAIN_BASE_PATH_ENV);
    }

    /**
     * Default constructor for CreatePresignedDownloadUrlHandler.
     */
    @JacocoGenerated
    public CreatePresignedDownloadUrlHandler() {
        this(ResourceService.defaultService(),
             defaultS3Presigner(),
             new Environment(),
             UriShortenerImpl.createDefault(new Environment().readEnv(API_HOST_ENV)),
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
        var publicationId = RequestUtil.getIdentifier(requestInfo);
        var fileIdentifier = RequestUtil.getFileIdentifier(requestInfo);

        var publication = resourceService.getPublicationByIdentifier(publicationId);
        var file =  publication.getFile(fileIdentifier);

        if (file.isEmpty() || !hasFileAccess(publication, file.get(), requestInfo)) {
            throw new NotFoundException(
                String.format(REQUESTED_RESOURCE_NOT_FOUND, publication.getIdentifier(), fileIdentifier));
        }

        return getPresignedUriResponse(file.get());
    }

    private PresignedUriResponse getPresignedUriResponse(File file) throws S3ServiceException {
        var expiration = defaultExpiration();
        var preSignedUriLong = getPresignedDownloadUrl(file, expiration);
        var shortenedPresignUri = uriShortener.shorten(preSignedUriLong.id(), basePath, expiration);
        return new PresignedUriResponse(file.getIdentifier().toString(), preSignedUriLong.id(), expiration, shortenedPresignUri);
    }

    private boolean hasFileAccess(Publication publication, File file, RequestInfo requestInfo) {
        return userHasUpdatePermission(publication, requestInfo) || fileIsVisibleForNonOwner(publication, file);
    }

    private boolean fileIsVisibleForNonOwner(Publication publication, File file) {
        return isPublicationPublished(publication) && file.isVisibleForNonOwner();
    }

    private boolean isPublicationPublished(Publication publication) {
        return PublicationStatus.PUBLISHED.equals(publication.getStatus());
    }

    private boolean userHasUpdatePermission(Publication publication, RequestInfo requestInfo) {
        return attempt(() -> RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient)).toOptional()
                   .map(user -> hasUpdatePermission(publication, user))
                   .orElse(false);
    }

    private boolean hasUpdatePermission(Publication publication, UserInstance user) {
        return PublicationPermissionStrategy.create(publication, user, resourceService)
                   .allowsAction(PublicationOperation.UPDATE);
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