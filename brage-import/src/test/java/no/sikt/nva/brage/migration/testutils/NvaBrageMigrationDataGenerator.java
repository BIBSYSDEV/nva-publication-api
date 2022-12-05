package no.sikt.nva.brage.migration.testutils;

import static java.util.Objects.isNull;
import static no.unit.nva.testutils.RandomDataGenerator.randomBoolean;
import static no.unit.nva.testutils.RandomDataGenerator.randomDoi;
import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomIssn;
import static no.unit.nva.testutils.RandomDataGenerator.randomLocalDate;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import no.sikt.nva.brage.migration.record.Contributor;
import no.sikt.nva.brage.migration.record.Identity;
import no.sikt.nva.brage.migration.record.Language;
import no.sikt.nva.brage.migration.record.Pages;
import no.sikt.nva.brage.migration.record.PublicationContext;
import no.sikt.nva.brage.migration.record.PublicationDate;
import no.sikt.nva.brage.migration.record.PublicationInstance;
import no.sikt.nva.brage.migration.record.PublishedDate;
import no.sikt.nva.brage.migration.record.Record;
import no.sikt.nva.brage.migration.record.Type;
import no.sikt.nva.brage.migration.record.content.ContentFile;
import no.sikt.nva.brage.migration.record.content.ResourceContent;
import no.sikt.nva.brage.migration.record.content.ResourceContent.BundleType;
import no.sikt.nva.brage.migration.record.license.License;
import no.sikt.nva.brage.migration.record.license.NvaLicense;
import no.sikt.nva.brage.migration.record.license.NvaLicenseIdentifier;
import no.sikt.nva.brage.migration.testutils.type.BrageType;
import no.sikt.nva.brage.migration.testutils.type.TypeMapper;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.Role;
import no.unit.nva.model.pages.MonographPages;
import nva.commons.core.language.LanguageMapper;
import nva.commons.core.paths.UriWrapper;
import org.joda.time.Instant;

public class NvaBrageMigrationDataGenerator {

    private final Record brageRecord;
    private final Publication correspondingNvaPublication;

    public NvaBrageMigrationDataGenerator(Builder builder) {
        brageRecord = createRecord(builder);
        correspondingNvaPublication = createCorrespondingNvaPublication(builder);
    }

    public Record getBrageRecord() {
        return brageRecord;
    }

    public Publication getCorrespondingNvaPublication() {
        return correspondingNvaPublication;
    }

    private static java.time.Instant convertPublishedDateToInstant(Builder builder) {
        return Instant.parse((builder.publishedDate.getNvaDate())).toDate()
                   .toInstant();
    }

    private Publication createCorrespondingNvaPublication(Builder builder) {
        return new Publication.Builder()
                   .withDoi(builder.getDoi())
                   .withHandle(builder.getHandle())
                   .withEntityDescription(createEntityDescription(builder))
                   .withPublishedDate(convertPublishedDateToInstant(builder))
                   .withStatus(PublicationStatus.PUBLISHED)
                   .withPublisher(builder.getOrganization())
                   .build();
    }

    private EntityDescription createEntityDescription(Builder builder) {
        return new EntityDescription.Builder()
                   .withLanguage(builder.getLanguage().getNva())
                   .withContributors(List.of(createCorrespondingContributor()))
                   .withReference(ReferenceGenerator.generateReference(builder))
                   .withDescription(builder.getDescriptionsForPublication())
                   .withAbstract(builder.getEntityAbstractsForPublication())
                   .withAlternativeTitles(builder.getAlternativeTitlesMap())
                   .withMainTitle(builder.getMainTitle())
                   .withDate(builder.getPublicationDateForPublication())
                   .build();
    }

