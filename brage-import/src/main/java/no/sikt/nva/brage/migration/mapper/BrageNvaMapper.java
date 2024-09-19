package no.sikt.nva.brage.migration.mapper;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.sikt.nva.brage.migration.lambda.BrageEntryEventConsumer.BRAGE_MIGRATION_REPORTS_BUCKET_NAME;
import static no.sikt.nva.brage.migration.mapper.PublicationContextMapper.HTTPS_PREFIX;
import static no.sikt.nva.brage.migration.merger.CristinImportPublicationMerger.DUMMY_HANDLE_THAT_EXIST_FOR_PROCESSING_UNIS;
import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValuesIgnoringFields;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.sikt.nva.brage.migration.NvaType;
import no.sikt.nva.brage.migration.lambda.MappingConstants;
import no.sikt.nva.brage.migration.lambda.MissingFieldsError;
import no.sikt.nva.brage.migration.record.Affiliation;
import no.sikt.nva.brage.migration.record.Language;
import no.sikt.nva.brage.migration.record.Project;
import no.sikt.nva.brage.migration.record.PublisherAuthority;
import no.sikt.nva.brage.migration.record.Record;
import no.sikt.nva.brage.migration.record.content.ContentFile;
import no.sikt.nva.brage.migration.record.content.ResourceContent;
import no.sikt.nva.brage.migration.record.content.ResourceContent.BundleType;
import no.unit.nva.model.additionalidentifiers.AdditionalIdentifierBase;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.Corporation;
import no.unit.nva.model.additionalidentifiers.CristinIdentifier;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.additionalidentifiers.HandleIdentifier;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.PublicationDate.Builder;
import no.unit.nva.model.Reference;
import no.unit.nva.model.additionalidentifiers.SourceName;
import no.unit.nva.model.associatedartifacts.AssociatedArtifact;
import no.unit.nva.model.associatedartifacts.AssociatedLink;
import no.unit.nva.model.associatedartifacts.NullAssociatedArtifact;
import no.unit.nva.model.associatedartifacts.file.AdministrativeAgreement;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.associatedartifacts.file.ImportUploadDetails;
import no.unit.nva.model.associatedartifacts.file.ImportUploadDetails.Source;
import no.unit.nva.model.associatedartifacts.file.PublisherVersion;
import no.unit.nva.model.exceptions.InvalidIsbnException;
import no.unit.nva.model.exceptions.InvalidIssnException;
import no.unit.nva.model.exceptions.InvalidUnconfirmedSeriesException;
import no.unit.nva.model.funding.Funding;
import no.unit.nva.model.role.Role;
import no.unit.nva.model.role.RoleType;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.StringUtils;
import nva.commons.core.paths.UnixPath;
import nva.commons.core.paths.UriWrapper;
import org.apache.tika.langdetect.optimaize.OptimaizeLangDetector;
import software.amazon.awssdk.services.s3.S3Client;

@SuppressWarnings("PMD.GodClass")
public final class BrageNvaMapper {

    public static final String CRISTIN = "cristin";
    public static final String PERSON = "person";
    public static final String BASE_PATH = new Environment().readEnv("DOMAIN_NAME");
    public static final String ORGANIZATION = "organization";
    public static final int HUNDRED_YEARS = 36_524;
    private static final List<String> LEGAL_NOTES_WITH_EMBARGO = List.of(
        "Dette dokumentet er ikke elektronisk tilgjengelig etter ønske fra forfatter",
        "Kun forskere og studenter kan få innsyn i dokumentet",
        "Dokumentet er klausulert grunnet lovpålagt taushetsplikt",
        "Klausulert: Kan bare siteres etter nærmere avtale med forfatter",
        "Klausulert: Kan bare tillates lest etter nærmere avtale med forfatter");
    public static final String UNDEFINED_LANGUAGE = "und";
    public static final String TWO_NEWLINES = "\n\n";
    public static final String NO_ABSTRACT = null;
    public static final String ERROR_REPORT = "ERROR_REPORT";

    private BrageNvaMapper() {
    }

