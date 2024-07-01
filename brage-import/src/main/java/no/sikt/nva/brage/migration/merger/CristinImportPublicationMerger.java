package no.sikt.nva.brage.migration.merger;

import static java.util.Objects.nonNull;
import static java.util.function.Predicate.not;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
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
import no.unit.nva.model.funding.Funding;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.journal.AcademicArticle;
import no.unit.nva.model.pages.Pages;
import no.unit.nva.model.role.Role;
import no.unit.nva.model.role.RoleType;
import nva.commons.core.StringUtils;

public class CristinImportPublicationMerger {

    public static final String DUMMY_HANDLE_THAT_EXIST_FOR_PROCESSING_UNIS
        = "dummy_handle_unis";
    public static final String PRIORITIZE_CONTRIBUTORS_WITH_CREATOR_ROLE = "contributorsWithCreatorRole";
    public static final String PRIORITIZE_MAIN_TITLE = "mainTitle";
    public static final String PRIORITIZE_ALTERNATIVE_TITLES = "alternativeTitles";
    public static final String PRIORITIZE_ABSTRACT = "abstract";
    public static final String PRIORITIZE_ALTERNATIVE_ABSTRACTS = "alternativeAbstracts";
    public static final String PRIORITIZE_REFERENCE = "reference";
    public static final String PRIORITIZE_TAGS = "tags";
    public static final String DUBLIN_CORE_XML = "dublin_core.xml";
    public static final String PRIORITIZE_FUNDINGS = "fundings";

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
                   .withFundings(determineFundings())
                   .build();
    }

    private List<Funding> determineFundings() {
        if (existingPublication.getFundings().isEmpty()) {
            return bragePublicationRepresentation.publication().getFundings();
        }
        if (shouldPrioritizeField(PRIORITIZE_FUNDINGS)) {
            return mergeFundings();
        }
        return existingPublication.getFundings();
    }

    private List<Funding> mergeFundings() {
        var fundings = new ArrayList<>(existingPublication.getFundings());
        fundings.addAll(bragePublicationRepresentation.publication().getFundings());
        return fundings;
    }

    private EntityDescription determineEntityDescription()
        throws InvalidIsbnException, InvalidUnconfirmedSeriesException, InvalidIssnException {
        return existingPublication.getEntityDescription().copy()
                   .withMainTitle(determineMainTitle())
                   .withAlternativeTitles(determineAlternativeTitles())
                   .withContributors(determineContributors())
                   .withReference(determineReference())
                   .withDescription(getCorrectDescription())
                   .withAbstract(getCorrectAbstract())
                   .withAlternativeAbstracts(determineAlternativeAbstracts())
                   .withTags(determineTags())
                   .build();
    }

    private List<String> determineTags() {
        return shouldPrioritizeField(PRIORITIZE_TAGS)
                   ? mergeTags()
                   : existingPublication.getEntityDescription().getTags();
    }

    private List<String> mergeTags() {
        var tags = new HashSet<>(existingPublication.getEntityDescription().getTags());
        bragePublicationRepresentation.publication().getEntityDescription().getTags().stream()
            .filter(tag -> tags.stream().noneMatch(exTag -> exTag.equalsIgnoreCase(tag)))
            .forEach(tags::add);
        return tags.stream().toList();
    }

    private Map<String, String> determineAlternativeAbstracts() {
        return shouldPrioritizeAlternativeAbstractsFromBrage()
                   ? bragePublicationRepresentation.publication().getEntityDescription().getAlternativeAbstracts()
                   : existingPublication.getEntityDescription().getAlternativeAbstracts();
    }

    private boolean shouldPrioritizeAlternativeAbstractsFromBrage() {
        return shouldPrioritizeField(PRIORITIZE_ALTERNATIVE_ABSTRACTS);
    }

    private Map<String, String> determineAlternativeTitles() {
        return shouldPrioritizeAlternativeTitlesFromBrage()
                   ? bragePublicationRepresentation.publication().getEntityDescription().getAlternativeTitles()
                   : existingPublication.getEntityDescription().getAlternativeTitles();
    }

    private boolean shouldPrioritizeAlternativeTitlesFromBrage() {
        return shouldPrioritizeField(PRIORITIZE_ALTERNATIVE_TITLES);
    }

    private boolean shouldPrioritizeField(String field) {
        return bragePublicationRepresentation.brageRecord().getPrioritizedProperties().contains(field);
    }

    private String determineMainTitle() {
        return shouldPrioritizeMainTitleFromBrage()
                   ? bragePublicationRepresentation.publication().getEntityDescription().getMainTitle()
                   : existingPublication.getEntityDescription().getMainTitle();
    }

    private boolean shouldPrioritizeMainTitleFromBrage() {
        return shouldPrioritizeField(PRIORITIZE_MAIN_TITLE);
    }

    private List<Contributor> determineContributors() {
        return existingPublication.getEntityDescription().getContributors().isEmpty()
                   ? bragePublicationRepresentation.publication().getEntityDescription().getContributors()
                   : mergeContributors();
    }

    private List<Contributor> mergeContributors() {
        if (shouldPrioritizeContributorsWithCreatorRole()) {
            return replaceExistingCreatorsWithBrageCreators();
        }
        return existingPublication.getEntityDescription().getContributors();
    }

    private List<Contributor> replaceExistingCreatorsWithBrageCreators() {
        var nonCreatorExistingContributors = filterByNonCreatorContributors(existingPublication);
        var creatorBrageContributors = filterByCreatorContributors(bragePublicationRepresentation.publication());
        return Stream.concat(nonCreatorExistingContributors, creatorBrageContributors).toList();
    }

    private Stream<Contributor> filterByCreatorContributors(Publication publication) {
        return publication.getEntityDescription().getContributors().stream().filter(this::isCreator);
    }

    private Stream<Contributor> filterByNonCreatorContributors(Publication existingPublication) {
        return existingPublication.getEntityDescription().getContributors().stream().filter(not(this::isCreator));
    }

    private boolean isCreator(Contributor contributor) {
        return Optional.ofNullable(contributor.getRole())
                   .map(RoleType::getType)
                   .map(role -> Role.CREATOR == role)
                   .orElse(false);
    }

    private boolean shouldPrioritizeContributorsWithCreatorRole() {
        return shouldPrioritizeField(PRIORITIZE_CONTRIBUTORS_WITH_CREATOR_ROLE);
    }

    private Reference determineReference()
        throws InvalidIsbnException, InvalidUnconfirmedSeriesException, InvalidIssnException {
        if (shouldPrioritizeReferenceFromBrage()) {
            return bragePublicationRepresentation.publication().getEntityDescription().getReference();
        }
        var reference = existingPublication.getEntityDescription().getReference();
        return new Reference.Builder()
                   .withPublicationInstance(determincePublicationInstance(reference))
                   .withPublishingContext(determinePublicationContext(reference))
                   .withDoi(determineDoi(reference))
                   .build();
    }

    private boolean shouldPrioritizeReferenceFromBrage() {
        return shouldPrioritizeField(PRIORITIZE_REFERENCE);
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
        var record = bragePublicationRepresentation.brageRecord();
        var bragePublicationContext = bragePublicationRepresentation.publication()
                                          .getEntityDescription()
                                          .getReference()
                                          .getPublicationContext();
        return switch (publicationContext) {
            case Degree degree -> new DegreeMerger(record).merge(degree, bragePublicationContext);
            case Report report -> new ReportMerger(record).merge(report, bragePublicationContext);
            case Book book -> new BookMerger(record).merge(book, bragePublicationContext);
            case Periodical journal -> JournalMerger.merge(journal, bragePublicationContext);
            case Event event -> EventMerger.merge(event, publicationContext);
            case Anthology anthology -> AnthologyMerger.merge(anthology, publicationContext);
            case MediaContribution mediaContribution ->
                MediaContributionMerger.merge(mediaContribution, publicationContext);
            case ResearchData researchData -> new ResearchDataMerger(record).merge(researchData, publicationContext);
            case GeographicalContent geographicalContent ->
                new GeographicalContentMerger(record).merge(geographicalContent,
                                                            publicationContext);
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

        if (shouldOverWriteWithBrageArtifacts()) {
            return keepBrageAssociatedArtifactAndKeepDublinCoreFromExistsing();
        }
        if (!hasAdministrativeAgreement(existingPublication)) {
            var associatedArtifacts = new ArrayList<>(existingPublication.getAssociatedArtifacts());
            var administrativeAgreements = extractAdministrativeAgreements(
                bragePublicationRepresentation.publication());
            associatedArtifacts.addAll(administrativeAgreements);
            return new AssociatedArtifactList(associatedArtifacts);
        }
        return existingPublication.getAssociatedArtifacts();
    }

    private AssociatedArtifactList keepBrageAssociatedArtifactAndKeepDublinCoreFromExistsing() {
        var associatedArtifacts = new ArrayList<>(
            bragePublicationRepresentation.publication().getAssociatedArtifacts());
        var dublinCoresFromExisting = extractDublinCores(existingPublication.getAssociatedArtifacts());
        associatedArtifacts.addAll(dublinCoresFromExisting);
        return new AssociatedArtifactList(associatedArtifacts);
    }

    private List<File> extractDublinCores(AssociatedArtifactList associatedArtifacts) {
        return associatedArtifacts.stream().filter(a -> a instanceof File)
                   .map(a -> (File) a)
                   .filter(file -> DUBLIN_CORE_XML.equals(file.getName()))
                   .toList();
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
                   .filter(PublishedFile.class::isInstance)
                   .map(PublishedFile.class::cast)
                   .toList();
    }

    private boolean brageFileIsPublishedVersion(List<PublishedFile> publishedFiles) {
        return publishedFiles
                   .stream()
                   .map(File::getPublisherVersion)
                   .anyMatch(PublisherVersion.PUBLISHED_VERSION::equals);
    }

    private boolean noneOfTheExistingFilesArePublishedVersion(List<PublishedFile> publishedFiles) {
        return publishedFiles
                   .stream()
                   .noneMatch(
                       publishedFile -> PublisherVersion.PUBLISHED_VERSION == publishedFile.getPublisherVersion());
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
        if (StringUtils.isEmpty(existingPublication.getEntityDescription().getAbstract())) {
            return bragePublicationRepresentation.publication().getEntityDescription().getAbstract();
        }
        if (shouldPrioritizeAbstractFromBrage()) {
            return bragePublicationRepresentation.publication().getEntityDescription().getAbstract();
        }
        return existingPublication.getEntityDescription().getAbstract();
    }

    private boolean shouldPrioritizeAbstractFromBrage() {
        return shouldPrioritizeField(PRIORITIZE_ABSTRACT);
    }
}