    private Record createRecord(Builder builder) {
        var record = new Record();
        record.setSpatialCoverage(builder.getSpatialCoverage());
        record.setCustomer(builder.getCustomer());
        record.setDoi(builder.getDoi());
        record.setId(builder.getHandle());
        record.setEntityDescription(createBrageEntityDescription(builder));
        record.setType(builder.getType());
        record.setBrageLocation(createRandomBrageLocation());
        record.setContentBundle(createSingleResourceContent());
        record.setPublication(builder.getPublication());
        record.setPublishedDate(builder.getPublishedDate());
        return record;
    }

    private ResourceContent createSingleResourceContent() {
        var fileName = randomString();
        var description = randomString();
        var license = new License(null, new NvaLicense(NvaLicenseIdentifier.DEFAULT_LICENSE));
        var contentFile = new ContentFile(fileName, BundleType.ORIGINAL, description, UUID.randomUUID(), license, null);
        return new ResourceContent(List.of(contentFile));
    }

    private String createRandomBrageLocation() {
        return randomInteger() + "/" + randomInteger(100);
    }

    private no.unit.nva.model.Contributor createCorrespondingContributor() {
        var contributor = createContributor();
        String name = contributor.getIdentity().getName();
        return new no.unit.nva.model.Contributor.Builder()
                   .withIdentity(new no.unit.nva.model.Identity.Builder().withName(name).build())
                   .withRole(Role.lookup(contributor.getRole()))
                   .build();
    }

    private String createCorrespondingBrageRole(String role) {
        return role.substring(0, 1).toLowerCase() + role.substring(1);
    }

    private Identity createRandomIdentity() {
        return new Identity(randomString());
    }

    private String createRandomRole() {
        var listOfSomePossibleRoles = List.of("Creator", "Advisor", "Editor");
        return listOfSomePossibleRoles.get(randomInteger(2));
    }

    private no.sikt.nva.brage.migration.record.EntityDescription createBrageEntityDescription(
        Builder builder) {
        var entityDescription = new no.sikt.nva.brage.migration.record.EntityDescription();
        entityDescription.setLanguage(builder.getLanguage());
        entityDescription.setMainTitle(builder.getMainTitle());
        entityDescription.setAlternativeTitles(List.of(randomString()));
        entityDescription.setContributors(List.of(createContributor()));
        entityDescription.setDescriptions(builder.getDescriptions());
        entityDescription.setAbstracts(builder.getAbstracts());
        entityDescription.setAlternativeTitles(builder.getAlternativeTitles());
        entityDescription.setPublicationDate(builder.getPublicationDate());
        entityDescription.setPublicationInstance(createPublicationInstance(builder));
        return entityDescription;
    }

    private PublicationInstance createPublicationInstance(Builder builder) {
        var publicationInstance = new PublicationInstance();
        publicationInstance.setPageNumber(builder.getPages());
        return publicationInstance;
    }

    private Contributor createContributor() {
        return new Contributor(new Identity("Ola"), "Creator", "author");
    }

    public static class Builder {

        private URI handle;
        private URI doi;
        private String brageLocation;
        private List<ContentFile> contentFiles;
        private Language language;
        private Contributor contributor;
        private Type type;
        private ResourceContent resourceContent;
        private PublishedDate publishedDate;
        private no.sikt.nva.brage.migration.record.Publication publication;
        private List<String> descriptions;
        private List<String> abstracts;
        private List<String> alternativeTitles;
        private Map<String, String> alternativeTitlesMap;
        private String mainTitle;
        private PublicationDate publicationDate;
        private String seriesNumberRecord;
        private String seriesNumberPublication;
        private no.unit.nva.model.PublicationDate publicationDateForPublication;
        private Pages pages;
        private MonographPages monographPages;
        private List<String> spatialCoverage;
        private String customer;
        private Organization organization;

        public List<String> getAbstracts() {
            return abstracts;
        }

        public Builder withAbstracts(List<String> abstracts) {
            this.abstracts = abstracts;
            return this;
        }

        public Organization getOrganization() {
            return organization;
        }

        public String getCustomer() {
            return customer;
        }

        public Builder withOrganization(Organization organization) {
            this.organization = organization;
            return this;
        }

