package no.unit.nva.cristin.mapper;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.unit.nva.cristin.lambda.constants.MappingConstants.HRCS_ACTIVITIES_MAP;
import static no.unit.nva.cristin.lambda.constants.MappingConstants.HRCS_CATEGORIES_MAP;
import static no.unit.nva.cristin.lambda.constants.MappingConstants.IGNORED_AND_POSSIBLY_EMPTY_PUBLICATION_FIELDS;
import static no.unit.nva.cristin.lambda.constants.MappingConstants.IGNORE_CONTRIBUTOR_FIELDS_ADDITIONALLY;
import static no.unit.nva.cristin.mapper.CristinHrcsCategoriesAndActivities.HRCS_ACTIVITY_URI;
import static no.unit.nva.cristin.mapper.CristinHrcsCategoriesAndActivities.HRCS_CATEGORY_URI;
import static no.unit.nva.cristin.mapper.CristinMainCategory.isBook;
import static no.unit.nva.cristin.mapper.CristinMainCategory.isReport;
import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValuesIgnoringFields;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.cristin.mapper.artisticproduction.CristinArtisticProduction;
import no.unit.nva.cristin.mapper.channelregistry.ChannelRegistryMapper;
import no.unit.nva.cristin.mapper.exhibition.CristinExhibition;
import no.unit.nva.cristin.mapper.nva.CristinMappingModule;
import no.unit.nva.cristin.mapper.nva.ReferenceBuilder;
import no.unit.nva.cristin.utils.NvaCustomer;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.Publication.Builder;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.PublicationNote;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.ResearchProject;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.funding.Funding;
import no.unit.nva.publication.external.services.RawContentRetriever;
import nva.commons.core.SingletonCollector;
import nva.commons.core.StringUtils;
import nva.commons.core.attempt.Try;
import nva.commons.core.language.LanguageMapper;

@SuppressWarnings({"PMD.GodClass",
    "PMD.CouplingBetweenObjects",
    "PMD.ForLoopCanBeForeach",
    "PMD.ReturnEmptyCollectionRatherThanNull"})
public class CristinMapper extends CristinMappingModule {

    public static final String EMPTY_STRING = "";
    public static final int FIRST_DAY_OF_MONTH = 1;
    public static final String CRISTIN_INSTITUTION_CODE = "CRIS";
    public static final String UNIT_INSTITUTION_CODE = "UNIT";
    public static final ResourceOwner SIKT_OWNER = new CristinLocale("SIKT", "20754", "0", "0",
                                                                     "0", null, null, null).toResourceOwner();

    private static final String SCOPUS_CASING_ACCEPTED_BY_FRONTEND = "Scopus";

    public CristinMapper(CristinObject cristinObject) {
        super(cristinObject, ChannelRegistryMapper.getInstance());
    }

    public static ZoneOffset zoneOffset() {
        return ZoneOffset.UTC.getRules().getOffset(Instant.now());
    }

    public Publication generatePublication(RawContentRetriever uriRetriever) {
        var resourceOwner = extractResourceOwner();
        Publication publication =
            new Builder()
                .withHandle(extractHandle())
                .withAdditionalIdentifiers(extractAdditionalIdentifiers())
                .withEntityDescription(generateEntityDescription())
                .withCreatedDate(extractDate())
                .withModifiedDate(extractEntryLastModifiedDate())
                .withPublishedDate(extractDate())
                .withPublisher(fetchPublisher(uriRetriever, resourceOwner))
                .withResourceOwner(resourceOwner)
                .withStatus(PublicationStatus.PUBLISHED)
                .withProjects(extractProjects())
                .withSubjects(generateNvaHrcsCategoriesAndActivities())
                .withFundings(extractFundings())
                .withPublicationNotes(extractPublicationNotes())
                .build();
        assertPublicationDoesNotHaveEmptyFields(publication);
        return publication;
    }

    private static Organization fetchPublisher(RawContentRetriever uriRetriever, ResourceOwner resourceOwner) {
        return NvaCustomer.fromCristinOrganization(resourceOwner.getOwnerAffiliation())
                   .fetch(uriRetriever)
                   .toOrganization();
    }

