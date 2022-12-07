package no.sikt.nva.brage.migration;

import com.amazonaws.services.lambda.runtime.events.S3Event;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import no.unit.nva.model.Publication;
import no.unit.nva.model.associatedartifacts.AssociatedArtifact;
import no.unit.nva.model.associatedartifacts.file.File;
import nva.commons.core.Environment;
import nva.commons.core.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;

public class AssociatedArtifactMover {

    public static final String COULD_NOT_COPY_ASSOCIATED_ARTEFACT_EXCEPTION_MESSAGE = "Could not copy associated "
                                                                                      + "artefact";
    private final S3Client s3Client;
    private final S3Event s3Event;
    private final String persistedStorageBucket;
    private Logger logger = LoggerFactory.getLogger(AssociatedArtifactMover.class);

    public AssociatedArtifactMover(S3Client s3Client, S3Event s3Event) {
        this.s3Client = s3Client;
        this.s3Event = s3Event;
        this.persistedStorageBucket = new Environment().readEnv("NVA_PERSISTED_STORAGE_BUCKET_NAME");
    }

    public Publication pushAssociatedArtifactsToPersistedStorage(Publication publication) {
        publication.getAssociatedArtifacts()
            .forEach(this::pushAssociatedArtifactToPersistedStorage);
        return publication;
    }

    private void pushAssociatedArtifactToPersistedStorage(AssociatedArtifact associatedArtifact) {

        try {
            var file = (File) associatedArtifact;
            var objectKey = file.getIdentifier().toString();
            var objectKeyPath = getObjectKeyPath();
            var sourceBucket = getSourceBucket();

            var copyObjRequest = CopyObjectRequest.builder()
                                     .sourceBucket(sourceBucket)
                                     .destinationBucket(persistedStorageBucket)
                                     .sourceKey(objectKeyPath + objectKey)
                                     .destinationKey(objectKey)
                                     .build();
            logger.info(copyObjRequest.aclAsString());
            s3Client.copyObject(copyObjRequest);
        } catch (Exception e) {
            throw new AssociatedArtifactException(COULD_NOT_COPY_ASSOCIATED_ARTEFACT_EXCEPTION_MESSAGE, e);
        }
    }

    private String getSourceBucket() {
        return s3Event.getRecords().get(0).getS3().getBucket().getName();
    }

    private String getObjectKeyPath() {
        var recordObjectKey = getRecordObjectKey();
        Path path = Paths.get(recordObjectKey);
        var directory = path.getParent();
        if (Objects.nonNull(directory)) {
            return directory + "/";
        } else {
            return StringUtils.EMPTY_STRING;
        }
    }

    private String getRecordObjectKey() {
        return s3Event.getRecords().get(0).getS3().getObject().getKey();
    }
}
