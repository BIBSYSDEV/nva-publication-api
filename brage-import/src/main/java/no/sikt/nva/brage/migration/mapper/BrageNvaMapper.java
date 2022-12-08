package no.sikt.nva.brage.migration.mapper;

import static java.util.Objects.isNull;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import no.sikt.nva.brage.migration.lambda.MappingConstants;
import no.sikt.nva.brage.migration.lambda.MissingFieldsException;
import no.sikt.nva.brage.migration.record.Customer;
import no.sikt.nva.brage.migration.record.Language;
import no.sikt.nva.brage.migration.record.PublisherAuthority;
import no.sikt.nva.brage.migration.record.Record;
import no.sikt.nva.brage.migration.record.content.ContentFile;
import no.sikt.nva.brage.migration.record.content.ResourceContent;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.PublicationDate.Builder;
import no.unit.nva.model.Reference;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.Role;
import no.unit.nva.model.associatedartifacts.AssociatedArtifact;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.associatedartifacts.file.License;
import no.unit.nva.model.exceptions.InvalidIsbnException;
import no.unit.nva.model.exceptions.InvalidIssnException;
import no.unit.nva.model.exceptions.InvalidUnconfirmedSeriesException;
import nva.commons.core.JacocoGenerated;
import org.apache.tika.langdetect.optimaize.OptimaizeLangDetector;
import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValuesIgnoringFields;
import static org.hamcrest.MatcherAssert.assertThat;
import org.joda.time.Instant;

public final class BrageNvaMapper {

    private BrageNvaMapper() {

    }

    public static Publication toNvaPublication(Record record)
        throws InvalidIssnException, InvalidIsbnException, InvalidUnconfirmedSeriesException {
        var publication = new Publication.Builder()
                              .withDoi(extractDoi(record))
                              .withHandle(extractHandle(record))
                              .withEntityDescription(extractEntityDescription(record))
                              .withCreatedDate(extractPublishedDate(record))
                              .withPublishedDate(extractPublishedDate(record))
                              .withPublisher(extractPublisher(record))
                              .withAssociatedArtifacts(extractAssociatedArtifacts(record))
                              .withResourceOwner(extractResourceOwner(record))
                              .build();
        assertPublicationDoesNotHaveEmptyFields(publication);
        return publication;
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

    public static String extractDescription(Record record) {
        return Optional.ofNullable(record.getEntityDescription().getDescriptions())
                   .map(BrageNvaMapper::generateDescription)
                   .orElse(null);
    }

    private static ResourceOwner extractResourceOwner(Record record) {
        return Optional.ofNullable(record)
                   .map(Record::getResourceOwner)
                   .map(BrageNvaMapper::generateResourceOwner)
                   .orElse(null);
    }

    private static ResourceOwner generateResourceOwner(no.sikt.nva.brage.migration.record.ResourceOwner resourceOwner) {
        return new ResourceOwner(resourceOwner.getOwner(), resourceOwner.getOwnerAffiliation());
    }

    private static List<AssociatedArtifact> extractAssociatedArtifacts(Record record) {
        return Optional.ofNullable(record.getContentBundle())
                   .map(ResourceContent::getContentFiles)
                   .map(list -> convertFilesToAssociatedArtifact(list, record))
                   .orElse(null);
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
                   .withLicense(extractLicense(file))
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

    private static License extractLicense(ContentFile file) {
        return new License.Builder()
                   .withIdentifier(file.getLicense().getNvaLicense().getIdentifier().getValue())
                   .build();
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

    private static URI extractDoi(Record brageRecord) {
        return brageRecord.getDoi();
    }

    private static EntityDescription extractEntityDescription(Record record)
        throws InvalidIssnException, InvalidIsbnException, InvalidUnconfirmedSeriesException {
        return new EntityDescription.Builder()
                   .withLanguage(extractLanguage(record))
                   .withAbstract(extractAbstract(record))
                   .withDescription(extractDescription(record))
                   .withDate(extractDate(record))
                   .withContributors(extractContributors(record))
                   .withTags(extractTags(record))
                   .withReference(extractReference(record))
                   .withMainTitle(extractMainTitle(record))
                   .withAlternativeTitles(extractAlternativeTitles(record))
                   .build();
    }

    private static Map<String, String> extractAlternativeTitles(Record record) {
        return Optional.of(emptyIfNull(record.getEntityDescription().getAlternativeTitles()).stream()
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
                   .build();
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
                   .withRole(Role.lookup(contributor.getRole()))
                   .withIdentity(extractIdentity(contributor))
                   .build();
    }

    private static Identity extractIdentity(no.sikt.nva.brage.migration.record.Contributor contributor) {
        return Optional.ofNullable(contributor.getIdentity())
                   .map(BrageNvaMapper::generateIdentity)
                   .orElse(null);
    }

    private static Identity generateIdentity(no.sikt.nva.brage.migration.record.Identity identity) {
        return new Identity.Builder().withName(identity.getName())
                   .build();
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

    private static String generateDescription(List<String> descriptions) {
        return isNull(descriptions) || descriptions.isEmpty() ? null : descriptions.get(0);
    }

    private static String generateAbstract(List<String> abstracts) {
        return isNull(abstracts) || abstracts.isEmpty() ? null : abstracts.get(0);
    }

    private static String extractAbstract(Record record) {
        return Optional.ofNullable(record.getEntityDescription().getAbstracts())
                   .map(BrageNvaMapper::generateAbstract)
                   .orElse(null);
    }
}
