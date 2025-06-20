package no.unit.nva.cristin.mapper;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.unit.nva.cristin.lambda.constants.MappingConstants.HRCS_ACTIVITIES_MAP;
import static no.unit.nva.cristin.lambda.constants.MappingConstants.HRCS_CATEGORIES_MAP;
import static no.unit.nva.cristin.lambda.constants.MappingConstants.IGNORED_AND_POSSIBLY_EMPTY_PUBLICATION_FIELDS;
import static no.unit.nva.cristin.lambda.constants.MappingConstants.IGNORE_CONTRIBUTOR_FIELDS_ADDITIONALLY;
import static no.unit.nva.cristin.lambda.constants.MappingConstants.NVA_API_DOMAIN;
import static no.unit.nva.cristin.lambda.constants.MappingConstants.PATH_CUSTOMER;
import static no.unit.nva.cristin.mapper.CristinHrcsCategoriesAndActivities.HRCS_ACTIVITY_URI;
import static no.unit.nva.cristin.mapper.CristinHrcsCategoriesAndActivities.HRCS_CATEGORY_URI;
import static no.unit.nva.cristin.mapper.CristinMainCategory.isBook;
import static no.unit.nva.cristin.mapper.CristinMainCategory.isChapter;
import static no.unit.nva.cristin.mapper.CristinMainCategory.isReport;
import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.auth.uriretriever.RawContentRetriever;
import no.unit.nva.cristin.lambda.ErrorReport;
import no.unit.nva.cristin.mapper.artisticproduction.CristinArtisticProduction;
import no.unit.nva.cristin.mapper.channelregistry.ChannelRegistryMapper;
import no.unit.nva.cristin.mapper.exhibition.CristinExhibition;
import no.unit.nva.cristin.mapper.nva.CristinMappingModule;
import no.unit.nva.cristin.mapper.nva.ReferenceBuilder;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.CuratingInstitution;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.Publication.Builder;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.PublicationNote;
import no.unit.nva.model.PublicationNoteBase;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.ResearchProject;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.additionalidentifiers.AdditionalIdentifier;
import no.unit.nva.model.additionalidentifiers.AdditionalIdentifierBase;
import no.unit.nva.model.additionalidentifiers.CristinIdentifier;
import no.unit.nva.model.additionalidentifiers.ScopusIdentifier;
import no.unit.nva.model.additionalidentifiers.SourceName;
import no.unit.nva.model.funding.Funding;
import no.unit.nva.publication.model.utils.CuratingInstitutionsUtil;
import no.unit.nva.publication.model.utils.CustomerService;
import no.unit.nva.publication.utils.CristinUnitsUtil;
import no.unit.nva.publication.utils.DoesNotHaveEmptyValues;
import nva.commons.core.Environment;
import nva.commons.core.SingletonCollector;
import nva.commons.core.StringUtils;
import nva.commons.core.attempt.Try;
import nva.commons.core.language.LanguageMapper;
import nva.commons.core.paths.UriWrapper;
import software.amazon.awssdk.services.s3.S3Client;

@SuppressWarnings({"PMD.GodClass",
    "PMD.CouplingBetweenObjects",
    "PMD.ForLoopCanBeForeach",
    "PMD.ReturnEmptyCollectionRatherThanNull"})
public class CristinMapper extends CristinMappingModule {


    public static final String CRISTIN_INSTITUTION_CODE = "CRIS";
    public static final String UNIT_INSTITUTION_CODE = "UNIT";
    public static final ResourceOwner SIKT_OWNER = new CristinLocale("SIKT", "20754", "0", "0",
                                                                     "0", null, null, null, null).toResourceOwner();
    private static final String SCOPUS_CASING_ACCEPTED_BY_FRONTEND = "Scopus";
    private static final String DOMAIN_NAME = new Environment().readEnv("DOMAIN_NAME");
    private static final Map<String, String> CUSTOMER_MAP = Map.of("api.sandbox.nva.aws.unit.no",
                                                                   "bb3d0c0c-5065-4623-9b98-5810983c2478",
                                                                   "api.dev.nva.aws.unit.no",
                                                                   "bb3d0c0c-5065-4623-9b98-5810983c2478",
                                                                   "api.test.nva.aws.unit.no",
                                                                   "0baf8fcb-b18d-4c09-88bb-956b4f659103",
                                                                   "api.e2e.nva.aws.unit.no",
                                                                   "bb3d0c0c-5065-4623-9b98-5810983c2478",
                                                                   "api.nva.unit.no",
                                                                   "22139870-8d31-4df9-bc45-14eb68287c4a");
    public static final String SCOPUS_IDENTIFIER_SOURCE_CODE_FROM_CRISTIN = "scopus";
    private final CristinUnitsUtil cristinUnitsUtil;
    private final S3Client s3Client;
    private final RawContentRetriever uriRetriever;
    private final CustomerService customerService;