    private static Optional<URI> extractArchiveUri(List<CristinAssociatedUri> associatedUris) {
        return associatedUris
                   .stream()
                   .filter(CristinAssociatedUri::isArchive)
                   .findFirst() // Analysis of the cristin dataset shows that there is either 0 or 1 archive uri
                   // present.
                   .map(CristinAssociatedUri::toURI);
    }

    private static void addContributorNumberIfMissing(List<CristinContributor> cristinContributors) {
        if (allContributerNumbersAreNullValues(cristinContributors)) {
            addMissingSequenceNumberToAllContributors(cristinContributors);
        } else {
            addMissingSequenceNumbers(cristinContributors);
        }
    }

    private static void addMissingSequenceNumberToAllContributors(List<CristinContributor> cristinContributors) {
        for (int i = 0; i < cristinContributors.size(); i++) {
            cristinContributors.get(i).setContributorOrder(i);
        }
    }

    private static void addMissingSequenceNumbers(List<CristinContributor> cristinContributors) {
        for (int i = 0; i < cristinContributors.size(); i++) {
            if (isNull(cristinContributors.get(i).getContributorOrder())) {
                cristinContributors.get(i)
                    .setContributorOrder(cristinContributors.get(i - 1).getContributorOrder() + 1);
            }
        }
    }

    private static boolean allContributerNumbersAreNullValues(List<CristinContributor> cristinContributors) {
        return cristinContributors.stream().map(CristinContributor::getContributorOrder).noneMatch(Objects::nonNull);
    }

    private static List<CristinContributor> sortCristinContributors(List<CristinContributor> cristinContributors) {
        return cristinContributors.stream()
                   .sorted(Comparator.nullsLast(Comparator.naturalOrder()))
                   .collect(Collectors.toList());
    }

    private static String craftSourceCode(CristinSource cristinSource) {
        return SCOPUS_CASING_ACCEPTED_BY_FRONTEND.equalsIgnoreCase(cristinSource.getSourceCode())
                   ? SCOPUS_CASING_ACCEPTED_BY_FRONTEND
                   : cristinSource.getSourceCode();
    }

    private URI extractHandle() {
        return Optional.ofNullable(cristinObject.getCristinAssociatedUris())
                   .flatMap(CristinMapper::extractArchiveUri)
                   .orElse(null);
    }

    private List<PublicationNote> extractPublicationNotes() {
        return hasPublicationNote() ? List.of(new PublicationNote(cristinObject.getNote())) : List.of();
    }

    private boolean hasPublicationNote() {
        return StringUtils.isNotBlank(cristinObject.getNote());
    }

    private ResourceOwner extractResourceOwner() {
        var cristinLocales = getValidCristinLocales();
        if (shouldUseOwnerCodeCreated(cristinLocales)) {
            return CristinLocale.builder()
                       .withOwnerCode(cristinObject.getOwnerCodeCreated())
                       .withInstitutionIdentifier(cristinObject.getInstitutionIdentifierCreated())
                       .withDepartmentIdentifier(cristinObject.getDepartmentIdentifierCreated())
                       .withSubDepartmentIdentifier(cristinObject.getSubDepartmendIdentifierCreated())
                       .withGroupIdentifier(cristinObject.getGroupIdentifierCreated())
                       .build().toResourceOwner();
        }
        if (cristinLocalesContainsCristinOwnerCodeCreated(cristinLocales)) {
            return bestMatchingResourceOwner(cristinLocales);
        }
        return Optional.of(cristinLocales)
                   .flatMap(list -> list.stream().findFirst())
                   .map(CristinLocale::toResourceOwner)
                   .orElse(SIKT_OWNER);
    }

    private List<CristinLocale> getValidCristinLocales() {
        return Optional.ofNullable(cristinObject.getCristinLocales())
                   .map(list -> list.stream().filter(this::doesNotContainInvalidInstitutionCode))
                   .map(stream -> stream.collect(Collectors.toList()))
                   .orElse(List.of());
    }

    private boolean doesNotContainInvalidInstitutionCode(CristinLocale cristinLocale) {
        return !CRISTIN_INSTITUTION_CODE.equalsIgnoreCase(cristinLocale.getOwnerCode())
               && !UNIT_INSTITUTION_CODE.equalsIgnoreCase(cristinLocale.getOwnerCode());
    }

