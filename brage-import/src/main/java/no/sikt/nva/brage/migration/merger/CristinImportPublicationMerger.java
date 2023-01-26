package no.sikt.nva.brage.migration.merger;

import static java.util.Objects.nonNull;
import java.util.ArrayList;
import no.unit.nva.model.Publication;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import nva.commons.core.StringUtils;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.services.s3.S3Client;

public class CristinImportPublicationMerger {

    public static final String HANDLE_MISMATCH_INFORMATION = "Handle mismatch. cristin publication handle: %s, "
                                                             + "brage-publication handle: %s";

    private final Publication cristinPublication;
    private final Publication bragePublication;

    private final S3Client s3Client;

    public CristinImportPublicationMerger(Publication cristinPublication, Publication bragePublication,
                                          S3Client s3Client) {
        this.cristinPublication = cristinPublication;
        this.bragePublication = bragePublication;
        this.s3Client = s3Client;
    }

    public Publication mergePublications() {
        if (cristinImportedPublicationHasHandleThatIsDifferentToBragePublication()) {
            throw new MergePublicationException(String.format(HANDLE_MISMATCH_INFORMATION,
                                                              cristinPublication.getHandle(),
                                                              bragePublication.getHandle()));
        }
        var publicationForUpdating = cristinPublication.copy()
                                         .withHandle(bragePublication.getHandle())
                                         .build();
        return fillNewPublicationWithMetadataFromBrage(publicationForUpdating);
    }

    @NotNull
    private Publication fillNewPublicationWithMetadataFromBrage(Publication publicationForUpdating) {
        publicationForUpdating.getEntityDescription().setDescription(getCorrectDescription());
        publicationForUpdating.getEntityDescription().setAbstract(getCorrectAbstract());
        publicationForUpdating.setAssociatedArtifacts(mergeAssociatedArtifacts());
        return publicationForUpdating;
    }

    private AssociatedArtifactList mergeAssociatedArtifacts() {
        var associatedArtifacts1 = bragePublication.getAssociatedArtifacts();
        var associatedArtifactsToExistingPublication1 = cristinPublication.getAssociatedArtifacts();
        var comparator = new AssociatedArtifactComparator(s3Client, associatedArtifactsToExistingPublication1);
        var list = new ArrayList<>(associatedArtifactsToExistingPublication1);
        associatedArtifacts1.stream()
            .filter(artifact -> !comparator.containsAssociatedArtifact(artifact))
            .forEach(list::add);
        return new AssociatedArtifactList(list);
    }

    private String getCorrectDescription() {
        return StringUtils.isNotEmpty(cristinPublication.getEntityDescription().getDescription())
                   ? cristinPublication.getEntityDescription().getDescription()
                   : bragePublication.getEntityDescription().getDescription();
    }

    private String getCorrectAbstract() {
        return StringUtils.isNotEmpty(cristinPublication.getEntityDescription().getAbstract())
                   ? cristinPublication.getEntityDescription().getAbstract()
                   : bragePublication.getEntityDescription().getAbstract();
    }

    private boolean cristinImportedPublicationHasHandleThatIsDifferentToBragePublication() {
        return nonNull(cristinPublication.getHandle())
               && !cristinPublication.getHandle().equals(bragePublication.getHandle());
    }
}
