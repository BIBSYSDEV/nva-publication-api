package no.sikt.nva.brage.migration.merger;

import static java.util.Objects.nonNull;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import no.sikt.nva.brage.migration.merger.publicationcontextmerger.AnthologyMerger;
import no.sikt.nva.brage.migration.merger.publicationcontextmerger.BookMerger;
import no.sikt.nva.brage.migration.merger.publicationcontextmerger.DegreeMerger;
import no.sikt.nva.brage.migration.merger.publicationcontextmerger.EventMerger;
import no.sikt.nva.brage.migration.merger.publicationcontextmerger.GeographicalContentMerger;
import no.sikt.nva.brage.migration.merger.publicationcontextmerger.JournalMerger;
import no.sikt.nva.brage.migration.merger.publicationcontextmerger.MediaContributionMerger;
import no.sikt.nva.brage.migration.merger.publicationcontextmerger.ReportMerger;
import no.sikt.nva.brage.migration.merger.publicationcontextmerger.ResearchDataMerger;
import no.sikt.nva.brage.migration.merger.publicationinstancemerger.PublicationInstanceMerger;
import no.sikt.nva.brage.migration.model.PublicationRepresentation;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Publication;
import no.unit.nva.model.Reference;
import no.unit.nva.model.associatedartifacts.AssociatedArtifact;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import no.unit.nva.model.associatedartifacts.file.AdministrativeAgreement;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.associatedartifacts.file.PublishedFile;
import no.unit.nva.model.associatedartifacts.file.PublisherVersion;
import no.unit.nva.model.contexttypes.Anthology;
import no.unit.nva.model.contexttypes.Book;
import no.unit.nva.model.contexttypes.Degree;
import no.unit.nva.model.contexttypes.Event;
import no.unit.nva.model.contexttypes.GeographicalContent;
import no.unit.nva.model.contexttypes.MediaContribution;
import no.unit.nva.model.contexttypes.Periodical;
import no.unit.nva.model.contexttypes.PublicationContext;
import no.unit.nva.model.contexttypes.Report;
import no.unit.nva.model.contexttypes.ResearchData;
import no.unit.nva.model.exceptions.InvalidIsbnException;
import no.unit.nva.model.exceptions.InvalidIssnException;
import no.unit.nva.model.exceptions.InvalidUnconfirmedSeriesException;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.journal.AcademicArticle;

import no.unit.nva.model.pages.Pages;
import nva.commons.core.StringUtils;

public class CristinImportPublicationMerger {

    public static final String DUMMY_HANDLE_THAT_EXIST_FOR_PROCESSING_UNIS
        = "dummy_handle_unis";

    private final Publication existingPublication;
    private final PublicationRepresentation bragePublicationRepresentation;

    public CristinImportPublicationMerger(Publication existingPublication, PublicationRepresentation bragePublication) {
        this.existingPublication = existingPublication;
        this.bragePublicationRepresentation = bragePublication;
    }

    public Publication mergePublications()
        throws InvalidIsbnException, InvalidUnconfirmedSeriesException, InvalidIssnException {
        return PreMergeValidator.shouldNotMergeMetadata(bragePublicationRepresentation, existingPublication)
                   ? injectBrageHandleOnly()
                   : mergePublicationsMetadata();
    }

    private Publication injectBrageHandleOnly() {
        return existingPublication.copy()
                   .withAdditionalIdentifiers(mergeAdditionalIdentifiers())
                   .build();
    }

    private Publication mergePublicationsMetadata()
        throws InvalidIsbnException, InvalidUnconfirmedSeriesException, InvalidIssnException {
        return existingPublication.copy()
                                         .withAdditionalIdentifiers(mergeAdditionalIdentifiers())
                                         .withSubjects(determineSubject())
                                         .withRightsHolder(determineRightsHolder())
                                         .withEntityDescription(determineEntityDescription())
                                         .withAssociatedArtifacts(determineAssociatedArtifacts())
                                         .build();
    }

    private EntityDescription determineEntityDescription()
        throws InvalidIsbnException, InvalidUnconfirmedSeriesException, InvalidIssnException {
        return existingPublication.getEntityDescription().copy()
                   .withContributors(determineContributors())
                   .withReference(determineReference())
                   .withDescription(getCorrectDescription())
                   .withAbstract(getCorrectAbstract())
                   .build();
    }

    private List<Contributor> determineContributors() {
        return existingPublication.getEntityDescription().getContributors().isEmpty()
               ? bragePublicationRepresentation.publication().getEntityDescription().getContributors()
               : existingPublication.getEntityDescription().getContributors();
    }

    private Reference determineReference()
        throws InvalidIsbnException, InvalidUnconfirmedSeriesException, InvalidIssnException {
        var reference = existingPublication.getEntityDescription().getReference();
        reference.setPublicationContext(determinePublicationContext(reference));
        reference.setPublicationInstance(determincePublicationInstance(reference));
        reference.setDoi(determineDoi(reference));
        return reference;
    }

    private PublicationInstance<? extends Pages> determincePublicationInstance(Reference reference) {
        var publicationInstance = reference.getPublicationInstance();
        var newPublicationInstance =
            bragePublicationRepresentation.publication().getEntityDescription().getReference().getPublicationInstance();
        return PublicationInstanceMerger.of(publicationInstance).merge(newPublicationInstance);
    }

    private URI determineDoi(Reference reference) {
        return nonNull(reference.getDoi())
                   ? reference.getDoi()
                   : bragePublicationRepresentation.publication().getEntityDescription()
                         .getReference()
                         .getDoi();
    }