    private boolean shouldUseOwnerCodeCreated(List<CristinLocale> cristinLocales) {
        return cristinLocales.isEmpty()
               && nonNull(cristinObject.getOwnerCodeCreated())
               && !CRISTIN_INSTITUTION_CODE.equalsIgnoreCase(cristinObject.getOwnerCodeCreated())
               && !UNIT_INSTITUTION_CODE.equalsIgnoreCase(cristinObject.getOwnerCodeCreated());
    }

    private ResourceOwner bestMatchingResourceOwner(List<CristinLocale> cristinLocales) {
        return cristinLocales
                   .stream()
                   .filter(
                       cristinLocale ->
                           cristinLocale.getOwnerCode().equalsIgnoreCase(cristinObject.getOwnerCodeCreated()))
                   .collect(SingletonCollector.collect())
                   .toResourceOwner();
    }

    private boolean cristinLocalesContainsCristinOwnerCodeCreated(List<CristinLocale> cristinLocales) {
        return nonNull(cristinObject.getOwnerCodeCreated())
               && cristinLocales
                      .stream()
                      .anyMatch(cristinLocale ->
                                    cristinLocale.getOwnerCode().equalsIgnoreCase(cristinObject.getOwnerCodeCreated()));
    }

    private List<Funding> extractFundings() {
        return Optional.ofNullable(cristinObject.getCristinGrants())
                   .map(this::mapToNvaFunding).orElse(null);
    }

    private List<Funding> mapToNvaFunding(List<CristinGrant> grants) {
        return grants.stream().map(CristinGrant::toNvaFunding)
                   .collect(Collectors.toList());
    }

    private void assertPublicationDoesNotHaveEmptyFields(Publication publication) {
        try {
            if (publication.getEntityDescription().getContributors().isEmpty()) {
                assertThat(publication,
                           doesNotHaveEmptyValuesIgnoringFields(IGNORE_CONTRIBUTOR_FIELDS_ADDITIONALLY));
            } else {
                assertThat(publication,
                           doesNotHaveEmptyValuesIgnoringFields(IGNORED_AND_POSSIBLY_EMPTY_PUBLICATION_FIELDS));
            }
        } catch (Error error) {
            String message = error.getMessage();
            throw new MissingFieldsException(message);
        }
    }

    private List<ResearchProject> extractProjects() {
        if (cristinObject.getPresentationalWork() == null) {
            return null;
        }
        return cristinObject.getPresentationalWork()
                   .stream()
                   .filter(CristinPresentationalWork::isProject)
                   .map(CristinPresentationalWork::toNvaResearchProject)
                   .collect(Collectors.toList());
    }

    private Instant extractDate() {
        return Optional.ofNullable(cristinObject.getEntryCreationDate())
                   .map(this::localDateToInstant)
                   .orElseGet(() -> extractPublishedDate(cristinObject));
    }

    private Instant extractPublishedDate(CristinObject cristinObject) {
        return Optional.ofNullable(cristinObject.getEntryPublishedDate())
                   .map(this::localDateToInstant)
                   .orElseGet(() -> convertPublishedYearInstant(cristinObject));
    }

    private Instant convertPublishedYearInstant(CristinObject cristinObject) {

        return Optional.ofNullable(cristinObject.getPublicationYear())
                   .map(this::yearToFirstDayOfYear)
                   .orElse(null);
    }

    private Instant yearToFirstDayOfYear(int year) {
        return LocalDate.of(year, Month.JANUARY, FIRST_DAY_OF_MONTH)
                   .atStartOfDay()
                   .toInstant(zoneOffset());
    }

    private Instant localDateToInstant(LocalDate localDate) {
        return localDate.atStartOfDay().toInstant(zoneOffset());
    }

    private Instant extractEntryLastModifiedDate() {
        return Optional.ofNullable(cristinObject.getEntryLastModifiedDate())
                   .map(ld -> ld.atStartOfDay().toInstant(zoneOffset()))
                   .orElseGet(this::extractDate);
    }

