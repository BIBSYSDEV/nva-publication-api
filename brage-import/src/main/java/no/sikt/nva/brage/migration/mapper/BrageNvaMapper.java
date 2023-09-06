package no.sikt.nva.brage.migration.mapper;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.sikt.nva.brage.migration.lambda.BrageEntryEventConsumer.SOURCE_CRISTIN;
import static no.sikt.nva.brage.migration.mapper.PublicationContextMapper.HTTPS_PREFIX;
import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValuesIgnoringFields;
import static org.hamcrest.MatcherAssert.assertThat;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import no.sikt.nva.brage.migration.NvaType;
import no.sikt.nva.brage.migration.lambda.MappingConstants;
import no.sikt.nva.brage.migration.lambda.MissingFieldsException;
import no.sikt.nva.brage.migration.record.Affiliation;
import no.sikt.nva.brage.migration.record.Customer;
import no.sikt.nva.brage.migration.record.Language;
import no.sikt.nva.brage.migration.record.PublisherAuthority;
import no.sikt.nva.brage.migration.record.Record;
import no.sikt.nva.brage.migration.record.content.ContentFile;
import no.sikt.nva.brage.migration.record.content.ResourceContent;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.Contributor;
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
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.exceptions.InvalidIsbnException;
import no.unit.nva.model.exceptions.InvalidIssnException;
import no.unit.nva.model.exceptions.InvalidUnconfirmedSeriesException;
import no.unit.nva.model.role.Role;
import no.unit.nva.model.role.RoleType;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;
import org.apache.tika.langdetect.optimaize.OptimaizeLangDetector;
import org.joda.time.Instant;

@SuppressWarnings("PMD.GodClass")
public final class BrageNvaMapper {

    public static final String CRISTIN = "cristin";
    public static final String PERSON = "person";
    public static final String BASE_PATH = new Environment().readEnv("DOMAIN_NAME");
    public static final String ORGANIZATION = "organization";

    private BrageNvaMapper() {

    }

    public static Publication toNvaPublication(Record record)
        throws InvalidIssnException, InvalidIsbnException, InvalidUnconfirmedSeriesException {
        var publication = new Publication.Builder()
                              .withHandle(extractHandle(record))
                              .withEntityDescription(extractEntityDescription(record))
                              .withCreatedDate(extractPublishedDate(record))
                              .withPublishedDate(extractPublishedDate(record))
                              .withPublisher(extractPublisher(record))
                              .withAssociatedArtifacts(extractAssociatedArtifacts(record))
                              .withResourceOwner(extractResourceOwner(record))
                              .withAdditionalIdentifiers(extractCristinIdentifier(record))
                              .withRightsHolder(record.getRightsholder())
                              .build();
        if(!isNotCristinRecord(record)) {
            assertPublicationDoesNotHaveEmptyFields(publication);
        }
        return publication;
    }

    private static boolean isNotCristinRecord(Record record) {
        return NvaType.CRISTIN_RECORD.getValue().equals(record.getType().getNva());
    }

    public static String extractDescription(Record record) {
        return Optional.ofNullable(record.getEntityDescription().getDescriptions())
                   .map(descriptions -> descriptions.isEmpty() ? null : mergeStringsByLineBreak(descriptions))
                   .orElse(null);
    }

    private static List<AssociatedArtifact> extractAssociatedArtifacts(Record record) {
        var associatedArtifacts = new ArrayList<>(extractAssociatedFiles(record));
        associatedArtifacts.add(extractAssociatedLink(record));
        return associatedArtifacts.stream()
                   .filter(Objects::nonNull)
                   .collect(Collectors.toList());
    }

    private static AssociatedLink extractAssociatedLink(Record record) {
        return nonNull(record.getLink())
                   ? new AssociatedLink(record.getLink(), null, null)
                   : null;
    }

    private static Set<AdditionalIdentifier> extractCristinIdentifier(Record record) {
        if (isNull(record.getCristinId())) {
            return Set.of();
        } else {
            return Set.of(new AdditionalIdentifier(SOURCE_CRISTIN, record.getCristinId()));
        }
    }

