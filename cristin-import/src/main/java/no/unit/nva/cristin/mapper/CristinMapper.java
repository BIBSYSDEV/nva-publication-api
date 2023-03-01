package no.unit.nva.cristin.mapper;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.unit.nva.cristin.lambda.constants.HardcodedValues.HARDCODED_OWNER_AFFILIATION;
import static no.unit.nva.cristin.lambda.constants.HardcodedValues.HARDCODED_SAMPLE_DOI;
import static no.unit.nva.cristin.lambda.constants.HardcodedValues.SIKT_AFFILIATION_IDENTIFIER;
import static no.unit.nva.cristin.lambda.constants.HardcodedValues.SIKT_OWNER;
import static no.unit.nva.cristin.lambda.constants.HardcodedValues.UNIT_CUSTOMER_ID;
import static no.unit.nva.cristin.lambda.constants.MappingConstants.HRCS_ACTIVITIES_MAP;
import static no.unit.nva.cristin.lambda.constants.MappingConstants.HRCS_CATEGORIES_MAP;
import static no.unit.nva.cristin.lambda.constants.MappingConstants.IGNORED_AND_POSSIBLY_EMPTY_PUBLICATION_FIELDS;
import static no.unit.nva.cristin.lambda.constants.MappingConstants.NVA_API_DOMAIN;
import static no.unit.nva.cristin.lambda.constants.MappingConstants.PATH_CUSTOMER;
import static no.unit.nva.cristin.mapper.CristinHrcsCategoriesAndActivities.HRCS_ACTIVITY_URI;
import static no.unit.nva.cristin.mapper.CristinHrcsCategoriesAndActivities.HRCS_CATEGORY_URI;
import static no.unit.nva.cristin.mapper.CristinLocale.CRISTIN;
import static no.unit.nva.cristin.mapper.CristinLocale.ORGANIZATION;
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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import no.unit.nva.cristin.lambda.constants.HardcodedValues;
import no.unit.nva.cristin.mapper.nva.CristinMappingModule;
import no.unit.nva.cristin.mapper.nva.ReferenceBuilder;
import no.unit.nva.cristin.mapper.nva.exceptions.MissingContributorsException;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.Publication.Builder;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.ResearchProject;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.funding.Funding;
import nva.commons.core.SingletonCollector;
import nva.commons.core.attempt.Try;
import nva.commons.core.language.LanguageMapper;
import nva.commons.core.paths.UriWrapper;

@SuppressWarnings({"PMD.GodClass", "PMD.CouplingBetweenObjects"})
public class CristinMapper extends CristinMappingModule {

    public static final ResourceOwner SIKT_OWNER = createSiktOwner();
    public static final String EMPTY_STRING = "";
    public static final int FIRST_DAY_OF_MONTH = 1;

    public CristinMapper(CristinObject cristinObject) {
        super(cristinObject);
    }

    public static ResourceOwner createSiktOwner() {
        return new ResourceOwner(HardcodedValues.SIKT_OWNER, UriWrapper.fromUri(NVA_API_DOMAIN)
                                                                 .addChild(CRISTIN)
                                                                 .addChild(ORGANIZATION)
                                                                 .addChild(SIKT_AFFILIATION_IDENTIFIER)
                                                                 .getUri());
    }

    public static ZoneOffset zoneOffset() {
        return ZoneOffset.UTC.getRules().getOffset(Instant.now());
    }

    public Publication generatePublication() {
        Publication publication = new Builder()
                                      .withAdditionalIdentifiers(extractAdditionalIdentifiers())
                                      .withEntityDescription(generateEntityDescription())
                                      .withCreatedDate(extractDate())
                                      .withModifiedDate(extractEntryLastModifiedDate())
                                      .withPublishedDate(extractDate())
                                      .withPublisher(extractOrganization())
                                      .withResourceOwner(extractResourceOwner())
                                      .withStatus(PublicationStatus.PUBLISHED)
                                      .withLink(HARDCODED_SAMPLE_DOI)
                                      .withProjects(extractProjects())
                                      .withSubjects(generateNvaHrcsCategoriesAndActivities())
                                      .withFundings(extractFundings())
                                      .build();
        assertPublicationDoesNotHaveEmptyFields(publication);
        return publication;
    }

    private ResourceOwner extractResourceOwner() {
        if (isNull(cristinObject.getCristinLocales()) && isNull(cristinObject.getOwnerCodeCreated())) {
            return SIKT_OWNER;
        }
        if (isNull(cristinObject.getCristinLocales())) {
            if ("CRIS".equalsIgnoreCase(cristinObject.getOwnerCodeCreated()) || "UNIT".equalsIgnoreCase(
                cristinObject.getOwnerCodeCreated())) {
                return SIKT_OWNER;
            }
            return new ResourceOwner(craftOwnerFromOwnerCodeCreated(), craftAffiliationFromOwnerCode());
        }
        if (nonNull(cristinObject.getOwnerCodeCreated())) {
            var matchingCristinLocale =
                cristinObject.getCristinLocales()
                    .stream()
                    .filter(
                        cristinLocale -> cristinLocale.getOwnerCode()
                                             .equalsIgnoreCase(cristinObject.getOwnerCodeCreated()))
                    .findFirst();
            if (matchingCristinLocale.isPresent()) {
                return matchingCristinLocale.get().toResourceOwner();
            } else {
                return cristinObject.getCristinLocales()
                           .stream()
                           .findFirst()
                           .map(CristinLocale::toResourceOwner)
                           .orElse(SIKT_OWNER);
            }
        }

        return SIKT_OWNER;
    }

    private URI craftAffiliationFromOwnerCode() {
        return UriWrapper.fromUri(NVA_API_DOMAIN)
                   .addChild(CRISTIN)
                   .addChild(ORGANIZATION)
                   .addChild(craftAffiliationIdentifier())
                   .getUri();
    }

    private String craftOwnerFromOwnerCodeCreated() {
        return cristinObject.getOwnerCodeCreated().toLowerCase(Locale.ROOT) + "@" + craftAffiliationIdentifier();
    }

    private String craftAffiliationIdentifier() {
        return cristinObject.getInstitutionIdentifierCreated()
               + "."
               + cristinObject.getDepartmentIdentifierCreated()
               + "."
               + cristinObject.getSubDepartmendIdentifierCreated()
               + "."
               + cristinObject.getGroupIdentifierCreated();
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
            assertThat(publication,
                       doesNotHaveEmptyValuesIgnoringFields(IGNORED_AND_POSSIBLY_EMPTY_PUBLICATION_FIELDS));
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

    private Organization extractOrganization() {
        UriWrapper customerId = UriWrapper.fromUri(NVA_API_DOMAIN).addChild(PATH_CUSTOMER, UNIT_CUSTOMER_ID);
        return new Organization.Builder().withId(customerId.getUri()).build();
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
                   .withDate(extractPublicationDate())
                   .withReference(new ReferenceBuilder(cristinObject).buildReference())
                   .withContributors(extractContributors())
                   .withNpiSubjectHeading(extractNpiSubjectHeading())
                   .withAbstract(extractAbstract())
                   .withTags(extractTags())
                   .build();
    }

    private List<Contributor> extractContributors() {
        if (isNull(cristinObject.getContributors())) {
            throw new MissingContributorsException();
        }
        return cristinObject.getContributors()
                   .stream()
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
        return additionalIdentifier.getSource().equals(sourceCode);
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
        return new AdditionalIdentifier(cristinSource.getSourceCode(), cristinSource.getSourceIdentifier());
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