    private EntityDescription generateEntityDescription() {
        return new EntityDescription.Builder()
                   .withLanguage(extractLanguage())
                   .withMainTitle(extractMainTitle())
                   .withPublicationDate(extractPublicationDate())
                   .withReference(new ReferenceBuilder(cristinObject, channelRegistryMapper).buildReference())
                   .withContributors(extractContributors())
                   .withNpiSubjectHeading(extractNpiSubjectHeading())
                   .withAbstract(extractAbstract())
                   .withTags(extractTags())
                   .withAlternativeAbstracts(Collections.emptyMap())
                   .withDescription(extractDescription())
                   .build();
    }

    private String extractDescription() {
        var artisticDescription = getArtisticDescription();
        var exhibitionDescription = getMuseumExhibitDescription();
        return
            Stream.of(artisticDescription, exhibitionDescription)
                .flatMap(Optional::stream)
                .reduce(String::concat)
                .orElse(null);
    }

    private Optional<String> getArtisticDescription() {
        return Optional.ofNullable(cristinObject.getCristinArtisticProduction())
                   .map(CristinArtisticProduction::getDescription);
    }

    private Optional<String> getMuseumExhibitDescription() {
        return Optional.ofNullable(cristinObject.getCristinExhibition()).map(CristinExhibition::getDescription);
    }

    private List<Contributor> extractContributors() {
        var contributors = Optional.ofNullable(cristinObject.getContributors());
        return contributors.isPresent()
                   ? contributorsWithUpdatedSequenceNumbers(contributors.get())
                   : Collections.emptyList();
    }

    private List<Contributor> contributorsWithUpdatedSequenceNumbers(List<CristinContributor> cristinContributors) {
        var sortedContributors = sortCristinContributors(cristinContributors);
        addContributorNumberIfMissing(sortedContributors);
        return cristinContributors.stream()
                   .map(attempt(CristinContributor::toNvaContributor))
                   .map(Try::orElseThrow)
                   .collect(Collectors.toList());
    }

    private List<URI> generateNvaHrcsCategoriesAndActivities() {
        if (isNull(extractCristinHrcsCategoriesAndActivities())) {
            return null;
        }
        List<URI> listOfCategoriesAndActivities = new ArrayList<>();
        listOfCategoriesAndActivities.addAll(extractHrcsCategories());
        listOfCategoriesAndActivities.addAll(extractHrcsActivities());
        return listOfCategoriesAndActivities;
    }

    private List<CristinHrcsCategoriesAndActivities> extractCristinHrcsCategoriesAndActivities() {
        return cristinObject.getHrcsCategoriesAndActivities();
    }

    private List<URI> extractHrcsCategories() {
        return extractCristinHrcsCategoriesAndActivities()
                   .stream()
                   .map(CristinHrcsCategoriesAndActivities::getCategory)
                   .filter(CristinHrcsCategoriesAndActivities::validateCategory)
                   .map(categoryId -> HRCS_CATEGORY_URI + HRCS_CATEGORIES_MAP.get(categoryId).toLowerCase(Locale.ROOT))
                   .map(URI::create)
                   .collect(Collectors.toList());
    }

    private Collection<URI> extractHrcsActivities() {
        return extractCristinHrcsCategoriesAndActivities()
                   .stream()
                   .map(CristinHrcsCategoriesAndActivities::getActivity)
                   .filter(CristinHrcsCategoriesAndActivities::validateActivity)
                   .map(activityId -> HRCS_ACTIVITY_URI + HRCS_ACTIVITIES_MAP.get(activityId).toLowerCase(Locale.ROOT))
                   .map(URI::create)
                   .collect(Collectors.toList());
    }

    private PublicationDate extractPublicationDate() {
        return new PublicationDate.Builder().withYear(cristinObject.getPublicationYear().toString()).build();
    }

    private CristinTitle extractCristinMainTitle() {
        var cristinTitles = cristinObject.getCristinTitles();
        return cristinTitles.stream().filter(CristinTitle::isMainTitle)
                   .findFirst()
                   .orElseGet(() -> cristinTitles.stream().collect(SingletonCollector.collect()));
    }

    private String extractMainTitle() {
        return extractCristinMainTitle().getTitle();
    }