    public static Publication toNvaPublication(Record brageRecord, String host, S3Client s3Client)
        throws InvalidIssnException, InvalidIsbnException, InvalidUnconfirmedSeriesException {
        var customer = Customer.fromBrageArchiveName(brageRecord.getCustomer().getName());
        validateBrageRecord(brageRecord);
        var publication = new Publication.Builder()
                              .withEntityDescription(extractEntityDescription(brageRecord))
                              .withPublisher(customer.toPublisher(host))
                              .withResourceOwner(customer.toResourceOwner(host))
                              .withAssociatedArtifacts(extractAssociatedArtifacts(brageRecord, customer))
                              .withAdditionalIdentifiers(extractAdditionalIdentifiers(brageRecord))
                              .withRightsHolder(brageRecord.getRightsholder())
                              .withFundings(extractFundings(brageRecord))
                              .build();
        if (!isCristinRecord(brageRecord)) {
            assertPublicationDoesNotHaveEmptyFields(publication, brageRecord, s3Client);
        }
        return publication;
    }

    public static String extractDescription(Record record) {
        return Optional.ofNullable(record.getEntityDescription().getDescriptions())
                   .map(BrageNvaMapper::filterOutEmptyValues)
                   .filter(descriptions -> !descriptions.isEmpty())
                   .map(BrageNvaMapper::joinByNewLine)
                   .orElse(null);
    }

    private static void validateBrageRecord(Record brageRecord) {
        if (isNull(brageRecord.getId()) || StringUtils.isBlank(brageRecord.getId().toString())) {
            throw new IllegalArgumentException("Record must contain a handle");
        }
    }

    private static List<Funding> extractFundings(Record brageRecord) {
        return nonNull(brageRecord.getProjects())
                   ? brageRecord.getProjects().stream().map(Project::toFunding).collect(Collectors.toList())
                   : List.of();
    }

    private static String joinByNewLine(List<String> values) {
        return values.stream().collect(Collectors.joining(System.lineSeparator()));
    }

    private static List<AssociatedLink> extractAssociatedLinksFromSubjects(Record brageRecord) {
        return Optional.ofNullable(brageRecord)
                   .map(Record::getSubjects)
                   .stream()
                   .flatMap(Collection::stream)
                   .map(uri -> new AssociatedLink(uri, null, null))
                   .collect(Collectors.toList());
    }

    private static boolean isCristinRecord(Record record) {
        return NvaType.CRISTIN_RECORD.getValue().equals(record.getType().getNva());
    }

    private static List<String> filterOutEmptyValues(List<String> descriptions) {
        return descriptions.stream().filter(StringUtils::isNotBlank).toList();
    }

    private static List<AssociatedArtifact> extractAssociatedArtifacts(Record brageRecord, Customer customer) {
        var associatedArtifacts = new ArrayList<>(extractAssociatedFiles(brageRecord, customer));
        associatedArtifacts.add(extractAssociatedLink(brageRecord));
        associatedArtifacts.addAll(extractAssociatedLinksFromSubjects(brageRecord));
        return associatedArtifacts.stream().filter(Objects::nonNull).toList();
    }

    private static AssociatedLink extractAssociatedLink(Record brageRecord) {
        return nonNull(brageRecord.getLink()) ? new AssociatedLink(brageRecord.getLink(), null, null) : null;
    }

    private static Set<AdditionalIdentifierBase> extractAdditionalIdentifiers(Record brageRecord) {
        return Stream.of(extractCristinAdditionalIdentifier(brageRecord), extractBrageHandle(brageRecord))
                   .filter(Optional::isPresent)
                   .map(Optional::get)
                   .collect(Collectors.toSet());
    }

    private static Optional<AdditionalIdentifierBase> extractBrageHandle(Record brageRecord) {
        return isDummyHandle(brageRecord)
                   ? Optional.empty()
                   : Optional.of(handleIdentifierFromRecord(brageRecord));
    }

    private static HandleIdentifier handleIdentifierFromRecord(Record brageRecord) {
        return new HandleIdentifier(SourceName.fromBrage(brageRecord.getCustomer().getName()),
                                    brageRecord.getId());
    }

    private static boolean isDummyHandle(Record brageRecord) {
        return brageRecord.getId().toString().contains(DUMMY_HANDLE_THAT_EXIST_FOR_PROCESSING_UNIS);
    }

    private static Optional<CristinIdentifier> extractCristinAdditionalIdentifier(Record brageRecord) {
        return isNull(brageRecord.getCristinId())
                   ? Optional.empty()
                   : Optional.of(new CristinIdentifier(SourceName.fromBrage(brageRecord.getCustomer().getName()),
                                                       brageRecord.getCristinId()));
    }

