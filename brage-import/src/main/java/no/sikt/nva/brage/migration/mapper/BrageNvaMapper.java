package no.sikt.nva.brage.migration.mapper;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.sikt.nva.brage.migration.lambda.BrageEntryEventConsumer.SOURCE_CRISTIN;
import static no.sikt.nva.brage.migration.mapper.PublicationContextMapper.HTTPS_PREFIX;
import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValuesIgnoringFields;
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
import no.sikt.nva.brage.migration.lambda.MissingFieldsException;
import no.sikt.nva.brage.migration.record.Affiliation;
import no.sikt.nva.brage.migration.record.Customer;
import no.sikt.nva.brage.migration.record.Language;
import no.sikt.nva.brage.migration.record.Project;
import no.sikt.nva.brage.migration.record.PublisherAuthority;
import no.sikt.nva.brage.migration.record.Record;
import no.sikt.nva.brage.migration.record.content.ContentFile;
import no.sikt.nva.brage.migration.record.content.ResourceContent;
import no.sikt.nva.brage.migration.record.content.ResourceContent.BundleType;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.Corporation;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.PublicationDate.Builder;
import no.unit.nva.model.Reference;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.Username;
import no.unit.nva.model.associatedartifacts.AssociatedArtifact;
import no.unit.nva.model.associatedartifacts.AssociatedLink;
import no.unit.nva.model.associatedartifacts.file.AdministrativeAgreement;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.associatedartifacts.file.PublisherVersion;
import no.unit.nva.model.associatedartifacts.file.UploadDetails;
import no.unit.nva.model.exceptions.InvalidIsbnException;
import no.unit.nva.model.exceptions.InvalidIssnException;
import no.unit.nva.model.exceptions.InvalidUnconfirmedSeriesException;
import no.unit.nva.model.funding.Funding;
import no.unit.nva.model.role.Role;
import no.unit.nva.model.role.RoleType;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.StringUtils;
import nva.commons.core.paths.UriWrapper;
import org.apache.tika.langdetect.optimaize.OptimaizeLangDetector;

@SuppressWarnings("PMD.GodClass")
public final class BrageNvaMapper {

    public static final String CRISTIN = "cristin";
    public static final String PERSON = "person";
    public static final String BASE_PATH = new Environment().readEnv("DOMAIN_NAME");
    public static final String ORGANIZATION = "organization";
    private static final List<String> LEGAL_NOTES_WITH_EMBARGO = List.of(
        "Dette dokumentet er ikke elektronisk tilgjengelig etter ønske fra forfatter",
        "Kun forskere og studenter kan få innsyn i dokumentet",
        "Dokumentet er klausulert grunnet lovpålagt taushetsplikt",
        "Klausulert: Kan bare siteres etter nærmere avtale med forfatter",
        "Klausulert: Kan bare tillates lest etter nærmere avtale med forfatter");
    public static final int HUNDRED_YEARS = 36_524;



    private BrageNvaMapper() {

    }

    public static Publication toNvaPublication(Record brageRecord)
        throws InvalidIssnException, InvalidIsbnException, InvalidUnconfirmedSeriesException {
        var now = Instant.now();
        var publication = new Publication.Builder().withHandle(extractHandle(brageRecord))
                              .withEntityDescription(extractEntityDescription(brageRecord))
                              .withCreatedDate(now)
                              .withModifiedDate(now)
                              .withPublishedDate(now)
                              .withPublisher(extractPublisher(brageRecord))
                              .withAssociatedArtifacts(extractAssociatedArtifacts(brageRecord))
                              .withResourceOwner(extractResourceOwner(brageRecord))
                              .withAdditionalIdentifiers(extractCristinIdentifier(brageRecord))
                              .withRightsHolder(brageRecord.getRightsholder())
                              .withSubjects(extractSubjects(brageRecord))
                              .withFundings(extractFundings(brageRecord))
                              .build();
        if (!isCristinRecord(brageRecord)) {
            assertPublicationDoesNotHaveEmptyFields(publication);
        }
        return publication;
    }

    private static List<Funding> extractFundings(Record brageRecord) {
        return nonNull(brageRecord.getProjects())
                   ? brageRecord.getProjects().stream().map(Project::toFunding).collect(Collectors.toList())
                   : List.of();
    }

    public static String extractDescription(Record record) {
        return Optional.ofNullable(record.getEntityDescription().getDescriptions())
                   .map(BrageNvaMapper::filterOutEmptyValues)
                   .filter(descriptions -> !descriptions.isEmpty())
                   .map(BrageNvaMapper::joinByNewLine)
                   .orElse(null);
    }

    private static String joinByNewLine(List<String> values) {
        return values.stream().collect(Collectors.joining(System.lineSeparator()));
    }

    private static List<URI> extractSubjects(Record brageRecord) {
        return Optional.ofNullable(brageRecord)
                   .map(Record::getSubjects)
                   .map(Collection::stream)
                   .map(Stream::toList)
                   .orElse(List.of());
    }

    private static boolean isCristinRecord(Record record) {
        return NvaType.CRISTIN_RECORD.getValue().equals(record.getType().getNva());
    }

    private static List<String> filterOutEmptyValues(List<String> descriptions) {
        return descriptions.stream().filter(StringUtils::isNotBlank).toList();
    }

    private static List<AssociatedArtifact> extractAssociatedArtifacts(Record brageRecord) {
        var associatedArtifacts = new ArrayList<>(extractAssociatedFiles(brageRecord));
        associatedArtifacts.add(extractAssociatedLink(brageRecord));
        return associatedArtifacts.stream().filter(Objects::nonNull).toList();
    }

