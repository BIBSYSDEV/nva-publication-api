package no.sikt.nva.brage.migration.merger;

import java.util.List;
import no.unit.nva.model.associatedartifacts.AssociatedArtifact;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import no.unit.nva.model.associatedartifacts.file.File;
import nva.commons.core.Environment;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;

public class AssociatedArtifactComparator {

    private final S3Client s3Client;
    private final String persistedStorageBucket;
    private final List<AssociatedArtifact> associatedArtifactsList;

    public AssociatedArtifactComparator(S3Client s3Client, AssociatedArtifactList associatedArtifactList) {
        this.s3Client = s3Client;
        this.associatedArtifactsList = associatedArtifactList;
        this.persistedStorageBucket = new Environment().readEnv("NVA_PERSISTED_STORAGE_BUCKET_NAME");
    }

    public boolean containsAssociatedArtifact(AssociatedArtifact artifact) {
        var contentLength = getFileContentLength(artifact);
        for (AssociatedArtifact art : associatedArtifactsList) {
            var contentLengthOfExistingArtifact = getFileContentLength(art);
            if (contentLength.equals(contentLengthOfExistingArtifact)) {
                return true;
            }
        }
        return false;
    }

    private String getFileContentLength(AssociatedArtifact artifact) {
        var file = (File) artifact;
        var objectKey = file.getIdentifier().toString();
        HeadObjectRequest request1 = HeadObjectRequest.builder()
                                         .bucket(persistedStorageBucket)
                                         .key(objectKey)
                                         .build();
        var response = s3Client.headObject(request1);
        return response.contentLength().toString();
    }
}
