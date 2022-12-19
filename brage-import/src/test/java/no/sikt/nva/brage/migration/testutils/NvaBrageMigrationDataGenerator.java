package no.sikt.nva.brage.migration.testutils;

import static java.util.Objects.isNull;
import static no.unit.nva.testutils.RandomDataGenerator.randomBoolean;
import static no.unit.nva.testutils.RandomDataGenerator.randomDoi;
import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomIsbn10;
import static no.unit.nva.testutils.RandomDataGenerator.randomIssn;
import static no.unit.nva.testutils.RandomDataGenerator.randomLocalDate;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import no.sikt.nva.brage.migration.record.Contributor;
import no.sikt.nva.brage.migration.record.Customer;
import no.sikt.nva.brage.migration.record.Identity;
import no.sikt.nva.brage.migration.record.Journal;
import no.sikt.nva.brage.migration.record.Language;
import no.sikt.nva.brage.migration.record.Pages;
import no.sikt.nva.brage.migration.record.PublicationContext;
import no.sikt.nva.brage.migration.record.PublicationDate;
import no.sikt.nva.brage.migration.record.PublicationDateNva;
import no.sikt.nva.brage.migration.record.PublicationInstance;
import no.sikt.nva.brage.migration.record.PublishedDate;
import no.sikt.nva.brage.migration.record.Publisher;
import no.sikt.nva.brage.migration.record.Range;
import no.sikt.nva.brage.migration.record.Record;
import no.sikt.nva.brage.migration.record.ResourceOwner;
import no.sikt.nva.brage.migration.record.Series;
import no.sikt.nva.brage.migration.record.Type;
import no.sikt.nva.brage.migration.record.content.ContentFile;
import no.sikt.nva.brage.migration.record.content.ResourceContent;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.Role;
import no.unit.nva.model.associatedartifacts.AssociatedArtifact;
import no.unit.nva.model.pages.MonographPages;
import nva.commons.core.language.LanguageMapper;
import nva.commons.core.paths.UriWrapper;
import org.jetbrains.annotations.NotNull;
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

    private static no.unit.nva.model.ResourceOwner getResourceOwnerIfPresent(Builder builder) {
        return Optional.ofNullable(builder)
                   .map(Builder::getResourceOwner)
                   .map(generateResourceOwner())
                   .orElse(null);
    }

    @NotNull
    private static Function<ResourceOwner, no.unit.nva.model.ResourceOwner> generateResourceOwner() {
        return resourceOwner -> new no.unit.nva.model.ResourceOwner(resourceOwner.getOwner(),
                                                                    resourceOwner.getOwnerAffiliation());
    }

    private Publication createCorrespondingNvaPublication(Builder builder) {
        return new Publication.Builder()
                   .withDoi(builder.getDoi())
                   .withHandle(builder.getHandle())
                   .withEntityDescription(createEntityDescription(builder))
                   .withCreatedDate(convertPublishedDateToInstant(builder))
                   .withPublishedDate(convertPublishedDateToInstant(builder))
                   .withStatus(PublicationStatus.PUBLISHED)
                   .withIdentifier(FakeResourceService.SORTABLE_IDENTIFIER)
                   .withPublisher(new Organization.Builder().withId(builder.getCustomer().getId()).build())
                   .withAssociatedArtifacts(builder.getAssociatedArtifacts())
                   .withResourceOwner(getResourceOwnerIfPresent(builder))
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
        record.setResourceOwner(builder.getResourceOwner());
        record.setSpatialCoverage(builder.getSpatialCoverage());
        record.setCustomer(builder.getCustomer());
        record.setDoi(builder.getDoi());
        record.setId(builder.getHandle());
        record.setEntityDescription(createBrageEntityDescription(builder));
        record.setType(builder.getType());
        record.setBrageLocation(createRandomBrageLocation());
        record.setContentBundle(builder.getResourceContent());
        record.setPublication(builder.getPublication());
        record.setPublishedDate(builder.getPublishedDate());
        return record;
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

    private no.sikt.nva.brage.migration.record.EntityDescription createBrageEntityDescription(
        Builder builder) {
        var entityDescription = new no.sikt.nva.brage.migration.record.EntityDescription();
        entityDescription.setMainTitle(builder.getMainTitle());
        entityDescription.setAlternativeTitles(builder.getAlternativeTitles());
        entityDescription.setContributors(List.of(createContributor()));
        entityDescription.setDescriptions(builder.getDescriptions());
        entityDescription.setAbstracts(builder.getAbstracts());
        entityDescription.setAlternativeTitles(builder.getAlternativeTitles());
        entityDescription.setPublicationDate(builder.getPublicationDate());
        entityDescription.setPublicationInstance(createPublicationInstance(builder));
        entityDescription.setLanguage(builder.getLanguage());
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

        public static final URI CUSTOMER_URi = URI.create("https://dev.nva.sikt.no/registration/0184ebf2c2ad"
                                                          + "-0b4cd833-2f8c-4bd6-b11b-7b9cb15e9c05/edit");
        public static final URI RESOURCE_OWNER_URI = URI.create("https://api.nva.unit.no/customer/test");
        public ResourceOwner resourceOwner;
        private URI handle;
        private boolean handleShouldBeNull;
        private URI doi;
        private List<ContentFile> contentFiles;
        private Language language;
        private Contributor contributor;
        private Type type;
        private ResourceContent resourceContent;
        private PublishedDate publishedDate;
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
        private Customer customer;
        private Organization organization;
        private List<AssociatedArtifact> associatedArtifacts;
        private SortableIdentifier identifier;
        private String publisherId;
        private String journalId;
        private String seriesId;
        private String isbn;
        private String issn;
        private String journal;
        private no.sikt.nva.brage.migration.record.Publication publication;

        public String getJournal() {
            return journal;
        }

        public Builder withJournalTitle(String journal) {
            this.journal = journal;
            return this;
        }

        public String getIssn() {
            return issn;
        }

        public Builder withIssn(String issn) {
            this.issn = issn;
            return this;
        }

        public no.sikt.nva.brage.migration.record.Publication getPublication() {
            return publication;
        }

        public Builder withPublication(no.sikt.nva.brage.migration.record.Publication publication) {
            this.publication = publication;
            return this;
        }

        public String getIsbn() {
            return isbn;
        }

        public Builder withIsbn(String isbn) {
            this.isbn = isbn;
            return this;
        }

        public String getSeriesId() {
            return seriesId;
        }

        public Builder withSeries(String seriesId) {
            this.seriesId = seriesId;
            return this;
        }

        public String getJournalId() {
            return journalId;
        }

        public Builder withJournalId(String journalId) {
            this.journalId = journalId;
            return this;
        }

        public Builder withNullHandle() {
            this.handleShouldBeNull = true;
            return this;
        }

        public String getPublisherId() {
            return publisherId;
        }

        public Builder withPublisherId(String publisherId) {
            this.publisherId = publisherId;
            return this;
        }

        public ResourceOwner getResourceOwner() {
            return resourceOwner;
        }

        public Builder withResourceOwner(ResourceOwner resourceOwner) {
            this.resourceOwner = resourceOwner;
            return this;
        }

        public Builder withIdentifier(SortableIdentifier identifier) {
            this.identifier = identifier;
            return this;
        }

        public List<AssociatedArtifact> getAssociatedArtifacts() {
            return associatedArtifacts;
        }

        public Builder withAssociatedArtifacts(List<AssociatedArtifact> associatedArtifacts) {
            this.associatedArtifacts = associatedArtifacts;
            return this;
        }

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

        public Customer getCustomer() {
            return customer;
        }

        public Builder withOrganization(Organization organization) {
            this.organization = organization;
            return this;
        }

        public Builder withCustomer(Customer customer) {
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

        public PublishedDate getPublishedDate() {
            return publishedDate;
        }

        public ResourceContent getResourceContent() {
            return resourceContent;
        }

        public List<String> getDescriptions() {
            return descriptions;
        }

        public String getDescriptionsForPublication() {
            return isNull(descriptions) || descriptions.isEmpty() ? null : mergeStringsByLineBreak(descriptions);
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

        public Builder withPages(Pages pages) {
            this.pages = pages;
            return this;
        }

        public Builder withResourceContent(ResourceContent resourceContent) {
            this.resourceContent = resourceContent;
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
            if (isNull(handle) && !handleShouldBeNull) {
                handle = randomHandle();
            }
            if (isNull(alternativeTitles)) {
                alternativeTitles = notRandomAlternativeTitle();
            }
            if (isNull(descriptions)) {
                descriptions = List.of(randomString());
            }
            if (isNull(abstracts)) {
                abstracts = List.of(randomString());
            }
            if (isNull(customer)) {
                customer = new Customer("someCustomer", CUSTOMER_URi);
            }
            if (isNull(resourceOwner)) {
                resourceOwner = new ResourceOwner("someOwner", RESOURCE_OWNER_URI);
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
            if (isNull(publicationDate) && isNull(publicationDateForPublication)) {
                this.publicationDate = createPublicationDate();
                this.publicationDateForPublication = createPublicationDateForPublication(publicationDate);
            } else if (Objects.nonNull(publicationDate) && isNull(publicationDateForPublication)) {
                this.publicationDateForPublication = createPublicationDateForPublication(publicationDate);
            }
            if (isNull(pages)) {
                pages = new Pages("46 s.", new Range("5", "10"), "5");
            }
            if (isNull(monographPages)) {
                monographPages = new MonographPages.Builder().withPages("5").build();
            }
            if (isNull(isbn)) {
                isbn = randomIsbn10();
            }
            if (isNull(issn)) {
                issn = randomIssn();
            }
            if (isNull(publication)) {
                publication = createPublication();
            }
            return new NvaBrageMigrationDataGenerator(this);
        }

        public String getEntityAbstractsForPublication() {
            return isNull(abstracts) || abstracts.isEmpty() ? null : mergeStringsByLineBreak(abstracts);
        }

        private static no.unit.nva.model.PublicationDate createPublicationDateForPublication(
            PublicationDate publicationDate) {
            return new no.unit.nva.model.PublicationDate.Builder()
                       .withYear(publicationDate.getNva().getYear())
                       .withMonth(publicationDate.getNva().getMonth())
                       .withDay(publicationDate.getNva().getDay())
                       .build();
        }

        private static String mergeStringsByLineBreak(List<String> list) {
            var sb = new StringBuilder();
            for (String string : list) {
                sb.append(string).append("\n");
            }
            return sb.toString();
        }

        private PublicationDate createPublicationDate() {
            return new PublicationDate("2020",
                                       new PublicationDateNva.Builder()
                                           .withYear("2020").build());
        }

        private PublishedDate createRandomPublishedDate() {
            var publishedDate = new PublishedDate();
            publishedDate.setBrageDates(List.of("2019"));
            publishedDate.setNvaDate("2019");
            return publishedDate;
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
            var publishedDate = new PublishedDate();
            publishedDate.setBrageDates(Collections.singletonList(date.toString()));
            publishedDate.setNvaDate(date.toString());
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

        private no.sikt.nva.brage.migration.record.Publication createPublication() {
            var publication = new no.sikt.nva.brage.migration.record.Publication();
            publication.setPublicationContext(new PublicationContext());
            publication.getPublicationContext().setPublisher(new Publisher(publisherId));
            publication.getPublicationContext().setJournal(new Journal(journalId));
            publication.getPublicationContext().setSeries(new Series(seriesId));
            publication.setPartOfSeries(seriesNumberRecord);
            publication.setIsbn(isbn);
            publication.setIssn(List.of(issn));
            publication.setJournal(journal);
            return publication;
        }
    }
}