    private static void assertPublicationDoesNotHaveEmptyFields(Publication publication, Record brageRecord, S3Client s3Client) {
        // TODO: Fix this so we don't depend on JUnit.
        try {
            Set<String> ignoredAndPossiblyEmptyPublicationFields =
                MappingConstants.IGNORED_AND_POSSIBLY_EMPTY_PUBLICATION_FIELDS;
            assertThat(publication, doesNotHaveEmptyValuesIgnoringFields(
                ignoredAndPossiblyEmptyPublicationFields));
        } catch (Error error) {
            persistErrorReport(brageRecord, s3Client, error);
        }
    }

    private static void persistErrorReport(Record brageRecord, S3Client s3Client, Error error) {
        var customerName = brageRecord.getCustomer().getName();
        var handlePath = brageRecord.getId().getPath();
        var location = UnixPath.fromString(ERROR_REPORT)
                           .addChild(customerName)
                           .addChild(MissingFieldsError.name())
                           .addChild(handlePath);
        var errorMessage = error.getMessage();
        var driver = new S3Driver(s3Client, new Environment().readEnv(BRAGE_MIGRATION_REPORTS_BUCKET_NAME));
        attempt(() -> driver.insertFile(location, errorMessage)).orElseThrow();
    }

    private static List<AssociatedArtifact> extractAssociatedFiles(Record brageRecord, Customer customer) {
        return Optional.ofNullable(brageRecord.getContentBundle())
                   .map(ResourceContent::getContentFiles)
                   .map(list -> convertFilesToAssociatedArtifact(list, brageRecord, customer))
                   .orElse(Collections.emptyList());
    }

    private static List<AssociatedArtifact> convertFilesToAssociatedArtifact(List<ContentFile> files,
                                                                             Record brageRecord, Customer customer) {
        return files.stream().map(file -> generateFile(file, brageRecord, customer)).toList();
    }

    private static AssociatedArtifact generateFile(ContentFile file, Record brageRecord, Customer customer) {
        var legalNote = extractLegalNote(brageRecord);
        var embargoDate = defineEmbargoDate(legalNote, file);
        return switch (file.getBundleType()) {
            case BundleType.ORIGINAL -> createPublishedFile(file, brageRecord, embargoDate, legalNote, customer);
            case BundleType.LICENSE -> createAdministrativeAgreement(file, customer);
            case BundleType.IGNORED -> createAdministrativeAgreementForDublinCore(file, customer);
            default -> new NullAssociatedArtifact();
        };
    }

    private static AssociatedArtifact createAdministrativeAgreementForDublinCore(ContentFile file,
                                                                                 Customer customer) {
        return AdministrativeAgreement.builder()
                   .withName(file.getFilename())
                   .withIdentifier(file.getIdentifier())
                   .withUploadDetails(createUploadDetails(customer))
                   .withAdministrativeAgreement(true)
                   .buildUnpublishableFile();
    }

    private static AssociatedArtifact createAdministrativeAgreement(ContentFile file,
                                                                    Customer customer) {
        return AdministrativeAgreement.builder()
                   .withName(file.getFilename())
                   .withIdentifier(file.getIdentifier())
                   .withUploadDetails(createUploadDetails(customer))
                   .withAdministrativeAgreement(true)
                   .buildUnpublishableFile();
    }

    private static ImportUploadDetails createUploadDetails(Customer customer) {
        return new ImportUploadDetails(Source.BRAGE, customer.shortName(), Instant.now());
    }

    private static File createPublishedFile(ContentFile file, Record brageRecord, Instant embargoDate,
                                            String legalNote, Customer customer) {
        return File.builder()
                   .withName(file.getFilename())
                   .withIdentifier(file.getIdentifier())
                   .withLicense(getLicenseUri(file))
                   .withPublisherVersion(extractPublisherAuthority(brageRecord))
                   .withEmbargoDate(embargoDate)
                   .withLegalNote(legalNote)
                   .withUploadDetails(createUploadDetails(customer))
                   .buildPublishedFile();
    }

    private static Instant defineEmbargoDate(String legalNote, ContentFile file) {
        if (nonNull(legalNote) && LEGAL_NOTES_WITH_EMBARGO.contains(legalNote)) {
            return Instant.now().plus(Duration.ofDays(HUNDRED_YEARS));
        } else {
            return extractEmbargoDate(file);
        }
    }

