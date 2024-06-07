package no.sikt.nva.brage.migration.merger;

import static java.util.Objects.nonNull;
import java.net.URI;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import no.sikt.nva.brage.migration.model.PublicationRepresentation;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.Course;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.Reference;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import no.unit.nva.model.contexttypes.Book;
import no.unit.nva.model.contexttypes.Book.BookBuilder;
import no.unit.nva.model.contexttypes.BookSeries;
import no.unit.nva.model.contexttypes.Degree;
import no.unit.nva.model.contexttypes.Degree.Builder;
import no.unit.nva.model.contexttypes.PublicationContext;
import no.unit.nva.model.contexttypes.Publisher;
import no.unit.nva.model.contexttypes.PublishingHouse;
import no.unit.nva.model.contexttypes.Series;
import no.unit.nva.model.contexttypes.UnconfirmedPublisher;
import no.unit.nva.model.contexttypes.UnconfirmedSeries;
import no.unit.nva.model.exceptions.InvalidIsbnException;
import no.unit.nva.model.exceptions.InvalidUnconfirmedSeriesException;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.degree.DegreePhd;
import no.unit.nva.model.instancetypes.degree.RelatedDocument;
import no.unit.nva.model.pages.MonographPages;
import no.unit.nva.model.pages.Pages;
import nva.commons.core.StringUtils;

public class CristinImportPublicationMerger {

    public static final String DUMMY_HANDLE_THAT_EXIST_FOR_PROCESSING_UNIS
        = "dummy_handle_unis";

    private final Publication cristinPublication;
    private final PublicationRepresentation bragePublication;

    public CristinImportPublicationMerger(Publication existingPublication, PublicationRepresentation bragePublication) {
        this.cristinPublication = existingPublication;
        this.bragePublication = bragePublication;
    }

    public Publication mergePublications() throws InvalidIsbnException, InvalidUnconfirmedSeriesException {
        preMergeValidation();
        var publicationForUpdating = cristinPublication.copy()
                                         .withAdditionalIdentifiers(mergeAdditionalIdentifiers())
                                         .withSubjects(determineSubject())
                                         .withRightsHolder(determineRightsHolder())
                                         .withEntityDescription(determineEntityDescription())
                                         .build();
        return fillNewPublicationWithMetadataFromBrage(publicationForUpdating);
    }

    private void preMergeValidation() {
        PreMergeValidator.validate(cristinPublication);
    }

    private EntityDescription determineEntityDescription()
        throws InvalidIsbnException, InvalidUnconfirmedSeriesException {
        return cristinPublication.getEntityDescription().copy()
                   .withContributors(determineContributors())
                   .withReference(determineReference())
                   .build();
    }

    private List<Contributor> determineContributors() {
        return cristinPublication.getEntityDescription().getContributors().isEmpty()
               ? bragePublication.publication().getEntityDescription().getContributors()
               : cristinPublication.getEntityDescription().getContributors();
    }

    private Reference determineReference() throws InvalidIsbnException, InvalidUnconfirmedSeriesException {
        var reference = cristinPublication.getEntityDescription().getReference();
        reference.setPublicationContext(determinePublicationContext(reference));
        reference.setPublicationInstance(determincePublicationInstance(reference));
        reference.setDoi(determineDoi(reference));
        return reference;
    }

    private PublicationInstance<? extends Pages> determincePublicationInstance(Reference reference) {
        var publicationInstance = reference.getPublicationInstance();
        var bragePublicationInstance =
            bragePublication.publication().getEntityDescription().getReference().getPublicationInstance();

        if (publicationInstance instanceof DegreePhd degreePhd && bragePublicationInstance instanceof DegreePhd brageDegreePhd) {
            return new DegreePhd(getPages(degreePhd.getPages(), brageDegreePhd.getPages()),
                                 getDate(degreePhd.getSubmittedDate(), brageDegreePhd.getSubmittedDate()),
                                 getRelated(degreePhd.getRelated(), brageDegreePhd.getRelated()));
        }
        else {
            return publicationInstance;
        }
    }

    private Set<RelatedDocument> getRelated(Set<RelatedDocument> documents, Set<RelatedDocument> brageDocuments) {
        if (nonNull(documents) && !documents.isEmpty()) {
            var mergedDocuments = new LinkedHashSet<RelatedDocument>();
            mergedDocuments.addAll(documents);
            mergedDocuments.addAll(brageDocuments);
            return mergedDocuments;
        } else {
            return brageDocuments;
        }
    }

    private PublicationDate getDate(PublicationDate submittedDate, PublicationDate brageDate) {
        return nonNull(submittedDate) ? submittedDate : brageDate;
    }

    private MonographPages getPages(MonographPages pages, MonographPages bragePages) {
        return nonNull(pages) ? pages : bragePages;
    }

    private URI determineDoi(Reference reference) {
        return nonNull(reference.getDoi())
                   ? reference.getDoi()
                   : bragePublication.publication().getEntityDescription()
                         .getReference()
                         .getDoi();
    }