    public CristinMapper(CristinObject cristinObject, CristinUnitsUtil cristinUnitsUtil, S3Client s3Client,
                         RawContentRetriever uriRetriever, CustomerService customerService) {
        super(cristinObject, ChannelRegistryMapper.getInstance(), s3Client);
        this.cristinUnitsUtil = cristinUnitsUtil;
        this.s3Client = s3Client;
        this.uriRetriever = uriRetriever;
        this.customerService = customerService;
    }

    public static ZoneOffset zoneOffset() {
        return ZoneOffset.UTC.getRules().getOffset(Instant.now());
    }

    public Publication generatePublication() {
        var entityDescription = generateEntityDescription();
        Publication publication =
            new Builder()
                .withHandle(extractHandle())
                .withAdditionalIdentifiers(extractAdditionalIdentifiers())
                .withEntityDescription(entityDescription)
                .withPublisher(extractOrganization())
                .withResourceOwner(extractResourceOwner())
                .withStatus(PublicationStatus.PUBLISHED)
                .withProjects(extractProjects())
                .withSubjects(generateNvaHrcsCategoriesAndActivities())
                .withFundings(extractFundings())
                .withPublicationNotes(extractPublicationNotes())
                .withCuratingInstitutions(extractCuratingInstitutions(entityDescription))
                .withAssociatedArtifacts(AssociatedLinkExtractor.extractAssociatedLinks(cristinObject))
                .build();
        validateDoesNotHaveEmptyFields(publication);
        return publication;
    }

    protected Set<CuratingInstitution> extractCuratingInstitutions(EntityDescription entityDescription) {
        return new CuratingInstitutionsUtil(uriRetriever, customerService).getCuratingInstitutionsCached(entityDescription,
                                                                                    cristinUnitsUtil);
    }

    private static Optional<URI> extractArchiveUri(List<CristinAssociatedUri> associatedUris) {
        return associatedUris
                   .stream()
                   .filter(CristinAssociatedUri::isArchive)
                   .findFirst() // Analysis of the cristin dataset shows that there is either 0 or 1 archive uri present
                   .filter(CristinAssociatedUri::isValidUri)
                   .map(CristinAssociatedUri::toURI);
    }

