package no.sikt.nva.brage.migration.merger;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.Course;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Publication;
import no.unit.nva.model.Reference;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import no.unit.nva.model.contexttypes.Degree;
import no.unit.nva.model.contexttypes.Degree.Builder;
import no.unit.nva.model.contexttypes.PublicationContext;
import no.unit.nva.model.exceptions.InvalidIsbnException;
import no.unit.nva.model.exceptions.InvalidUnconfirmedSeriesException;
import nva.commons.core.StringUtils;

public class CristinImportPublicationMerger {

    public static final String DUMMY_HANDLE_THAT_EXIST_FOR_PROCESSING_UNIS
        = "dummy_handle_unis";

    private final Publication cristinPublication;
    private final Publication bragePublication;

    public CristinImportPublicationMerger(Publication cristinPublication, Publication bragePublication) {
        this.cristinPublication = cristinPublication;
        this.bragePublication = bragePublication;
    }

    public Publication mergePublications() throws InvalidIsbnException, InvalidUnconfirmedSeriesException {
        var publicationForUpdating = cristinPublication.copy()
                                         .withHandle(determineHandle())
                                         .withAdditionalIdentifiers(mergeAdditionalIdentifiers())
                                         .withSubjects(determineSubject())
                                         .withRightsHolder(determineRightsHolder())
                                         .withEntityDescription(determineEntityDescription())
                                         .build();
        return fillNewPublicationWithMetadataFromBrage(publicationForUpdating);
    }

    private EntityDescription determineEntityDescription()
        throws InvalidIsbnException, InvalidUnconfirmedSeriesException {
        return cristinPublication.getEntityDescription().copy()
                   .withReference(determineReference())
                   .build();
    }

    private Reference determineReference() throws InvalidIsbnException, InvalidUnconfirmedSeriesException {
        var reference = cristinPublication.getEntityDescription().getReference();
        reference.setPublicationContext(determinePublicationContext(reference));
        reference.setDoi(determineDoi(reference));
        return reference;
    }

    private URI determineDoi(Reference reference) {
        return nonNull(reference.getDoi())
                   ? reference.getDoi()
                   : bragePublication.getEntityDescription()
                         .getReference()
                         .getDoi();
    }

    private PublicationContext determinePublicationContext(Reference reference) throws InvalidIsbnException, InvalidUnconfirmedSeriesException {
        var publicationContext = reference.getPublicationContext();
        if (publicationContext instanceof Degree degree) {
            return new Builder().withIsbnList(degree.getIsbnList())
                               .withSeries(degree.getSeries())
                               .withPublisher(degree.getPublisher())
                               .withSeriesNumber(degree.getSeriesNumber())
                               .withCourse(determineCourse(degree))
                               .build();
        } else {
            return publicationContext;
        }
    }

    private Course determineCourse(Degree degree) {
        return nonNull(degree.getCourse()) ? degree.getCourse() : extractBrageCourse();
    }

    private Course extractBrageCourse() {
        return Optional.ofNullable(bragePublication.getEntityDescription().getReference().getPublicationContext())
                   .filter(Degree.class::isInstance)
                   .map(Degree.class::cast)
                   .map(Degree::getCourse)
                   .orElse(null);
    }

    private String determineRightsHolder() {
        return nonNull(cristinPublication.getRightsHolder())
                   ? cristinPublication.getRightsHolder()
                   : bragePublication.getRightsHolder();
    }

    private List<URI> determineSubject() {
        return cristinPublication.getSubjects().isEmpty()
                   ? bragePublication.getSubjects()
                   : cristinPublication.getSubjects();
    }

    private Set<AdditionalIdentifier> mergeAdditionalIdentifiers() {
        var additionalIdentifiers = new HashSet<>(cristinPublication.getAdditionalIdentifiers());
        additionalIdentifiers.addAll(bragePublication.getAdditionalIdentifiers());
        if (shouldAddBrageHandleToAdditionalIdentifiers()) {
            additionalIdentifiers.add(extractBrageHandleAsAdditionalIdentifier());
        }
        return additionalIdentifiers;
    }

    private boolean shouldAddBrageHandleToAdditionalIdentifiers() {
        return nonNull(cristinPublication.getHandle())
               && nonNull(bragePublication.getHandle())
               && !isDummyHandle();
    }

    private AdditionalIdentifier extractBrageHandleAsAdditionalIdentifier() {
        return new AdditionalIdentifier("handle", bragePublication.getHandle().toString());
    }

    private URI determineHandle() {
        return nonNull(cristinPublication.getHandle())
                   ? cristinPublication.getHandle()
                   : getBrageHandleAsLongAsItIsNotADummyHandle().orElse(null);
    }

    private boolean isDummyHandle() {
        return nonNull(bragePublication.getHandle())
               && bragePublication
                      .getHandle()
                      .toString()
                      .contains(DUMMY_HANDLE_THAT_EXIST_FOR_PROCESSING_UNIS);
    }

    private Optional<URI> getBrageHandleAsLongAsItIsNotADummyHandle() {
        return isDummyHandle()
                   ? Optional.empty()
                   : Optional.of(bragePublication.getHandle());
    }

    private Publication fillNewPublicationWithMetadataFromBrage(Publication publication) {
        var associatedArtifacts = determineAssociatedArtifacts();
        publication.getEntityDescription().setDescription(getCorrectDescription());
        publication.getEntityDescription().setAbstract(getCorrectAbstract());

        if (hasBeenUpdated(associatedArtifacts, publication)) {
            updatePublicationResourceOwnerAndPublisher(publication);
        }

        publication.setAssociatedArtifacts(associatedArtifacts);
        return publication;
    }

    private void updatePublicationResourceOwnerAndPublisher(Publication publication) {
        publication.setResourceOwner(bragePublication.getResourceOwner());
//        publication.setPublisher(bragePublication.getPublisher());
    }

    private boolean hasBeenUpdated(AssociatedArtifactList associatedArtifacts, Publication publication) {
        return !associatedArtifacts.equals(publication.getAssociatedArtifacts());
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
