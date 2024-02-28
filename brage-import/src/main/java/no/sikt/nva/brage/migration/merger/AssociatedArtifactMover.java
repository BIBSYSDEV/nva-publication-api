package no.sikt.nva.brage.migration.merger;

import com.amazonaws.services.lambda.runtime.events.S3Event;
import java.util.stream.Collectors;
import no.unit.nva.model.Publication;
import no.unit.nva.model.associatedartifacts.AssociatedArtifact;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.associatedartifacts.file.PublishedFile;
import nva.commons.core.Environment;
import nva.commons.core.StringUtils;
import nva.commons.core.paths.UnixPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;

public class AssociatedArtifactMover {

    public static final String COULD_NOT_COPY_ASSOCIATED_ARTEFACT_EXCEPTION_MESSAGE = "Could not copy associated "
                                                                                      + "artifact: ";
    private final S3Client s3Client;
    private final S3Event s3Event;
    private final String persistedStorageBucket;
    private final Logger logger = LoggerFactory.getLogger(AssociatedArtifactMover.class);

    public AssociatedArtifactMover(S3Client s3Client, S3Event s3Event) {
        this.s3Client = s3Client;
        this.s3Event = s3Event;
        this.persistedStorageBucket = new Environment().readEnv("NVA_PERSISTED_STORAGE_BUCKET_NAME");
    }

    public void pushAssociatedArtifactsToPersistedStorage(Publication publication) {
        publication.setAssociatedArtifacts(pushAssociatedArtefactsToPersistedStorageAndGetMetadata(publication));
    }

    private AssociatedArtifactList pushAssociatedArtefactsToPersistedStorageAndGetMetadata(Publication publication) {
        var associatedArtifacts = publication.getAssociatedArtifacts()
                                      .stream()
                                      .map(this::pushAssociatedArtifactToPersistedStorage)
                                      .collect(Collectors.toList());
        return new AssociatedArtifactList(associatedArtifacts);
    }

    private AssociatedArtifact pushAssociatedArtifactToPersistedStorage(AssociatedArtifact associatedArtifact) {
        try {
            if (associatedArtifact instanceof File) {
                var file = (PublishedFile) associatedArtifact;
                var objectKey = file.getIdentifier().toString();

                S3MultipartCopier.fromSourceKey(getObjectKeyPath() + objectKey)
                    .sourceBucket(getSourceBucket())
                    .destinationKey(objectKey)
                    .destinationBucket(persistedStorageBucket)
                    .copy(s3Client);

                return extractMimeTypeAndSize(file, objectKey);
            } else {
                return associatedArtifact;
            }
        } catch (Exception e) {
            throw new AssociatedArtifactException(
                COULD_NOT_COPY_ASSOCIATED_ARTEFACT_EXCEPTION_MESSAGE + associatedArtifact, e);
        }
    }

    private File extractMimeTypeAndSize(File file, String objectKey) {
        var headObjectResponse = s3Client.headObject(createHeadObjectRequest(objectKey));
        var size = headObjectResponse.contentLength();
        var mimeType = headObjectResponse.contentType();

        return File.builder()
                   .withName(file.getName())
                   .withIdentifier(file.getIdentifier())
                   .withLicense(file.getLicense())
                   .withPublisherAuthority(file.isPublisherAuthority())
                   .withEmbargoDate(file.getEmbargoDate().orElse(null))
                   .withMimeType(mimeType)
                   .withSize(size)
                   .withLegalNote(file.getLegalNote())
                   .buildPublishedFile();
    }

    private HeadObjectRequest createHeadObjectRequest(String objectKey) {
        return HeadObjectRequest.builder().bucket(persistedStorageBucket).key(objectKey).build();
    }

    private String getSourceBucket() {
        return s3Event.getRecords().getFirst().getS3().getBucket().getName();
    }

    private String getObjectKeyPath() {
        var directory = UnixPath.of(getRecordObjectKey()).getParent();
        return directory.map(unixPath -> unixPath + "/").orElse(StringUtils.EMPTY_STRING);
    }

    private String getRecordObjectKey() {
        return s3Event.getRecords().getFirst().getS3().getObject().getKey();
    }
}
