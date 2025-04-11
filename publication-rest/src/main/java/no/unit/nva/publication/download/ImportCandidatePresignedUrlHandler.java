package no.unit.nva.publication.download;

import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import java.util.UUID;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.publication.RequestUtil;
import no.unit.nva.publication.model.business.importcandidate.ImportCandidate;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

public class ImportCandidatePresignedUrlHandler extends ApiGatewayHandler<Void, PresignedUri> {

    public static final String BUCKET = new Environment().readEnv("IMPORT_CANDIDATES_STORAGE_BUCKET");
    public static final String IMPORT_CANDIDATE_MISSES_FILE_EXCEPTION_MESSAGE = "Import candidate with identifier: %s"
                                                                                + " \nDoes not have file with "
                                                                                + "identifier: %s";
    private final S3Presigner s3Presigner;
    private final ResourceService importCandidateService;

    @JacocoGenerated
    public ImportCandidatePresignedUrlHandler() {
        this(defaultS3Presigner(), ResourceService.defaultService(), new Environment());
    }

    public ImportCandidatePresignedUrlHandler(S3Presigner s3Presigner, ResourceService importCandidateService,
                                              Environment environment) {
        super(Void.class, environment);
        this.s3Presigner = s3Presigner;
        this.importCandidateService = importCandidateService;
    }

    @JacocoGenerated
    public static S3Presigner defaultS3Presigner() {
        return S3Presigner.builder()
                   .region(new Environment().readEnvOpt("AWS_REGION").map(Region::of).orElse(Region.EU_WEST_1))
                   .credentialsProvider(DefaultCredentialsProvider.create())
                   .build();
    }

    @Override
    protected void validateRequest(Void unused, RequestInfo requestInfo, Context context) throws ApiGatewayException {
        //Do nothing
    }

    @Override
    protected PresignedUri processInput(Void unused, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        var importCandidateIdentifier = RequestUtil.getImportCandidateIdentifier(requestInfo);
        var fileIdentifier = RequestUtil.getFileIdentifier(requestInfo);

        validateExistence(importCandidateIdentifier, fileIdentifier);

        return PresignedUri.fromS3Key(fileIdentifier).bucket(BUCKET).create(s3Presigner);
    }

    @Override
    protected Integer getSuccessStatusCode(Void unused, PresignedUri o) {
        return HttpURLConnection.HTTP_OK;
    }

    private static boolean fileDoesNotExists(ImportCandidate importCandidate, UUID fileIdentifier) {
        return importCandidate.getAssociatedArtifacts()
                   .stream()
                   .filter(File.class::isInstance)
                   .map(File.class::cast)
                   .map(File::getIdentifier)
                   .noneMatch(identifier -> identifier.equals(fileIdentifier));
    }

    private void validateExistence(SortableIdentifier importCandidateIdentifier, UUID fileIdentifier)
        throws NotFoundException {
        var importCandidate = importCandidateService.getImportCandidateByIdentifier(importCandidateIdentifier);
        if (fileDoesNotExists(importCandidate, fileIdentifier)) {
            throw new NotFoundException(String.format(IMPORT_CANDIDATE_MISSES_FILE_EXCEPTION_MESSAGE,
                                                      importCandidate.getIdentifier().toString(),
                                                      fileIdentifier.toString()));
        }
    }
}
