package no.sikt.nva.brage.migration.merger;

import static java.util.Objects.nonNull;
import static no.sikt.nva.brage.migration.merger.AssociatedArtifactsMerger.merge;
import static no.unit.nva.model.role.Role.CREATOR;
import static no.unit.nva.model.role.Role.SUPERVISOR;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
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
import no.unit.nva.model.Contributor;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.Reference;
import no.unit.nva.model.additionalidentifiers.AdditionalIdentifierBase;
import no.unit.nva.model.additionalidentifiers.CristinIdentifier;
import no.unit.nva.model.associatedartifacts.AssociatedArtifact;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.associatedartifacts.file.HiddenFile;
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
import no.unit.nva.model.pages.Pages;
import nva.commons.core.StringUtils;

@SuppressWarnings("PMD.CouplingBetweenObjects")
public class CristinImportPublicationMerger {

    public static final String DUMMY_HANDLE_THAT_EXIST_FOR_PROCESSING_UNIS = "dummy_handle_unis";
    public static final String PRIORITIZE_MAIN_TITLE = "mainTitle";
    public static final String PRIORITIZE_ALTERNATIVE_TITLES = "alternativeTitles";
    public static final String PRIORITIZE_PUBLICATION_DATE = "publicationDate";
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
                   ? mergeMinimalPublicationMetadata() : mergePublicationsMetadata();
    }

    private static Contributor updateCreatorAffiliation(Contributor contributor, Organization organization) {
        return isCreator(contributor) ? contributor.copy().withAffiliations(List.of(organization)).build()
                   : contributor;
    }

    private static boolean isCreator(Contributor contributor) {
        return CREATOR.equals(contributor.getRole().getType());
    }

    private static boolean isDublinCore(File file) {
        return DUBLIN_CORE_XML.equals(file.getName());
    }

    private Publication mergeMinimalPublicationMetadata() {
        return existingPublication.copy()
                   .withAdditionalIdentifiers(mergeAdditionalIdentifiers())
                   .withAssociatedArtifacts(addDublinCoreToExistingAssociatedArtifacts())
                   .withEntityDescription(existingPublication.getEntityDescription()
                                              .copy()
                                              .withTags(determineTags())
                                              .withPublicationDate(bragePublicationRepresentation.publication()
                                                                       .getEntityDescription()
                                                                       .getPublicationDate())
                                              .build())
                   .build();
    }

    private List<AssociatedArtifact> addDublinCoreToExistingAssociatedArtifacts() {
        var associatedArtifacts = new ArrayList<>(existingPublication.getAssociatedArtifacts());
        var dublinCore = extractDublinCore(bragePublicationRepresentation.publication().getAssociatedArtifacts());
        associatedArtifacts.addAll(dublinCore);
        return associatedArtifacts;
    }

    private Publication mergePublicationsMetadata()
        throws InvalidIsbnException, InvalidUnconfirmedSeriesException, InvalidIssnException {
        return existingPublication.copy()
                   .withAdditionalIdentifiers(mergeAdditionalIdentifiers())
                   .withSubjects(determineSubject())
                   .withRightsHolder(determineRightsHolder())
                   .withEntityDescription(determineEntityDescription())
                   .withAssociatedArtifacts(merge(existingPublication.getAssociatedArtifacts(),
                                                  bragePublicationRepresentation.publication()
                                                      .getAssociatedArtifacts()))
                   .withFundings(determineFundings())
                   .build();
    }

    private Set<Funding> determineFundings() {
        if (existingPublication.getFundings().isEmpty()) {
            return bragePublicationRepresentation.publication().getFundings();
        }
        if (shouldPrioritizeField(PRIORITIZE_FUNDINGS)) {
            return mergeFundings();
        }
        return existingPublication.getFundings();
    }

    private Set<Funding> mergeFundings() {
        var fundings = new HashSet<>(existingPublication.getFundings());
        fundings.addAll(bragePublicationRepresentation.publication().getFundings());
        return fundings;
    }

    private EntityDescription determineEntityDescription()
        throws InvalidIsbnException, InvalidUnconfirmedSeriesException, InvalidIssnException {
        return existingPublication.getEntityDescription()
                   .copy()
                   .withMainTitle(determineMainTitle())
                   .withAlternativeTitles(determineAlternativeTitles())
                   .withContributors(determineContributors())
                   .withReference(determineReference())
                   .withDescription(getCorrectDescription())
                   .withAbstract(getCorrectAbstract())
                   .withAlternativeAbstracts(determineAlternativeAbstracts())
                   .withTags(determineTags())
                   .withPublicationDate(determinePublicationDate())
                   .build();
    }

    private PublicationDate determinePublicationDate() {
        return shouldPrioritizeField(PRIORITIZE_PUBLICATION_DATE) ? bragePublicationRepresentation.publication()
                                                                        .getEntityDescription()
                                                                        .getPublicationDate()
                   : existingPublication.getEntityDescription().getPublicationDate();
    }

    private List<String> determineTags() {
        return mergeTags();
    }

    private List<String> mergeTags() {
        var tags = new HashSet<>(existingPublication.getEntityDescription().getTags());
        bragePublicationRepresentation.publication()
            .getEntityDescription()
            .getTags()
            .stream()
            .filter(tag -> tags.stream().filter(Objects::nonNull).noneMatch(exTag -> exTag.equalsIgnoreCase(tag)))
            .forEach(tags::add);
        return tags.stream().filter(Objects::nonNull).toList();
    }

    private Map<String, String> determineAlternativeAbstracts() {
        return shouldPrioritizeAlternativeAbstractsFromBrage() ? bragePublicationRepresentation.publication()
                                                                     .getEntityDescription()
                                                                     .getAlternativeAbstracts()
                   : existingPublication.getEntityDescription().getAlternativeAbstracts();
    }

    private boolean shouldPrioritizeAlternativeAbstractsFromBrage() {
        return shouldPrioritizeField(PRIORITIZE_ALTERNATIVE_ABSTRACTS);
    }

    private Map<String, String> determineAlternativeTitles() {
        return shouldPrioritizeAlternativeTitlesFromBrage() ? bragePublicationRepresentation.publication()
                                                                  .getEntityDescription()
                                                                  .getAlternativeTitles()
                   : existingPublication.getEntityDescription().getAlternativeTitles();
    }

    private boolean shouldPrioritizeAlternativeTitlesFromBrage() {
        return shouldPrioritizeField(PRIORITIZE_ALTERNATIVE_TITLES);
    }

    private boolean shouldPrioritizeField(String field) {
        return bragePublicationRepresentation.brageRecord().getPrioritizedProperties().contains(field);
    }

    private String determineMainTitle() {
        return shouldPrioritizeMainTitleFromBrage() ? bragePublicationRepresentation.publication()
                                                          .getEntityDescription()
                                                          .getMainTitle()
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
        if (isDegree(existingPublication)) {
            return updateExistingCreatorsAffiliationWithBrageAffiliation();
        }
        return existingPublication.getEntityDescription().getContributors();
    }

    private List<Contributor> updateExistingCreatorsAffiliationWithBrageAffiliation() {
        var contributors = new ArrayList<>(existingContributorsWithUpdatedAffiliation());
        if (shouldAddSupervisorFromBrage(contributors)) {
            contributors.addAll(extractIncomingSupervisors());
        }
        return contributors;
    }

    private boolean shouldAddSupervisorFromBrage(ArrayList<Contributor> contributors) {
        return isMissingSupervisor(contributors) && incomingPublicationHasSupervisor();
    }

    private List<Contributor> extractIncomingSupervisors() {
        return extractSupervisors().stream()
                   .filter(this::isNotContributorWithAnotherRoleInExistingPublication)
                   .toList();
    }

    private boolean isNotContributorWithAnotherRoleInExistingPublication(Contributor contributor) {
        var name = contributor.getIdentity().getName();
        return existingPublication.getEntityDescription()
                   .getContributors()
                   .stream()
                   .map(Contributor::getIdentity)
                   .map(Identity::getName)
                   .noneMatch(name::equals);
    }

    private List<Contributor> extractSupervisors() {
        return bragePublicationRepresentation.publication()
                   .getEntityDescription()
                   .getContributors()
                   .stream()
                   .filter(contributor -> SUPERVISOR.equals(contributor.getRole().getType()))
                   .toList();
    }

    private boolean incomingPublicationHasSupervisor() {
        return bragePublicationRepresentation.publication()
                   .getEntityDescription()
                   .getContributors()
                   .stream()
                   .anyMatch(contributor -> SUPERVISOR.equals(contributor.getRole().getType()));
    }

    private boolean isMissingSupervisor(List<Contributor> contributors) {
        return contributors.stream().noneMatch(contributor -> SUPERVISOR.equals(contributor.getRole().getType()));
    }

    private List<Contributor> existingContributorsWithUpdatedAffiliation() {
        return getBrageAffiliation().map(this::updateExistingCreatorWithAffiliation)
                   .orElseGet(() -> existingPublication.getEntityDescription().getContributors());
    }

    private List<Contributor> updateExistingCreatorWithAffiliation(Organization organization) {
        return existingPublication.getEntityDescription()
                   .getContributors()
                   .stream()
                   .map(contributor -> updateCreatorAffiliation(contributor, organization))
                   .toList();
    }

    private Optional<Organization> getBrageAffiliation() {
        return bragePublicationRepresentation.publication()
                   .getEntityDescription()
                   .getContributors()
                   .stream()
                   .map(Contributor::getAffiliations)
                   .flatMap(List::stream)
                   .filter(Organization.class::isInstance)
                   .map(Organization.class::cast)
                   .findFirst();
    }

    private boolean isDegree(Publication existingPublication) {
        return existingPublication.getEntityDescription().getReference().getPublicationContext() instanceof Degree;
    }

    private Reference determineReference()
        throws InvalidIsbnException, InvalidUnconfirmedSeriesException, InvalidIssnException {
        if (shouldPrioritizeReferenceFromBrage()) {
            return bragePublicationRepresentation.publication().getEntityDescription().getReference();
        }
        var reference = existingPublication.getEntityDescription().getReference();
        return new Reference.Builder().withPublicationInstance(determincePublicationInstance(reference))
                   .withPublishingContext(determinePublicationContext(reference))
                   .withDoi(determineDoi(reference))
                   .build();
    }

    private boolean shouldPrioritizeReferenceFromBrage() {
        return shouldPrioritizeField(PRIORITIZE_REFERENCE);
    }

    private PublicationInstance<? extends Pages> determincePublicationInstance(Reference reference) {
        var publicationInstance = reference.getPublicationInstance();
        var newPublicationInstance = bragePublicationRepresentation.publication()
                                         .getEntityDescription()
                                         .getReference()
                                         .getPublicationInstance();
        return PublicationInstanceMerger.of(publicationInstance).merge(newPublicationInstance);
    }

    private URI determineDoi(Reference reference) {
        return nonNull(reference.getDoi()) ? reference.getDoi()
                   : bragePublicationRepresentation.publication().getEntityDescription().getReference().getDoi();
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
                new GeographicalContentMerger(record).merge(geographicalContent, publicationContext);
            default -> publicationContext;
        };
    }

    private String determineRightsHolder() {
        return nonNull(existingPublication.getRightsHolder()) ? existingPublication.getRightsHolder()
                   : bragePublicationRepresentation.publication().getRightsHolder();
    }

    private List<URI> determineSubject() {
        return existingPublication.getSubjects().isEmpty() ? bragePublicationRepresentation.publication().getSubjects()
                   : existingPublication.getSubjects();
    }

    private Set<AdditionalIdentifierBase> mergeAdditionalIdentifiers() {
        var additionalIdentifiers = new HashSet<>(existingPublication.getAdditionalIdentifiers());
        if (bothPublicationsContainCristinIdentifier()) {
            var additionalIdentifiersExceptCristinIdentifier = getAdditionalIdentifiersExceptCristinIdentifier();
            additionalIdentifiers.addAll(additionalIdentifiersExceptCristinIdentifier);
        } else {
            additionalIdentifiers.addAll(bragePublicationRepresentation.publication().getAdditionalIdentifiers());
        }
        removePossiblyRedundantCristinIdentifier(additionalIdentifiers);
        return additionalIdentifiers;
    }

    private boolean bothPublicationsContainCristinIdentifier() {
        return existingPublication.getAdditionalIdentifiers().stream().anyMatch(CristinIdentifier.class::isInstance) &&
               bragePublicationRepresentation.publication()
                   .getAdditionalIdentifiers()
                   .stream()
                   .anyMatch(CristinIdentifier.class::isInstance);
    }

    private Set<AdditionalIdentifierBase> getAdditionalIdentifiersExceptCristinIdentifier() {
        return bragePublicationRepresentation.publication()
                   .getAdditionalIdentifiers()
                   .stream()
                   .filter(identifier -> !(identifier instanceof CristinIdentifier))
                   .collect(Collectors.toSet());
    }

    private void removePossiblyRedundantCristinIdentifier(HashSet<AdditionalIdentifierBase> additionalIdentifiers) {
        var cristinIdentifierOptional = getCristinIdentifier();
        cristinIdentifierOptional.ifPresent(
            cristinIdentifier -> removeCristinIdentifierFromBrage(additionalIdentifiers, cristinIdentifier));
    }

    private void removeCristinIdentifierFromBrage(HashSet<AdditionalIdentifierBase> additionalIdentifiers,
                                                  AdditionalIdentifierBase cristinIdentifier) {
        additionalIdentifiers.removeIf(
            additionalIdentifierBase -> cristinIdentifierFromBrageIsIdentical(cristinIdentifier,
                                                                              additionalIdentifierBase));
    }

    private boolean cristinIdentifierFromBrageIsIdentical(AdditionalIdentifierBase cristinIdentifier,
                                                          AdditionalIdentifierBase additionalIdentifierBase) {
        return additionalIdentifierBase.value().equals(cristinIdentifier.value()) &&
               !"Cristin".equalsIgnoreCase(additionalIdentifierBase.sourceName());
    }

    private Optional<AdditionalIdentifierBase> getCristinIdentifier() {
        return existingPublication.getAdditionalIdentifiers()
                   .stream()
                   .filter(
                       additionalIdentifierBase -> "Cristin".equalsIgnoreCase(additionalIdentifierBase.sourceName()))
                   .findFirst();
    }

    private List<HiddenFile> extractDublinCore(AssociatedArtifactList associatedArtifactList) {
        return associatedArtifactList.stream()
                   .filter(HiddenFile.class::isInstance)
                   .map(HiddenFile.class::cast)
                   .filter(CristinImportPublicationMerger::isDublinCore)
                   .toList();
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