    private URI extractLanguage() {
        var cristinMainTitle = extractCristinMainTitle();
        return nonNull(cristinMainTitle) && nonNull(cristinMainTitle.getLanguagecode())
                   ? LanguageMapper.toUri(cristinMainTitle.getLanguagecode())
                   : LanguageMapper.LEXVO_URI_UNDEFINED;
    }

    private Set<AdditionalIdentifier> extractAdditionalIdentifiers() {
        var cristinId = new AdditionalIdentifier(CristinObject.IDENTIFIER_ORIGIN, cristinObject.getId().toString());
        var additionalIdentifiers = extractCristinSourceids(cristinObject);
        additionalIdentifiers.add(cristinId);
        if (nonNull(cristinObject.getSourceCode())
            && sourceCodeHasNotBeenMappedAlready(additionalIdentifiers, cristinObject.getSourceCode())) {
            additionalIdentifiers.add(extractAdditionalIdentifierFromSourceCode());
        }
        return additionalIdentifiers;
    }

    private AdditionalIdentifier extractAdditionalIdentifierFromSourceCode() {
        return new AdditionalIdentifier(cristinObject.getSourceCode(), cristinObject.getSourceRecordIdentifier());
    }

    private boolean sourceCodeHasNotBeenMappedAlready(Set<AdditionalIdentifier> additionalIdentifiers,
                                                      String sourceCode) {
        return additionalIdentifiers
                   .stream()
                   .noneMatch(additionalIdentifier -> hasIdenticalSourceCode(sourceCode, additionalIdentifier));
    }

    private boolean hasIdenticalSourceCode(String sourceCode, AdditionalIdentifier additionalIdentifier) {
        return additionalIdentifier.getSourceName().equals(sourceCode);
    }

    private Set<AdditionalIdentifier> extractCristinSourceids(CristinObject cristinObject) {
        if (isNull(cristinObject.getCristinSources())) {
            return new HashSet<>();
        }
        return cristinObject.getCristinSources()
                   .stream()
                   .map(this::mapCristinSourceToAdditionalIdentifier)
                   .collect(
                       Collectors.toSet());
    }

    private AdditionalIdentifier mapCristinSourceToAdditionalIdentifier(CristinSource cristinSource) {
        return new AdditionalIdentifier(craftSourceCode(cristinSource), cristinSource.getSourceIdentifier());
    }

    private String extractNpiSubjectHeading() {
        CristinSubjectField subjectField = extractSubjectField();
        if (isNull(subjectField)) {
            return EMPTY_STRING;
        } else {
            return extractSubjectFieldCode(subjectField);
        }
    }

    private String extractSubjectFieldCode(CristinSubjectField subjectField) {
        return Optional.ofNullable(subjectField.getSubjectFieldCode())
                   .map(String::valueOf)
                   .orElseThrow(() -> new MissingFieldsException(CristinSubjectField.MISSING_SUBJECT_FIELD_CODE));
    }

    private boolean resourceTypeIsNotExpectedToHaveAnNpiSubjectHeading() {
        return !(isBook(cristinObject) || isReport(cristinObject));
    }

    private CristinSubjectField extractSubjectField() {
        if (resourceTypeIsNotExpectedToHaveAnNpiSubjectHeading()) {
            return null;
        }
        return extractCristinBookReport().getSubjectField();
    }

    private List<CristinTags> extractCristinTags() {
        return Optional.ofNullable(cristinObject)
                   .map(CristinObject::getTags)
                   .orElse(null);
    }

    private String extractAbstract() {
        var cristinMainTitle = extractCristinMainTitle();
        return nonNull(cristinMainTitle) ? cristinMainTitle.getAbstractText() : null;
    }

    private List<String> extractTags() {
        if (extractCristinTags() == null) {
            return null;
        }
        List<String> listOfTags = new ArrayList<>();
        for (CristinTags cristinTags : extractCristinTags()) {
            if (cristinTags.getBokmal() != null) {
                listOfTags.add(cristinTags.getBokmal());
            }
            if (cristinTags.getEnglish() != null) {
                listOfTags.add(cristinTags.getEnglish());
            }
            if (cristinTags.getNynorsk() != null) {
                listOfTags.add(cristinTags.getNynorsk());
            }
        }
        return listOfTags;
    }
}