    private static void assertPublicationDoesNotHaveEmptyFields(Publication publication) {
        try {
            assertThat(publication,
                       doesNotHaveEmptyValuesIgnoringFields(
                           MappingConstants.IGNORED_AND_POSSIBLY_EMPTY_PUBLICATION_FIELDS));
        } catch (Error error) {
            String message = error.getMessage();
            throw new MissingFieldsException(message);
        }
    }

    private static String mergeStringsByLineBreak(List<String> list) {
        var sb = new StringBuilder();
        for (String string : list) {
            sb.append(string).append('\n');
        }
        return sb.toString();
    }

    private static ResourceOwner extractResourceOwner(Record record) {
        return Optional.ofNullable(record)
                   .map(Record::getResourceOwner)
                   .map(BrageNvaMapper::generateResourceOwner)
                   .orElse(null);
    }

    private static ResourceOwner generateResourceOwner(no.sikt.nva.brage.migration.record.ResourceOwner resourceOwner) {
        return new ResourceOwner(new Username(resourceOwner.getOwner()), resourceOwner.getOwnerAffiliation());
    }

    private static List<AssociatedArtifact> extractAssociatedFiles(Record record) {
        return Optional.ofNullable(record.getContentBundle())
                   .map(ResourceContent::getContentFiles)
                   .map(list -> convertFilesToAssociatedArtifact(list, record))
                   .orElse(Collections.emptyList());
    }

    private static List<AssociatedArtifact> convertFilesToAssociatedArtifact(List<ContentFile> files, Record record) {
        return files.stream()
                   .map(file -> generateFile(file, record))
                   .collect(Collectors.toList());
    }

    private static AssociatedArtifact generateFile(ContentFile file, Record record) {
        return File.builder()
                   .withName(file.getFilename())
                   .withIdentifier(file.getIdentifier())
                   .withLicense(getLicenseUri(file))
                   .withPublisherAuthority(extractPublisherAuthority(record))
                   .withEmbargoDate(extractEmbargoDate(file))
                   .buildPublishedFile();
    }

    private static java.time.Instant extractEmbargoDate(ContentFile file) {
        return Optional.ofNullable(file)
                   .map(ContentFile::getEmbargoDate)
                   .map(date -> Instant.parse(date).toDate().toInstant())
                   .orElse(null);
    }

    private static Boolean extractPublisherAuthority(Record record) {
        return Optional.ofNullable(record.getPublisherAuthority())
                   .map(PublisherAuthority::getNva)
                   .orElse(false);
    }

    private static URI getLicenseUri(ContentFile file) {
        return file.getLicense().getNvaLicense().getLicense();
    }

    private static Organization extractPublisher(Record record) {
        return Optional.ofNullable(record.getCustomer())
                   .map(Customer::getId)
                   .map(BrageNvaMapper::generateOrganization)
                   .orElse(null);
    }

    private static Organization generateOrganization(URI customerUri) {
        return new Organization.Builder().withId(customerUri).build();
    }

    private static java.time.Instant extractPublishedDate(Record record) {
        return Optional.ofNullable(record.getPublishedDate())
                   .map(date -> Instant.parse(record.getPublishedDate().getNvaDate()).toDate().toInstant())
                   .orElse(null);
    }

    private static URI extractHandle(Record brageRecord) {
        return brageRecord.getId();
    }

    private static EntityDescription extractEntityDescription(Record record)
        throws InvalidIssnException, InvalidIsbnException, InvalidUnconfirmedSeriesException {
        return new EntityDescription.Builder()
                   .withLanguage(extractLanguage(record))
                   .withAbstract(extractAbstract(record))
                   .withDescription(extractDescription(record))
                   .withPublicationDate(extractDate(record))
                   .withContributors(extractContributors(record))
                   .withTags(extractTags(record))
                   .withReference(extractReference(record))
                   .withMainTitle(extractMainTitle(record))
                   .withAlternativeTitles(extractAlternativeTitles(record))
                   .build();
    }