    private static AssociatedLink extractAssociatedLink(Record brageRecord) {
        return nonNull(brageRecord.getLink()) ? new AssociatedLink(brageRecord.getLink(), null, null) : null;
    }

    private static Set<AdditionalIdentifier> extractCristinIdentifier(Record brageRecord) {
        if (isNull(brageRecord.getCristinId())) {
            return Set.of();
        } else {
            return Set.of(new AdditionalIdentifier(SOURCE_CRISTIN, brageRecord.getCristinId()));
        }
    }

    private static void assertPublicationDoesNotHaveEmptyFields(Publication publication) {
        // TODO: Fix this so we don't depend on JUnit.
        try {
            Set<String> ignoredAndPossiblyEmptyPublicationFields = MappingConstants.IGNORED_AND_POSSIBLY_EMPTY_PUBLICATION_FIELDS;
            assertThat(publication, doesNotHaveEmptyValuesIgnoringFields(
                ignoredAndPossiblyEmptyPublicationFields));
        } catch (Error error) {
            String message = error.getMessage();
            throw new MissingFieldsException(message);
        }
    }

    private static ResourceOwner extractResourceOwner(Record brageRecord) {
        return Optional.ofNullable(brageRecord)
                   .map(Record::getResourceOwner)
                   .map(BrageNvaMapper::generateResourceOwner)
                   .orElse(null);
    }

    private static ResourceOwner generateResourceOwner(no.sikt.nva.brage.migration.record.ResourceOwner resourceOwner) {
        return new ResourceOwner(new Username(resourceOwner.getOwner()), resourceOwner.getOwnerAffiliation());
    }

    private static List<AssociatedArtifact> extractAssociatedFiles(Record brageRecord) {
        return Optional.ofNullable(brageRecord.getContentBundle())
                   .map(ResourceContent::getContentFiles)
                   .map(list -> convertFilesToAssociatedArtifact(list, brageRecord))
                   .orElse(Collections.emptyList());
    }

    private static List<AssociatedArtifact> convertFilesToAssociatedArtifact(List<ContentFile> files,
                                                                             Record brageRecord) {
        return files.stream().map(file -> generateFile(file, brageRecord)).toList();
    }

    private static AssociatedArtifact generateFile(ContentFile file, Record brageRecord) {
        var legalNote = extractLegalNote(brageRecord);
        var embargoDate = defineEmbargoDate(legalNote, file);
        return BundleType.ORIGINAL.equals(file.getBundleType())
                   ? createPublishedFile(file, brageRecord, embargoDate, legalNote)
                   : createAdministrativeAgreement(file, brageRecord);
    }

    private static AssociatedArtifact createAdministrativeAgreement(ContentFile file, Record brageRecord) {
        return AdministrativeAgreement.builder()
                   .withName(file.getFilename())
                   .withIdentifier(file.getIdentifier())
                   .withUploadDetails(createUploadDetails(brageRecord))
                   .withAdministrativeAgreement(true)
                   .buildUnpublishableFile();
    }

    private static UploadDetails createUploadDetails(Record brageRecord) {
        return new UploadDetails(new Username(brageRecord.getResourceOwner().getOwner()), Instant.now());
    }

    private static File createPublishedFile(ContentFile file, Record brageRecord, Instant embargoDate, String legalNote) {
        return File.builder()
                   .withName(file.getFilename())
                   .withIdentifier(file.getIdentifier())
                   .withLicense(getLicenseUri(file))
                   .withPublisherVersion(extractPublisherAuthority(brageRecord))
                   .withEmbargoDate(embargoDate)
                   .withLegalNote(legalNote)
                   .withUploadDetails(createUploadDetails(brageRecord))
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

    private static Organization extractPublisher(Record brageRecord) {
        return Optional.ofNullable(brageRecord.getCustomer())
                   .map(Customer::getId)
                   .map(BrageNvaMapper::generateOrganization)
                   .orElse(null);
    }

    private static Organization generateOrganization(URI customerUri) {
        return new Organization.Builder().withId(customerUri).build();
    }

    private static URI extractHandle(Record brageRecord) {
        return brageRecord.getId();
    }

    private static EntityDescription extractEntityDescription(Record brageRecord)
        throws InvalidIssnException, InvalidIsbnException, InvalidUnconfirmedSeriesException {
        return new EntityDescription.Builder()
                   .withLanguage(extractLanguage(brageRecord))
                   .withAbstract(extractAbstract(brageRecord))
                   .withDescription(extractDescription(brageRecord))
                   .withPublicationDate(extractDate(brageRecord))
                   .withContributors(extractContributors(brageRecord))
                   .withTags(extractTags(brageRecord))
                   .withReference(extractReference(brageRecord))
                   .withMainTitle(extractMainTitle(brageRecord))
                   .withAlternativeTitles(extractAlternativeTitles(brageRecord))
                   .build();
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
        return brageRecord.getEntityDescription().getTags();
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
        return new Identity.Builder().withName(identity.getName()).withId(generateIdentityIdentifier(identity)).build();
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

    private static String extractAbstract(Record brageRecord) {
        return Optional.ofNullable(brageRecord.getEntityDescription().getAbstracts())
                   .map(BrageNvaMapper::filterOutEmptyValues)
                   .filter(abstracts -> !abstracts.isEmpty())
                   .map(BrageNvaMapper::joinByNewLine)
                   .orElse(null);
    }
}