    private PublicationContext determinePublicationContext(Reference reference) throws InvalidIsbnException, InvalidUnconfirmedSeriesException {
        var publicationContext = reference.getPublicationContext();
        var bragePublicationContext = bragePublication.publication().getEntityDescription().getReference().getPublicationContext();
        if (publicationContext instanceof Degree degree && bragePublicationContext instanceof Degree brageDegree) {
            return new Builder().withIsbnList(getIsbnList(degree.getIsbnList(), brageDegree.getIsbnList()))
                               .withSeries(getSeries(degree.getSeries(), brageDegree.getSeries()))
                               .withPublisher(getPublisher(degree.getPublisher(), brageDegree.getPublisher()))
                               .withSeriesNumber(getSeriesNumber(degree, brageDegree))
                               .withCourse(getCourse(degree))
                               .build();
        }
        if (publicationContext instanceof Book book && bragePublicationContext instanceof Book brageBook) {
            return new BookBuilder()
                       .withIsbnList(getIsbnList(book.getIsbnList(), brageBook.getIsbnList()))
                       .withPublisher(getPublisher(book.getPublisher(), brageBook.getPublisher()))
                       .withSeries(getSeries(book.getSeries(), brageBook.getSeries()))
                       .withSeriesNumber(getSeriesNumber(book, brageBook))
                       .withRevision(nonNull(book.getRevision()) ? book.getRevision() : brageBook.getRevision())
                       .build();
        }
        else {
            return publicationContext;
        }
    }

    private Course getCourse(Degree degree) {
        return nonNull(degree.getCourse()) ? degree.getCourse() : extractBrageCourse();
    }

    private static String getSeriesNumber(Book book, Book brageBook) {
        return nonNull(book.getSeriesNumber()) ? book.getSeriesNumber() :
                                                                                brageBook.getSeriesNumber();
    }

    private PublishingHouse getPublisher(PublishingHouse existingPublisher, PublishingHouse bragePublisher) {
        if (nonNull(existingPublisher) && existingPublisher instanceof Publisher publisher) {
            return publisher;
        }
        if (nonNull(bragePublisher) && bragePublisher instanceof Publisher publisher) {
            return publisher;
        }
        if (nonNull(existingPublisher) && existingPublisher instanceof UnconfirmedPublisher unconfirmedPublisher) {
            return unconfirmedPublisher;
        } else {
            return bragePublisher;
        }
    }

    private static BookSeries getSeries(BookSeries degree, BookSeries brageDegree) {
        if (nonNull(degree) && degree instanceof Series series) {
            return series;
        }
        if (nonNull(brageDegree) && brageDegree instanceof Series series) {
            return series;
        }
        if (nonNull(degree) && degree instanceof UnconfirmedSeries unconfirmedSeries) {
            return unconfirmedSeries;
        } else {
            return brageDegree;
        }
    }

    private static List<String> getIsbnList(List<String> existingList, List<String> brageList) {
        return nonNull(existingList) && !existingList.isEmpty() ? existingList : brageList;
    }

    private Course extractBrageCourse() {
        return Optional.ofNullable(bragePublication.publication().getEntityDescription().getReference().getPublicationContext())
                   .filter(Degree.class::isInstance)
                   .map(Degree.class::cast)
                   .map(Degree::getCourse)
                   .orElse(null);
    }

    private String determineRightsHolder() {
        return nonNull(cristinPublication.getRightsHolder())
                   ? cristinPublication.getRightsHolder()
                   : bragePublication.publication().getRightsHolder();
    }

    private List<URI> determineSubject() {
        return cristinPublication.getSubjects().isEmpty()
                   ? bragePublication.publication().getSubjects()
                   : cristinPublication.getSubjects();
    }

    private Set<AdditionalIdentifier> mergeAdditionalIdentifiers() {
        var additionalIdentifiers = new HashSet<>(cristinPublication.getAdditionalIdentifiers());
        additionalIdentifiers.addAll(bragePublication.publication().getAdditionalIdentifiers());
        return additionalIdentifiers;
    }

    private Publication fillNewPublicationWithMetadataFromBrage(Publication publicationForUpdating) {
        publicationForUpdating.getEntityDescription().setDescription(getCorrectDescription());
        publicationForUpdating.getEntityDescription().setAbstract(getCorrectAbstract());
        publicationForUpdating.setAssociatedArtifacts(determineAssociatedArtifacts());
        return publicationForUpdating;
    }

    private AssociatedArtifactList determineAssociatedArtifacts() {
        return shouldUseBrageArtifacts()
                   ? bragePublication.publication().getAssociatedArtifacts()
                   : cristinPublication.getAssociatedArtifacts();
    }

    private boolean shouldUseBrageArtifacts() {
        return bragePublicationHasAssociatedArtifacts()
               && (cristinPublication.getAssociatedArtifacts().isEmpty()
                   || hasTheSameHandle());
    }

    private boolean bragePublicationHasAssociatedArtifacts() {
        return !bragePublication.publication().getAssociatedArtifacts().isEmpty();
    }


    private boolean hasTheSameHandle() {
        return bragePublication.brageRecord().getId().equals(cristinPublication.getHandle());
    }

    private String getCorrectDescription() {
        return StringUtils.isNotEmpty(cristinPublication.getEntityDescription().getDescription())
                   ? cristinPublication.getEntityDescription().getDescription()
                   : bragePublication.publication().getEntityDescription().getDescription();
    }

    private String getCorrectAbstract() {
        return StringUtils.isNotEmpty(cristinPublication.getEntityDescription().getAbstract())
                   ? cristinPublication.getEntityDescription().getAbstract()
                   : bragePublication.publication().getEntityDescription().getAbstract();
    }
}