    private static void addContributorNumberIfMissing(List<CristinContributor> cristinContributors) {
        if (allContributorNumbersAreNullValues(cristinContributors)) {
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

    private static boolean allContributorNumbersAreNullValues(List<CristinContributor> cristinContributors) {
        return cristinContributors.stream().map(CristinContributor::getContributorOrder).noneMatch(Objects::nonNull);
    }

    private static List<CristinContributor> sortCristinContributors(List<CristinContributor> cristinContributors) {
        return cristinContributors.stream()
                   .sorted(Comparator.nullsLast(Comparator.naturalOrder()))
                   .toList();
    }

    private static PublicationDate convertToPublicationDate(LocalDate publishedDate) {
        return new PublicationDate
                       .Builder()
                   .withYear(String.valueOf(publishedDate.getYear()))
                   .withMonth(String.valueOf(publishedDate.getMonthValue()))
                   .withDay(String.valueOf(publishedDate.getDayOfMonth()))
                   .build();
    }

    private URI extractHandle() {
        return Optional.ofNullable(cristinObject.getCristinAssociatedUris())
                   .flatMap(CristinMapper::extractArchiveUri)
                   .map(this::updateHttpScheme)
                   .orElse(null);
    }

    private URI updateHttpScheme(URI uri) {
        return UriWrapper.fromHost(uri.getHost()).addChild(uri.getPath()).getUri();
    }

    private List<PublicationNoteBase> extractPublicationNotes() {
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
                   .map(Stream::toList)
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
                   .toList();
    }

    private void validateDoesNotHaveEmptyFields(Publication publication) {
        try {
            if (publication.getEntityDescription().getContributors().isEmpty()) {
                DoesNotHaveEmptyValues.checkForEmptyFields(publication, IGNORE_CONTRIBUTOR_FIELDS_ADDITIONALLY);
            } else {
                DoesNotHaveEmptyValues.checkForEmptyFields(publication, IGNORED_AND_POSSIBLY_EMPTY_PUBLICATION_FIELDS);
            }
        } catch (Exception error) {
            ErrorReport.exceptionName(MissingFieldsException.name())
                .withBody(error.getMessage())
                .withCristinId(cristinObject.getId())
                .persist(s3Client);
        }
    }

    private List<ResearchProject> extractProjects() {
        if (cristinObject.getPresentationalWork() == null) {
            return List.of();
        }
        return cristinObject.getPresentationalWork()
                   .stream()
                   .filter(CristinPresentationalWork::isProject)
                   .map(CristinPresentationalWork::toNvaResearchProject)
                   .toList();
    }

    private Organization extractOrganization() {
        UriWrapper customerId = UriWrapper.fromUri(NVA_API_DOMAIN).addChild(PATH_CUSTOMER, getSiktCustomerId());
        return new Organization.Builder().withId(customerId.getUri()).build();
    }

    private String getSiktCustomerId() {
        return Optional.ofNullable(CUSTOMER_MAP.get(DOMAIN_NAME)).orElseThrow();
    }

    private EntityDescription generateEntityDescription() {
        return new EntityDescription.Builder()
                   .withLanguage(extractLanguage())
                   .withMainTitle(extractMainTitle())
                   .withPublicationDate(extractPublicationDate())
                   .withReference(new ReferenceBuilder(cristinObject, channelRegistryMapper, s3Client).buildReference())
                   .withContributors(extractContributors(s3Client))
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

    private List<Contributor> extractContributors(S3Client s3Client) {
        var contributors = Optional.ofNullable(cristinObject.getContributors());
        return contributors.isPresent()
                   ? contributorsWithUpdatedSequenceNumbers(contributors.get(), cristinObject.getId(), s3Client)
                   : Collections.emptyList();
    }

    private List<Contributor> contributorsWithUpdatedSequenceNumbers(List<CristinContributor> cristinContributors,
                                                                     Integer cristinIdentifier, S3Client s3Client) {
        var sortedContributors = sortCristinContributors(cristinContributors);
        addContributorNumberIfMissing(sortedContributors);
        return cristinContributors.stream()
                   .map(cristinContributor -> attempt(() -> cristinContributor.toNvaContributor(cristinIdentifier,
                                                                                                s3Client)))
                   .map(Try::orElseThrow)
                   .toList();
    }

    private List<URI> generateNvaHrcsCategoriesAndActivities() {
        if (isNull(extractCristinHrcsCategoriesAndActivities())) {
            return List.of();
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
                   .toList();
    }

    private Collection<URI> extractHrcsActivities() {
        return extractCristinHrcsCategoriesAndActivities()
                   .stream()
                   .map(CristinHrcsCategoriesAndActivities::getActivity)
                   .filter(CristinHrcsCategoriesAndActivities::validateActivity)
                   .map(activityId -> HRCS_ACTIVITY_URI + HRCS_ACTIVITIES_MAP.get(activityId).toLowerCase(Locale.ROOT))
                   .map(URI::create)
                   .toList();
    }

    private PublicationDate extractPublicationDate() {
        return Optional.ofNullable(cristinObject.getEntryPublishedDate())
                   .map(CristinMapper::convertToPublicationDate)
                   .orElseGet(this::extractFromPublicationYear);
    }

    private PublicationDate extractFromPublicationYear() {
        return new PublicationDate.Builder()
                   .withYear(cristinObject.getPublicationYear().toString())
                   .build();
    }

    private CristinTitle extractCristinMainTitle() {
        var cristinTitles = cristinObject.getCristinTitles();
        return cristinTitles.stream().filter(CristinTitle::isMainTitle)
                   .findFirst()
                   .orElseGet(() -> cristinTitles.stream().findFirst().orElseThrow());
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

    private Set<AdditionalIdentifierBase> extractAdditionalIdentifiers() {
        var cristinId =
            new CristinIdentifier(SourceName.fromCristin(getInstanceName()), cristinObject.getId().toString());
        var additionalIdentifiers = extractCristinSourceids(cristinObject);
        additionalIdentifiers.add(cristinId);
        if (sourceCodeHasBeenMappedAlready(additionalIdentifiers)) {
            additionalIdentifiers.add(extractAdditionalIdentifierFromSourceCode());
        }
        return additionalIdentifiers;
    }

    private boolean sourceCodeHasBeenMappedAlready(Set<AdditionalIdentifierBase> additionalIdentifiers) {
        return nonNull(cristinObject.getSourceCode())
               && sourceCodeHasNotBeenMappedAlready(additionalIdentifiers, cristinObject.getSourceCode());
    }

    private String getInstanceName() {
        return extractResourceOwner().getOwner().getValue().split("@")[0];
    }

    private AdditionalIdentifier extractAdditionalIdentifierFromSourceCode() {
        return new AdditionalIdentifier(cristinObject.getSourceCode(), cristinObject.getSourceRecordIdentifier());
    }

    private boolean sourceCodeHasNotBeenMappedAlready(Set<AdditionalIdentifierBase> additionalIdentifiers,
                                                      String sourceCode) {
        return additionalIdentifiers
                   .stream()
                   .noneMatch(additionalIdentifier -> hasIdenticalSourceCode(sourceCode, additionalIdentifier));
    }

    private boolean hasIdenticalSourceCode(String sourceCode, AdditionalIdentifierBase additionalIdentifier) {
        if (scopusIdentifierThatAlreadyHasBeenMapped(sourceCode, additionalIdentifier)) {
            return true;
        } else {
            return additionalIdentifier.sourceName().toLowerCase(Locale.ROOT).equals(sourceCode.toLowerCase(Locale.ROOT));
        }
    }

    private static boolean scopusIdentifierThatAlreadyHasBeenMapped(String sourceCode,
                                                                    AdditionalIdentifierBase additionalIdentifier) {
        return SCOPUS_IDENTIFIER_SOURCE_CODE_FROM_CRISTIN.equals(sourceCode.toLowerCase(Locale.ROOT))
               && additionalIdentifier instanceof ScopusIdentifier;
    }

    private Set<AdditionalIdentifierBase> extractCristinSourceids(CristinObject cristinObject) {
        if (isNull(cristinObject.getCristinSources())) {
            return new HashSet<>();
        }
        return cristinObject.getCristinSources()
                   .stream()
                   .map(this::mapCristinSourceToAdditionalIdentifier)
                   .collect(
                       Collectors.toSet());
    }

    private AdditionalIdentifierBase mapCristinSourceToAdditionalIdentifier(CristinSource cristinSource) {
        return isScopusIdentifier(cristinSource)
                   ? extractScopusIdentifier(cristinSource)
                   : extractAdditionalIdentifier(cristinSource);
    }

    private static AdditionalIdentifier extractAdditionalIdentifier(CristinSource cristinSource) {
        return new AdditionalIdentifier(cristinSource.getSourceCode(), cristinSource.getSourceIdentifier());
    }

    private ScopusIdentifier extractScopusIdentifier(CristinSource cristinSource) {
        return new ScopusIdentifier(SourceName.fromCristin(getInstanceName()), cristinSource.getSourceIdentifier());
    }

    private static boolean isScopusIdentifier(CristinSource cristinSource) {
        return SCOPUS_CASING_ACCEPTED_BY_FRONTEND.equalsIgnoreCase(cristinSource.getSourceCode());
    }

    private String extractNpiSubjectHeading() {
        return Optional.ofNullable(extractSubjectField())
                   .map(CristinSubjectField::getSubjectFieldCode)
                   .map(String::valueOf)
                   .orElse(null);
    }

    private CristinSubjectField extractSubjectField() {
        if (isBook(cristinObject) || isReport(cristinObject)) {
            return Optional.ofNullable(extractCristinBookReport())
                       .map(CristinBookOrReportMetadata::getSubjectField)
                       .orElse(null);
        }
        if (isChapter(cristinObject)) {
            return Optional.ofNullable(cristinObject.getBookOrReportPartMetadata())
                       .map(CristinBookOrReportPartMetadata::getSubjectField)
                       .orElse(null);
        } else {
            return null;
        }
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
        var tags = extractCristinTags();
        if (isNull(tags)) {
            return List.of();
        }
        return tags.stream()
                   .flatMap(tag -> Stream.of(tag.getBokmal(), tag.getEnglish(), tag.getNynorsk()))
                   .filter(Objects::nonNull)
                   .collect(Collectors.toMap(String::trim, Function.identity(), (a, b) -> a, LinkedHashMap::new))
                   .values()
                   .stream()
                   .toList();
    }
}
