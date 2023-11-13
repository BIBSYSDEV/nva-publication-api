package no.sikt.nva.brage.migration.merger;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import no.unit.nva.model.AdditionalIdentifier;
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
                                         .withHandle(determineHandle())
                                         .withAdditionalIdentifiers(mergeAdditionalIdentifiers())
                                         .build();
        return fillNewPublicationWithMetadataFromBrage(publicationForUpdating);
    }

    private Set<AdditionalIdentifier> mergeAdditionalIdentifiers() {
        var additionalIdentifiers = new HashSet<>(cristinPublication.getAdditionalIdentifiers());
        additionalIdentifiers.addAll(bragePublication.getAdditionalIdentifiers());
        additionalIdentifiers.add(extractBrageHandleAsAdditionalIdentifier());
        return additionalIdentifiers;
    }

    private AdditionalIdentifier extractBrageHandleAsAdditionalIdentifier() {
        return new AdditionalIdentifier("handle", bragePublication.getHandle().toString());
    }

    private URI determineHandle() {
        return nonNull(cristinPublication.getHandle())
                   ? cristinPublication.getHandle()
                   : bragePublication.getHandle();
    }

    private Publication fillNewPublicationWithMetadataFromBrage(Publication publicationForUpdating) {
        publicationForUpdating.getEntityDescription().setDescription(getCorrectDescription());
        publicationForUpdating.getEntityDescription().setAbstract(getCorrectAbstract());
        publicationForUpdating.setAssociatedArtifacts(determineAssociatedArtifacts());
        return publicationForUpdating;
    }

    private AssociatedArtifactList determineAssociatedArtifacts() {
        return shouldUseBrageArtifacts()
                   ? bragePublication.getAssociatedArtifacts()
                   : cristinPublication.getAssociatedArtifacts();
    }

    private boolean shouldUseBrageArtifacts() {
        return bragePublicationHasAssociatedArtifacts()
               && (cristinPublication.getAssociatedArtifacts().isEmpty()
                   || hasTheSameHandle()
                   || cristinHandleIsNotSet());
    }

    private boolean bragePublicationHasAssociatedArtifacts() {
        return !bragePublication.getAssociatedArtifacts().isEmpty();
    }

    private boolean cristinHandleIsNotSet() {
        return isNull(cristinPublication.getHandle());
    }

    private boolean hasTheSameHandle() {
        return bragePublication.getHandle().equals(cristinPublication.getHandle());
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