    private static Map<String, String> extractAlternativeTitles(Record record) {
        return Optional.of(emptyIfNull(record.getEntityDescription().getAlternativeTitles()).stream()
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

    private static String extractMainTitle(Record record) {
        return Optional.ofNullable(record.getEntityDescription())
                   .map(no.sikt.nva.brage.migration.record.EntityDescription::getMainTitle)
                   .orElse(null);
    }

    private static Reference extractReference(Record record)
        throws InvalidIssnException, InvalidIsbnException, InvalidUnconfirmedSeriesException {
        return new Reference.Builder()
                   .withPublishingContext(PublicationContextMapper.buildPublicationContext(record))
                   .withPublicationInstance(PublicationInstanceMapper.buildPublicationInstance(record))
                   .withDoi(extractDoi(record))
                   .build();
    }

    private static URI extractDoi(Record record) {
        return record.getDoi();
    }

    private static List<String> extractTags(Record record) {
        return record.getEntityDescription().getTags();
    }

    private static List<Contributor> extractContributors(Record record) {
        return Optional.ofNullable(getContributors(record))
                   .map(BrageNvaMapper::generateContributors)
                   .orElse(null);
    }

    private static List<no.sikt.nva.brage.migration.record.Contributor> getContributors(Record record) {
        return record.getEntityDescription().getContributors();
    }

    private static List<Contributor> generateContributors(
        List<no.sikt.nva.brage.migration.record.Contributor> contributors) {
        return contributors.stream()
                   .filter(Objects::nonNull)
                   .map(BrageNvaMapper::mapContributorToNva)
                   .collect(Collectors.toList());
    }

    private static Contributor mapContributorToNva(no.sikt.nva.brage.migration.record.Contributor contributor) {
        return new Contributor.Builder()
                   .withRole(extractRole(contributor))
                   .withIdentity(extractIdentity(contributor))
                   .withAffiliations(generateAffiliations(contributor))
                   .build();
    }

    private static List<Organization> generateAffiliations(no.sikt.nva.brage.migration.record.Contributor contributor) {
        return Optional.ofNullable(contributor.getAffiliations())
                   .map(BrageNvaMapper::getCristinOrganizationList)
                   .orElse(null);
    }

    private static List<Organization> getCristinOrganizationList(List<Affiliation> affiliations) {
        return affiliations.stream()
                   .map(BrageNvaMapper::toCristinOrganization)
                   .collect(Collectors.toList());
    }

    private static Organization toCristinOrganization(Affiliation affiliation) {
        return new Organization.Builder()
                   .withId(generateCristinOrganization(affiliation))
                   .build();
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
        return Optional.of(new RoleType(Role.parse(contributor.getRole())))
                   .orElse(new RoleType(Role.OTHER));
    }

    private static Identity extractIdentity(no.sikt.nva.brage.migration.record.Contributor contributor) {
        return Optional.ofNullable(contributor.getIdentity())
                   .map(BrageNvaMapper::generateIdentity)
                   .orElse(null);
    }

    private static Identity generateIdentity(no.sikt.nva.brage.migration.record.Identity identity) {
        return new Identity.Builder()
                   .withName(identity.getName())
                   .withId(generateIdentityIdentifier(identity))
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

    private static PublicationDate extractDate(Record record) {
        return Optional.ofNullable(record.getEntityDescription().getPublicationDate())
                   .map(BrageNvaMapper::generatePublicationDate)
                   .orElse(null);
    }

    private static PublicationDate generatePublicationDate(
        no.sikt.nva.brage.migration.record.PublicationDate publicationDate) {
        return new Builder()
                   .withYear(publicationDate.getNva().getYear())
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

    private static String extractAbstract(Record record) {
        return Optional.ofNullable(record.getEntityDescription().getAbstracts())
                   .map(abstracts -> abstracts.isEmpty() ? null : mergeStringsByLineBreak(abstracts))
                   .orElse(null);
    }
}
