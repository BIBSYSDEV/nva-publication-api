package no.unit.nva.publication.create;

import java.util.UUID;
import no.unit.nva.importcandidate.ImportCandidate;
import no.unit.nva.model.Publication;
import no.unit.nva.model.associatedartifacts.file.File;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;

public class AssociatedArtifactsMover {

    private final S3Client s3Client;
    private final String importBucket;
    private final String resourceBucket;

    public AssociatedArtifactsMover(S3Client s3Client, String importBucket, String resourceBucket) {
        this.s3Client = s3Client;
        this.importBucket = importBucket;
        this.resourceBucket = resourceBucket;
    }

    public void moveAssociatedArtifacts(Publication publication, ImportCandidate importCandidate) {
        importCandidate.getAssociatedArtifacts()
            .stream()
            .filter(File.class::isInstance)
            .map(File.class::cast)
            .filter(file -> wasKeptByImporter(file, publication))
            .forEach(a -> copyS3file(a.getIdentifier()));
    }

    private boolean wasKeptByImporter(File file, Publication publication) {
        return publication.getAssociatedArtifacts()
                   .stream()
                   .filter(File.class::isInstance)
                   .map(File.class::cast)
                   .anyMatch(publicationFile -> publicationFile.getIdentifier().equals(file.getIdentifier()));
    }

    private void copyS3file(UUID identifier) {
        s3Client.copyObject(CopyObjectRequest.builder()
                                .sourceBucket(importBucket)
                                .sourceKey(identifier.toString())
                                .destinationBucket(resourceBucket)
                                .destinationKey(identifier.toString())
                                .build());
    }
}