        public Builder withCustomer(String customer) {
            this.customer = customer;
            return this;
        }

        public List<String> getSpatialCoverage() {
            return spatialCoverage;
        }

        public Pages getPages() {
            return pages;
        }

        public PublicationDate getPublicationDate() {
            return publicationDate;
        }

        public String getSeriesNumberPublication() {
            return seriesNumberPublication;
        }

        public String getSeriesNumberRecord() {
            return seriesNumberRecord;
        }

        public no.unit.nva.model.PublicationDate getPublicationDateForPublication() {
            return publicationDateForPublication;
        }

        public String getMainTitle() {
            return mainTitle;
        }

        public Map<String, String> getAlternativeTitlesMap() {
            return alternativeTitlesMap;
        }

        public List<String> getAlternativeTitles() {
            return alternativeTitles;
        }

        public no.sikt.nva.brage.migration.record.Publication getPublication() {
            return publication;
        }

        public PublishedDate getPublishedDate() {
            return publishedDate;
        }

        public ResourceContent getResourceContent() {
            return resourceContent;
        }

        public String getBrageLocation() {
            return brageLocation;
        }

        public List<String> getDescriptions() {
            return descriptions;
        }

        public String getDescriptionsForPublication() {
            return isNull(descriptions) || descriptions.isEmpty() ? null : descriptions.get(0);
        }

        public Type getType() {
            return type;
        }

        public Contributor getContributor() {
            return contributor;
        }

        public Language getLanguage() {
            return language;
        }

        public URI getHandle() {
            return handle;
        }

        public URI getDoi() {
            return doi;
        }

        public List<ContentFile> getContentFiles() {
            return contentFiles;
        }

        public MonographPages getMonographPages() {
            return monographPages;
        }

        public Builder withMonographPages(MonographPages monographPages) {
            this.monographPages = monographPages;
            return this;
        }

        public Builder withSpatialCoverage(List<String> spatialCoverage) {
            this.spatialCoverage = spatialCoverage;
            return this;
        }

        public Builder withPublication(no.sikt.nva.brage.migration.record.Publication publication) {
            this.publication = publication;
            return this;
        }

        public Builder withPublicationDateForPublication(no.unit.nva.model.PublicationDate publicationDate) {
            this.publicationDateForPublication = publicationDate;
            return this;
        }

        public Builder withMainTitle(String mainTitle) {
            this.mainTitle = mainTitle;
            return this;
        }

        public Builder withPublishedDate(PublishedDate publishedDate) {
            this.publishedDate = publishedDate;
            return this;
        }

        public Builder withAlternativeLanguages(List<String> alternativeLanguages) {
            this.alternativeTitles = alternativeLanguages;
            return this;
        }

        public Builder withPages(Pages pages) {
            this.pages = pages;
            return this;
        }

        public Builder withHandle(URI handle) {
            this.handle = handle;
            return this;
        }

        public Builder withAlternativeTitlesMap(Map<String, String> map) {
            this.alternativeTitlesMap = map;
            return this;
        }

        public Builder withResourceContent(ResourceContent resourceContent) {
            this.resourceContent = resourceContent;
            return this;
        }

        public Builder withBrageLocation(String location) {
            this.brageLocation = brageLocation;
            return this;
        }

        public Builder withSeriesNumberRecord(String seriesNumberRecord) {
            this.seriesNumberRecord = seriesNumberRecord;
            return this;
        }

        public Builder withSeriesNumberPublication(String seriesNumberPublication) {
            this.seriesNumberPublication = seriesNumberPublication;
            return this;
        }

        public Builder withDoi(URI doi) {
            this.doi = doi;
            return this;
        }

        public Builder withContentFiles(List<ContentFile> contentFiles) {
            this.contentFiles = contentFiles;
            return this;
        }

        public Builder withPublicationDate(PublicationDate publicationDate) {
            this.publicationDate = publicationDate;
            return this;
        }

        public Builder withContributor(Contributor contributor) {
            this.contributor = contributor;
            return this;
        }