    private static String extractLegalNote(Record brageRecord) {
        return Optional.ofNullable(brageRecord).map(Record::getAccessCode).orElse(null);
    }

    private static Instant extractEmbargoDate(ContentFile file) {
        return Optional.ofNullable(file).map(ContentFile::getEmbargoDate).orElse(null);
    }

    private static PublisherVersion extractPublisherAuthority(Record brageRecord) {
        return Optional.ofNullable(brageRecord.getPublisherAuthority()).map(PublisherAuthority::getNva).orElse(null);
    }

    private static URI getLicenseUri(ContentFile file) {
        return file.getLicense().getNvaLicense().getLicense();
    }

    private static EntityDescription extractEntityDescription(Record brageRecord)
        throws InvalidIssnException, InvalidIsbnException, InvalidUnconfirmedSeriesException {
        var abstractList = extractAbstract(brageRecord);
        return new EntityDescription.Builder()
                   .withLanguage(extractLanguage(brageRecord))
                   .withAbstract(abstractList.isEmpty() ? NO_ABSTRACT : abstractList.getFirst())
                   .withAlternativeAbstracts(extractAlternativeAbstracts(abstractList))
                   .withDescription(extractDescription(brageRecord))
                   .withPublicationDate(extractDate(brageRecord))
                   .withContributors(extractContributors(brageRecord))
                   .withTags(extractTags(brageRecord))
                   .withReference(extractReference(brageRecord))
                   .withMainTitle(extractMainTitle(brageRecord))
                   .withAlternativeTitles(extractAlternativeTitles(brageRecord))
                   .build();
    }

    private static Map<String, String> extractAlternativeAbstracts(List<String> abstractList) {
        return nonNull(abstractList) && hasMoreThatOneEntry(abstractList)
                   ? abstractList.subList(1, abstractList.size()).stream()
                   .collect(Collectors.collectingAndThen(
                       Collectors.joining(TWO_NEWLINES), joined -> Map.of(UNDEFINED_LANGUAGE, joined)))
            : Map.of();
    }

    private static boolean hasMoreThatOneEntry(List<String> abstractList) {
        return abstractList.size() >= 2;
    }

    private static Map<String, String> extractAlternativeTitles(Record brageRecord) {
        return Optional.of(emptyIfNull(brageRecord.getEntityDescription().getAlternativeTitles()).stream()
                               .filter(title -> nonNull(title) && !title.isEmpty())
                               .map(BrageNvaMapper::generateLanguageMap)
                               .flatMap(i -> i.entrySet().stream())
                               .collect(Collectors.toMap(Entry::getKey, Entry::getValue, (first, second) -> first)))
                   .orElse(null);
    }

    @JacocoGenerated
    private static List<String> emptyIfNull(List<String> values) {
        return isNull(values) ? Collections.emptyList() : values;
    }

    private static Map<String, String> generateLanguageMap(String title) {
        var detector = new OptimaizeLangDetector().loadModels();
        var language = detector.detect(title).getLanguage();
        return Map.of(language, title);
    }

    private static String extractMainTitle(Record brageRecord) {
        return Optional.ofNullable(brageRecord.getEntityDescription())
                   .map(no.sikt.nva.brage.migration.record.EntityDescription::getMainTitle)
                   .orElse(null);
    }

    private static Reference extractReference(Record brageRecord)
        throws InvalidIssnException, InvalidIsbnException, InvalidUnconfirmedSeriesException {
        return new Reference.Builder()
                   .withPublishingContext(PublicationContextMapper.buildPublicationContext(brageRecord))
                   .withPublicationInstance(PublicationInstanceMapper.buildPublicationInstance(brageRecord))
                   .withDoi(extractDoi(brageRecord))
                   .build();
    }

    private static URI extractDoi(Record brageRecord) {
        return brageRecord.getDoi();
    }

    private static List<String> extractTags(Record brageRecord) {
        return brageRecord.getEntityDescription().getTags().stream().filter(StringUtils::isNotBlank).toList();
    }

    private static List<Contributor> extractContributors(Record brageRecord) {
        return Optional.ofNullable(getContributors(brageRecord)).map(BrageNvaMapper::generateContributors).orElse(null);
    }

    private static List<no.sikt.nva.brage.migration.record.Contributor> getContributors(Record brageRecord) {
        return brageRecord.getEntityDescription().getContributors();
    }