    private PublicationContext determinePublicationContext(Reference reference)
        throws InvalidIsbnException, InvalidUnconfirmedSeriesException, InvalidIssnException {
        var publicationContext = reference.getPublicationContext();
        var bragePublicationContext = bragePublicationRepresentation.publication().getEntityDescription().getReference().getPublicationContext();
        return switch (publicationContext) {
            case Degree degree -> DegreeMerger.merge(degree, bragePublicationContext);
            case Report report -> ReportMerger.merge(report, bragePublicationContext);
            case Book book -> BookMerger.merge(book, bragePublicationContext);
            case Periodical journal -> JournalMerger.merge(journal, bragePublicationContext);
            case Event event -> EventMerger.merge(event, publicationContext);
            case Anthology anthology -> AnthologyMerger.merge(anthology, publicationContext);
            case MediaContribution mediaContribution -> MediaContributionMerger.merge(mediaContribution, publicationContext);
            case ResearchData researchData -> ResearchDataMerger.merge(researchData, publicationContext);
            case GeographicalContent geographicalContent -> GeographicalContentMerger.merge(geographicalContent, publicationContext);
            default -> publicationContext;
        };
    }

    private String determineRightsHolder() {
        return nonNull(existingPublication.getRightsHolder())
                   ? existingPublication.getRightsHolder()
                   : bragePublicationRepresentation.publication().getRightsHolder();
    }

    private List<URI> determineSubject() {
        return existingPublication.getSubjects().isEmpty()
                   ? bragePublicationRepresentation.publication().getSubjects()
                   : existingPublication.getSubjects();
    }

    private Set<AdditionalIdentifier> mergeAdditionalIdentifiers() {
        var additionalIdentifiers = new HashSet<>(existingPublication.getAdditionalIdentifiers());
        additionalIdentifiers.addAll(bragePublicationRepresentation.publication().getAdditionalIdentifiers());
        return additionalIdentifiers;
    }

    private AssociatedArtifactList determineAssociatedArtifacts() {
        if (existingPublication.getAssociatedArtifacts().isEmpty()) {
            return bragePublicationRepresentation.publication().getAssociatedArtifacts();
        }
        if (!hasAdministrativeAgreement(existingPublication) && hasAdministrativeAgreement(
            bragePublicationRepresentation.publication())) {
            var administrativeAgreements = extractAdministrativeAgreements(
                bragePublicationRepresentation.publication());
            existingPublication.getAssociatedArtifacts().addAll(administrativeAgreements);
            return existingPublication.getAssociatedArtifacts();
        }
        if (shouldOverWriteWithBrageArtifacts()) {
            return bragePublicationRepresentation.publication().getAssociatedArtifacts();
        }
        return existingPublication.getAssociatedArtifacts();
    }

    private boolean shouldOverWriteWithBrageArtifacts() {
        return bragePublicationHasAssociatedArtifacts() && (hasTheSameHandle() || academicArticleRulesApply());
    }

    private boolean academicArticleRulesApply() {
        return isAcademicArticle()
               && noneOfTheExistingFilesArePublishedVersion(extractPublishedFiles(existingPublication))
               && brageFileIsPublishedVersion(extractPublishedFiles(bragePublicationRepresentation.publication()));
    }

    private List<PublishedFile> extractPublishedFiles(Publication publication) {
        return publication.getAssociatedArtifacts()
                   .stream()
                   .filter(associatedArtifact -> associatedArtifact instanceof File)
                   .map(associatedArtifact -> (File) associatedArtifact)
                   .filter(file -> file instanceof PublishedFile)
                   .map(file -> (PublishedFile) file)
                   .toList();
    }

    private boolean brageFileIsPublishedVersion(List<PublishedFile> publishedFiles) {
        return publishedFiles
                   .stream()
                   .anyMatch(publishedFile -> PublisherVersion.PUBLISHED_VERSION ==
                                              publishedFile.getPublisherVersion());
    }

    private boolean noneOfTheExistingFilesArePublishedVersion(List<PublishedFile> publishedFiles) {
        return publishedFiles
                   .stream()
                   .noneMatch(publishedFile -> PublisherVersion.PUBLISHED_VERSION == publishedFile.getPublisherVersion());
    }

    private boolean isAcademicArticle() {
        return existingPublication.getEntityDescription()
                        .getReference()
                        .getPublicationInstance() instanceof AcademicArticle;

    }

    private boolean bragePublicationHasAssociatedArtifacts() {
        return !bragePublicationRepresentation.publication().getAssociatedArtifacts().isEmpty();
    }

    private boolean hasTheSameHandle() {
        return bragePublicationRepresentation.brageRecord().getId().equals(existingPublication.getHandle());
    }

    private List<AssociatedArtifact> extractAdministrativeAgreements(Publication publication) {
        return publication.getAssociatedArtifacts().stream()
                   .filter(AdministrativeAgreement.class::isInstance)
                   .toList();
    }

    private boolean hasAdministrativeAgreement(Publication publication) {
        return publication.getAssociatedArtifacts().stream()
                   .anyMatch(AdministrativeAgreement.class::isInstance);
    }

    private String getCorrectDescription() {
        return StringUtils.isNotEmpty(existingPublication.getEntityDescription().getDescription())
                   ? existingPublication.getEntityDescription().getDescription()
                   : bragePublicationRepresentation.publication().getEntityDescription().getDescription();
    }

    private String getCorrectAbstract() {
        return StringUtils.isNotEmpty(existingPublication.getEntityDescription().getAbstract())
                   ? existingPublication.getEntityDescription().getAbstract()
                   : bragePublicationRepresentation.publication().getEntityDescription().getAbstract();
    }
}
