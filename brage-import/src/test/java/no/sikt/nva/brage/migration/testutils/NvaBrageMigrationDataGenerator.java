package no.sikt.nva.brage.migration.testutils;

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
import no.unit.nva.model.Publication;
import no.unit.nva.model.Role;
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
        var publication = new Publication.Builder()
                              .withDoi(builder.getDoi())
                              .withHandle(builder.getHandle())
                              .withEntityDescription(createEntityDescription(builder))
                              .withPublishedDate(convertPublishedDateToInstant(builder))
                              .build();
        return publication;
    }

    private EntityDescription createEntityDescription(Builder builder) {
        return new EntityDescription.Builder()
                   .withLanguage(builder.getLanguage().getNva())
                   .withContributors(List.of(createCorrespondingContributor()))
                   .withReference(ReferenceGenerator.generateReference(builder))
                   .withDescription(builder.getDescription())
                   .withAbstract(builder.getEntityAbstract())
                   .withAlternativeTitles(builder.getAlternativeTitlesMap())
                   .withMainTitle(builder.getMainTitle())
                   .build();
    }

    private Record createRecord(Builder builder) {
        var record = new Record();
        record.setCustomer(builder.getCustomer());
        record.setDoi(builder.getDoi());
        record.setId(builder.getHandle());
        record.setEntityDescription(createBrageEntityDescription(builder));
        record.setType(createRandomSingleType());
        record.setBrageLocation(createRandomBrageLocation());
        record.setContentBundle(createSingleResourceContent());
        record.setPublication(createRandomPublication());
        record.setPublishedDate(builder.getPublishedDate());
        return record;
    }

    private no.sikt.nva.brage.migration.record.Publication createRandomPublication() {
        var publication = new no.sikt.nva.brage.migration.record.Publication();
        publication.setIssn(randomIssn());
        return publication;
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

    private Type createRandomSingleType() {
        int valueToChoose = new Random().nextInt(BrageType.values().length);
        var randomBrageType = BrageType.values()[valueToChoose];
        var matchingNvaType = TypeMapper.convertBrageTypeToNvaType(List.of(randomBrageType.toString()));
        return new Type(List.of(randomBrageType.getType()), matchingNvaType);
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
        entityDescription.setDescriptions(Collections.singletonList(builder.getDescription()));
        entityDescription.setAbstracts(Collections.singletonList(builder.getEntityAbstract()));
        entityDescription.setAlternativeTitles(builder.getAlternativeTitles());
        return entityDescription;
    }

    private Contributor createContributor() {
        return new Contributor(new Identity("Ola"), "Creator", "author");
    }

    private PublicationInstance createRandomPublicationInstance() {
        var publicationInstance = new PublicationInstance();
        publicationInstance.setVolume(randomInteger().toString());
        publicationInstance.setIssue(randomInteger().toString());
        return publicationInstance;
    }

    public static class Builder {

        private URI handle;
        private URI doi;
        private String brageLocation;
        private List<ContentFile> contentFiles;
        private String customerUri;
        private Language language;
        private Contributor contributor;
        private Type type;
        private ResourceContent resourceContent;
        private PublishedDate publishedDate;
        private EntityDescription entityDescription;
        private no.sikt.nva.brage.migration.record.Publication publication;
        private String description;
        private String entityAbstract;
        private List<String> alternativeTitles;
        private Map<String, String> alternativeTitlesMap;
        private String mainTitle;

        public String getMainTitle() {
            return mainTitle;
        }

        public Map<String, String> getAlternativeTitlesMap() {
            return alternativeTitlesMap;
        }

        public List<String> getAlternativeTitles() {
            return alternativeTitles;
        }

        public String getEntityAbstract() {
            return entityAbstract;
        }

        public no.sikt.nva.brage.migration.record.Publication getPublication() {
            return publication;
        }

        public EntityDescription getEntityDescription() {
            return entityDescription;
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

        public String getDescription() {
            return description;
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

        public String getCustomer() {
            return customerUri;
        }

        public Builder withPublication(no.sikt.nva.brage.migration.record.Publication publication) {
            this.publication = publication;
            return this;
        }

        public Builder withMainTitle(String mainTitle) {
            this.mainTitle = mainTitle;
            return this;
        }

        public Builder withEntityDescription(EntityDescription entityDescription) {
            this.entityDescription = entityDescription;
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

        public Builder withDoi(URI doi) {
            this.doi = doi;
            return this;
        }

        public Builder withContentFiles(List<ContentFile> contentFiles) {
            this.contentFiles = contentFiles;
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

        public Builder withDescription() {
            this.description = description;
            return this;
        }

        public NvaBrageMigrationDataGenerator build() {
            if (Objects.isNull(handle)) {
                handle = randomHandle();
            }
            if (Objects.isNull(alternativeTitles)) {
                alternativeTitles = notRandomAlternativeTitle();
            }
            if (Objects.isNull(alternativeTitlesMap)) {
                alternativeTitlesMap = createCorrespondingMap();
            }
            if (Objects.isNull(doi) && randomBoolean()) {
                doi = randomDoi();
            }
            if (Objects.isNull(customerUri)) {
                customerUri = randomString();
            }
            if (Objects.isNull(language)) {
                language = randomLanguage1();
            }
            if (Objects.isNull(publishedDate)) {
                publishedDate = randomPublishedDate();
            }
            if (Objects.isNull(description)) {
                description = randomString();
            }
            if (Objects.isNull(entityAbstract)) {
                entityAbstract = randomString();
            }
            if(Objects.isNull(mainTitle)) {
                mainTitle = randomString();
            }
            return new NvaBrageMigrationDataGenerator(this);
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
    }
}
