package no.sikt.nva.brage.migration.testutils;

import static no.unit.nva.testutils.RandomDataGenerator.randomBoolean;
import static no.unit.nva.testutils.RandomDataGenerator.randomDoi;
import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomIssn;
import static no.unit.nva.testutils.RandomDataGenerator.randomLocalDateTime;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import java.net.URI;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import no.sikt.nva.brage.migration.model.Record;
import no.sikt.nva.brage.migration.model.Type;
import no.sikt.nva.brage.migration.model.content.ContentFile;
import no.sikt.nva.brage.migration.model.content.ResourceContent;
import no.sikt.nva.brage.migration.model.content.ResourceContent.BundleType;
import no.sikt.nva.brage.migration.model.entitydescription.Contributor;
import no.sikt.nva.brage.migration.model.entitydescription.Identity;
import no.sikt.nva.brage.migration.model.entitydescription.Language;
import no.sikt.nva.brage.migration.model.entitydescription.PublicationDate;
import no.sikt.nva.brage.migration.model.entitydescription.PublicationInstance;
import no.sikt.nva.brage.migration.model.entitydescription.PublishedDate;
import no.sikt.nva.brage.migration.model.license.License;
import no.sikt.nva.brage.migration.model.license.NvaLicense;
import no.sikt.nva.brage.migration.model.license.NvaLicenseIdentifier;
import no.sikt.nva.brage.migration.testutils.type.BrageType;
import no.sikt.nva.brage.migration.testutils.type.TypeMapper;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.ResourceOwner;
import nva.commons.core.language.LanguageMapper;
import nva.commons.core.paths.UriWrapper;

public class NvaBrageMigrationDataGenerator {

    private final Record brageRecord;
    private final Publication correspondingNvaPublication;

    public NvaBrageMigrationDataGenerator(Builder builder) {
        brageRecord = createBrageRecordWithoutCristinId(builder);
        correspondingNvaPublication = createCorrespondingNvaPublication(builder);
    }

    public Record getBrageRecord() {
        return brageRecord;
    }

    public Publication getCorrespondingNvaPublication() {
        return correspondingNvaPublication;
    }

    public PublishedDate createRandomPublishedDate() {
        var date = randomLocalDateTime().toString();
        return new PublishedDate(List.of(date), date);
    }

    private Publication createCorrespondingNvaPublication(Builder builder) {
        var publication = new Publication.Builder()
                              .withDoi(builder.getDoi())
                              .withHandle(builder.getHandle())
                              .withEntityDescription(createEntityDescription(builder))
                              .withPublisher(createPublisher(builder))
                              .build();
        return publication;
    }

    private Organization createPublisher(Builder builder) {
        var organization = new Organization();
        organization.setId(builder.getCustomerUri());
        return organization;
    }

    private EntityDescription createEntityDescription(Builder builder) {
        return new EntityDescription.Builder()
                   .withLanguage(builder.getLanguage().getNva())
                   .build();
    }

    private ResourceOwner createResourceOwner(Builder builder) {
        return new ResourceOwner(null, builder.getCustomerUri());
    }

    private Record createBrageRecordWithoutCristinId(Builder builder) {
        var record = new Record();
        record.setCustomerId(builder.getCustomerUri());
        record.setDoi(builder.getDoi());
        record.setId(builder.getHandle());
        record.setEntityDescription(createBrageEntityDescription(builder));
        record.setType(createRandomSingleType());
        record.setBrageLocation(createRandomBrageLocation());
        record.setContentBundle(createSingleResourceContent());
        record.setPublishedDate(createRandomPublishedDate());
        record.setPublication(createRandomPublication());
        return record;
    }

    private no.sikt.nva.brage.migration.model.Publication createRandomPublication() {
        var publication = new no.sikt.nva.brage.migration.model.Publication();
        publication.setIssn(randomIssn());
        publication.setPublisher(randomString());
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

    private Contributor createRandomContributor() {
        var randomRole = createRandomRole();
        return new Contributor(Builder.DEFAULT_CONTRIBUTOR_TYPE, createRandomIdentity(), randomRole,
                               createCorrespondingBrageRole(randomRole));
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
        var listOfSomePossibleRoles = List.of("Author", "Advisor", "Editor");
        return listOfSomePossibleRoles.get(randomInteger(2));
    }

    private no.sikt.nva.brage.migration.model.entitydescription.EntityDescription createBrageEntityDescription(
        Builder builder) {
        var entityDescription = new no.sikt.nva.brage.migration.model.entitydescription.EntityDescription();
        entityDescription.setLanguage(builder.getLanguage());
        entityDescription.setDescriptions(List.of(randomString()));
        entityDescription.setAbstracts(List.of(randomString()));
        entityDescription.setContributors(List.of(createRandomContributor()));
        entityDescription.setMainTitle(randomString());
        entityDescription.setAlternativeTitles(List.of(randomString()));
        entityDescription.setTags(List.of(randomString()));
        entityDescription.setPublicationInstance(createRandomPublicationInstance());
        String date = randomLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy"));
        entityDescription.setPublicationDate(new PublicationDate(date, date));
        return entityDescription;
    }

    private PublicationInstance createRandomPublicationInstance() {
        var publicationInstance = new PublicationInstance();
        publicationInstance.setVolume(randomInteger().toString());
        publicationInstance.setIssue(randomInteger().toString());
        return publicationInstance;
    }

    public static class Builder {

        public static final String DEFAULT_CONTRIBUTOR_TYPE = "Contributor";
        private URI handle;
        private URI doi;
        private String brageLocation;
        private List<ContentFile> contentFiles;
        private URI customerUri;
        private Language language;
        private Contributor contributor;
        private Type type;
        private ResourceContent resourceContent;
        private PublishedDate publishedDate;
        private EntityDescription entityDescription;
        private no.sikt.nva.brage.migration.model.Publication publication;

        public no.sikt.nva.brage.migration.model.Publication getPublication() {
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

        public URI getCustomerUri() {
            return customerUri;
        }

        public Builder withPublication(no.sikt.nva.brage.migration.model.Publication publication) {
            this.publication = publication;
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

        public Builder withHandle(URI handle) {
            this.handle = handle;
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

        public Builder withContributor() {
            this.contributor = contributor;
            return this;
        }

        public Builder withType() {
            this.type = type;
            return this;
        }

        public NvaBrageMigrationDataGenerator build() {
            if (Objects.isNull(handle)) {
                handle = randomHandle();
            }
            if (Objects.isNull(doi) && randomBoolean()) {
                doi = randomDoi();
            }
            if (Objects.isNull(customerUri)) {
                customerUri = randomUri();
            }
            if (Objects.isNull(language)) {
                language = randomLanguage1();
            }
            return new NvaBrageMigrationDataGenerator(this);
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
