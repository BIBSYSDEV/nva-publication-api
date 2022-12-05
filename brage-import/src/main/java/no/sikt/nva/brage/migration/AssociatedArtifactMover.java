package no.sikt.nva.brage.migration;

import com.amazonaws.services.lambda.runtime.events.S3Event;
import java.nio.file.Path;
import java.nio.file.Paths;
import no.unit.nva.model.Publication;
import no.unit.nva.model.associatedartifacts.AssociatedArtifact;
import no.unit.nva.model.associatedartifacts.file.File;
import nva.commons.core.Environment;
import nva.commons.core.StringUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;

public class AssociatedArtifactMover {

    private final S3Client s3Client;
    private final S3Event s3Event;
    private final String persistedStorageBucket;

    public AssociatedArtifactMover(S3Client s3Client, S3Event s3Event) {
        this.s3Client = s3Client;
        this.s3Event = s3Event;
        this.persistedStorageBucket = new Environment().readEnv("NVA_PERSISTED_STORAGE_BUCKET_NAME");
    }

    public Publication pushAssociatedArtifactsToPersistedStorage(Publication publication) {
        publication.getAssociatedArtifacts()
            .forEach(associatedArtifact -> pushAssociatedArtifactToPersistedStorage(associatedArtifact));
        return publication;
    }

    private void pushAssociatedArtifactToPersistedStorage(AssociatedArtifact associatedArtifact) {
        if (associatedArtifact instanceof File) {

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
                s3Client.copyObject(copyObjRequest);
            } catch (Exception e) {
                throw new AssociatedArtifactException("Could not copy associated artefact");
            }
        } else {
            throw new AssociatedArtifactException("Associated Artefact not a file");
        }
    }

    private String getSourceBucket() {
        return s3Event.getRecords().get(0).getS3().getBucket().getName();
    }

    private String getObjectKeyPath() {
        var recordObjectKey = s3Event.getRecords().get(0).getS3().getObject().getKey();
        Path path = Paths.get(recordObjectKey);
        var directory = path.getParent().toString();
        if (StringUtils.isNotEmpty(directory)) {
            return directory + "/";
        } else {
            return StringUtils.EMPTY_STRING;
        }
    }
}
