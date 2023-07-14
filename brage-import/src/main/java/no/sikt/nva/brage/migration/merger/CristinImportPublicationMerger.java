package no.sikt.nva.brage.migration.merger;

import no.unit.nva.model.Publication;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import nva.commons.core.StringUtils;

public class CristinImportPublicationMerger {

    private final Publication cristinPublication;
    private final Publication bragePublication;

    public CristinImportPublicationMerger(Publication cristinPublication, Publication bragePublication) {
        this.cristinPublication = cristinPublication;
        this.bragePublication = bragePublication;
    }

    public Publication mergePublications() {
        var publicationForUpdating = cristinPublication.copy()
                                         .withHandle(bragePublication.getHandle())
                                         .build();
        return fillNewPublicationWithMetadataFromBrage(publicationForUpdating);
    }

    private Publication fillNewPublicationWithMetadataFromBrage(Publication publicationForUpdating) {
        publicationForUpdating.getEntityDescription().setDescription(getCorrectDescription());
        publicationForUpdating.getEntityDescription().setAbstract(getCorrectAbstract());
        publicationForUpdating.setAssociatedArtifacts(mergeAssociatedArtifacts());
        return publicationForUpdating;
    }

    private AssociatedArtifactList mergeAssociatedArtifacts() {
        return cristinPublication.getAssociatedArtifacts().isEmpty()
                   ? mergeFiles()
                   : cristinPublication.getAssociatedArtifacts();
    }

    private AssociatedArtifactList mergeFiles() {
        var associatedArtifactsToExistingPublication = cristinPublication.getAssociatedArtifacts();
        var associatedArtifacts = bragePublication.getAssociatedArtifacts();
        return associatedArtifacts.size() > associatedArtifactsToExistingPublication.size()
                   ? new AssociatedArtifactList(associatedArtifacts)
                   : associatedArtifactsToExistingPublication;
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
}