        public Builder withType(Type type) {
            this.type = type;
            return this;
        }

        public Builder withDescription(List<String> descriptions) {
            this.descriptions = descriptions;
            return this;
        }

        public NvaBrageMigrationDataGenerator build() {
            if (isNull(handle)) {
                handle = randomHandle();
            }
            if (isNull(alternativeTitles)) {
                alternativeTitles = notRandomAlternativeTitle();
            }
            if (isNull(alternativeTitlesMap)) {
                alternativeTitlesMap = createCorrespondingMap();
            }
            if (isNull(doi) && randomBoolean()) {
                doi = randomDoi();
            }
            if (isNull(language)) {
                language = randomLanguage1();
            }
            if (isNull(publishedDate)) {
                publishedDate = randomPublishedDate();
            }
            if (isNull(mainTitle)) {
                mainTitle = randomString();
            }
            if (isNull(type)) {
                type = createRandomSingleType();
            }
            if (isNull(publication)) {
                publication = createRandomPublication();
            }
            if (Objects.nonNull(publicationDate)) {
                this.publicationDateForPublication =
                    new no.unit.nva.model.PublicationDate.Builder().withDay(publicationDate.getNva().getDay())
                        .withMonth(publicationDate.getNva().getMonth())
                        .withDay(publicationDate.getNva().getDay())
                        .build();
            }
            return new NvaBrageMigrationDataGenerator(this);
        }

        public String getEntityAbstractsForPublication() {
            return isNull(abstracts) || abstracts.isEmpty() ? null : abstracts.get(0);
        }

        private Map<String, String> createCorrespondingMap() {
            return Map.of("no", "Noe på norsk språk",
                          "en", "Something in English",
                          "fr", "Une chose en Francais",
                          "lv", "Labdien, mans nezināmais draugs",
                          "de", "Ein ding aus Deutsch",
                          "is", "Mér likar þessir hestar");
        }

        private List<String> notRandomAlternativeTitle() {
            return List.of("Noe på norsk språk",
                           "Something in English",
                           "One more title in English",
                           "Une chose en Francais",
                           "Labdien, mans nezināmais draugs",
                           "Ein ding aus Deutsch",
                           "Mér likar þessir hestar");
        }

        private PublishedDate randomPublishedDate() {
            var date = randomLocalDate();
            var publishedDate = new PublishedDate(Collections.singletonList(date.toString()), date.toString());
            return publishedDate;
        }

        private Language randomLanguage1() {
            var someWeirdNess = randomInteger(3);
            switch (someWeirdNess) {
                case 0:
                    return new Language(List.of("nob"),
                                        UriWrapper.fromUri(LanguageMapper.LEXVO_URI_PREFIX + "nob").getUri());
                case 1:
                    return new Language(null, UriWrapper.fromUri(LanguageMapper.LEXVO_URI_UNDEFINED).getUri());
                default:
                    return new Language(List.of("norsk", "svensk"),
                                        UriWrapper.fromUri(LanguageMapper.LEXVO_URI_UNDEFINED).getUri());
            }
        }

        private URI randomHandle() {
            return UriWrapper.fromUri("http://hdl.handle.net/11250/" + randomInteger()).getUri();
        }

        private Type createRandomSingleType() {
            int valueToChoose = new Random().nextInt(BrageType.values().length);
            var randomBrageType = BrageType.values()[valueToChoose];
            var matchingNvaType = TypeMapper.convertBrageTypeToNvaType(List.of(randomBrageType.toString()));
            return new Type(List.of(randomBrageType.getType()), matchingNvaType);
        }

        private no.sikt.nva.brage.migration.record.Publication createRandomPublication() {
            var publication = new no.sikt.nva.brage.migration.record.Publication();
            publication.setIssn(randomIssn());
            publication.setPublicationContext(new PublicationContext());
            publication.setPartOfSeries(this.seriesNumberRecord);
            return publication;
        }
    }
}