    private static List<Contributor> generateContributors(
        List<no.sikt.nva.brage.migration.record.Contributor> contributors) {
        return contributors.stream().filter(Objects::nonNull).map(BrageNvaMapper::mapContributorToNva).toList();
    }

    private static Contributor mapContributorToNva(no.sikt.nva.brage.migration.record.Contributor contributor) {
        return new Contributor.Builder().withRole(extractRole(contributor))
                   .withIdentity(extractIdentity(contributor))
                   .withAffiliations(generateAffiliations(contributor))
                   .withSequence(contributor.getSequence())
                   .build();
    }

    private static List<Corporation> generateAffiliations(no.sikt.nva.brage.migration.record.Contributor contributor) {
        return Optional.ofNullable(contributor.getAffiliations())
                   .map(BrageNvaMapper::getCristinOrganizationList)
                   .orElse(null);
    }

    private static List<Corporation> getCristinOrganizationList(List<Affiliation> affiliations) {
        return affiliations.stream().map(BrageNvaMapper::toCristinOrganization).toList();
    }

    private static Corporation toCristinOrganization(Affiliation affiliation) {
        return new Organization.Builder().withId(generateCristinOrganization(affiliation)).build();
    }

    private static URI generateCristinOrganization(Affiliation affiliation) {
        return Optional.ofNullable(affiliation)
                   .map(Affiliation::getIdentifier)
                   .map(BrageNvaMapper::constructOrganizationUri)
                   .orElse(null);
    }

    private static URI constructOrganizationUri(String id) {
        return UriWrapper.fromUri(HTTPS_PREFIX + BASE_PATH)
                   .addChild(CRISTIN)
                   .addChild(ORGANIZATION)
                   .addChild(id)
                   .getUri();
    }

    private static RoleType extractRole(no.sikt.nva.brage.migration.record.Contributor contributor) {
        return Optional.of(new RoleType(Role.parse(contributor.getRole()))).orElse(new RoleType(Role.OTHER));
    }

    private static Identity extractIdentity(no.sikt.nva.brage.migration.record.Contributor contributor) {
        return Optional.ofNullable(contributor.getIdentity()).map(BrageNvaMapper::generateIdentity).orElse(null);
    }

    private static Identity generateIdentity(no.sikt.nva.brage.migration.record.Identity identity) {
        return new Identity.Builder()
                   .withName(identity.getName())
                   .withId(generateIdentityIdentifier(identity))
                   .withOrcId(identity.getOrcId())
                   .build();
    }

    private static URI generateIdentityIdentifier(no.sikt.nva.brage.migration.record.Identity identity) {
        return Optional.ofNullable(identity)
                   .map(no.sikt.nva.brage.migration.record.Identity::getIdentifier)
                   .map(BrageNvaMapper::generateIdentityUri)
                   .orElse(null);
    }

    private static URI generateIdentityUri(String identifier) {
        return UriWrapper.fromUri(HTTPS_PREFIX + BASE_PATH)
                   .addChild(CRISTIN)
                   .addChild(PERSON)
                   .addChild(identifier)
                   .getUri();
    }

    private static PublicationDate extractDate(Record brageRecord) {
        return Optional.ofNullable(brageRecord.getEntityDescription().getPublicationDate())
                   .map(BrageNvaMapper::generatePublicationDate)
                   .orElse(null);
    }

    private static PublicationDate generatePublicationDate(
        no.sikt.nva.brage.migration.record.PublicationDate publicationDate) {
        return new Builder().withYear(publicationDate.getNva().getYear())
                   .withMonth(publicationDate.getNva().getMonth())
                   .withDay(publicationDate.getNva().getDay())
                   .build();
    }

    private static URI extractLanguage(Record brageRecord) {
        return Optional.ofNullable(brageRecord.getEntityDescription().getLanguage())
                   .map(BrageNvaMapper::generateLanguage)
                   .orElse(null);
    }

    private static URI generateLanguage(Language language) {
        return language.getNva();
    }

    private static List<String> extractAbstract(Record brageRecord) {
        return Optional.ofNullable(brageRecord.getEntityDescription().getAbstracts())
                   .map(BrageNvaMapper::filterOutEmptyValues)
                   .filter(abstracts -> !abstracts.isEmpty())
                   .orElse(List.of());
    }
}
