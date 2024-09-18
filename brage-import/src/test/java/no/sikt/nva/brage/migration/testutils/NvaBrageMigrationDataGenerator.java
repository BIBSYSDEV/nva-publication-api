package no.sikt.nva.brage.migration.testutils;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomIsbn10;
import static no.unit.nva.testutils.RandomDataGenerator.randomIssn;
import static no.unit.nva.testutils.RandomDataGenerator.randomLocalDate;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import no.sikt.nva.brage.migration.record.Affiliation;
import no.sikt.nva.brage.migration.record.Contributor;
import no.sikt.nva.brage.migration.record.Customer;
import no.sikt.nva.brage.migration.record.Identity;
import no.sikt.nva.brage.migration.record.Journal;
import no.sikt.nva.brage.migration.record.Language;
import no.sikt.nva.brage.migration.record.Pages;
import no.sikt.nva.brage.migration.record.PartOfSeries;
import no.sikt.nva.brage.migration.record.Project;
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
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.AdditionalIdentifierBase;
import no.unit.nva.model.Corporation;
import no.unit.nva.model.CristinIdentifier;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.HandleIdentifier;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.SourceName;
import no.unit.nva.model.Username;
import no.unit.nva.model.associatedartifacts.AssociatedArtifact;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import no.unit.nva.model.associatedartifacts.AssociatedLink;
import no.unit.nva.model.funding.Funding;
import no.unit.nva.model.funding.FundingBuilder;
import no.unit.nva.model.pages.MonographPages;
import no.unit.nva.model.role.Role;
import no.unit.nva.model.role.RoleType;
import nva.commons.core.language.LanguageMapper;
import nva.commons.core.paths.UriWrapper;
import org.joda.time.Instant;

public class NvaBrageMigrationDataGenerator {

    private static final URI HARDCODED_NTNU_CUSTOMER_VALUE = URI.create(
        "https://test.nva.aws.unit.no/customer/33c17ef6-864b-4267-bc9d-0cee636e247e");
    private static final URI HARDCODED_NTNU_CRISTIN_ID = URI.create(
        "https://test.nva.aws.unit.no/cristin/organization/194.0.0.0");
    private static final String HARDCODED_NTNU_USERNAME = "ntnu@194.0.0.0";
    public static final String WISEFLOW_IDENTIFIER_SOURCE_NAME = "wiseflow";
    public static final String INSPERA_IDENTIFIER_SOURCE_NAME = "inspera";
    private final Record brageRecord;
    private final Publication correspondingNvaPublication;

    public NvaBrageMigrationDataGenerator(Builder builder) {
        brageRecord = createRecord(builder);
        correspondingNvaPublication = createCorrespondingNvaPublication(builder);
    }

    public Record getBrageRecord() {
        return brageRecord;
    }

    public Publication getNvaPublication() {
        return correspondingNvaPublication;
    }

    private static java.time.Instant convertPublishedDateToInstant(Builder builder) {
        return Instant.parse((builder.publishedDate.getNvaDate())).toDate().toInstant();
    }

    private static Set<AdditionalIdentifierBase> generateAdditionalIdentifiers(Builder builder) {
        var additionalIdentifiers = new HashSet<AdditionalIdentifierBase>();
        if (nonNull(builder.getHandle())) {
            additionalIdentifiers.add(new HandleIdentifier(SourceName.fromBrage(builder.getCustomer().getName()),
                                                           builder.getHandle()));
        }
        if (nonNull(builder.getCristinIdentifier())) {
            additionalIdentifiers.add(new CristinIdentifier(SourceName.fromBrage(builder.getCustomer().getName()),
                                                            builder.getCristinIdentifier()));
        }
        if (nonNull(builder.getInsperaIdentifier())) {
            additionalIdentifiers.add(new AdditionalIdentifier(INSPERA_IDENTIFIER_SOURCE_NAME,
                                                               builder.getInsperaIdentifier()));
        }
        if (nonNull(builder.getWiseflowIdentifier())) {
            additionalIdentifiers.add(new AdditionalIdentifier(WISEFLOW_IDENTIFIER_SOURCE_NAME,
                                                               builder.getInsperaIdentifier()));
        }
        return additionalIdentifiers;
    }

    private static List<Corporation> createAffiliationList(Contributor contributor) {
        return contributor.getAffiliations().stream()
            .map(Affiliation::getIdentifier)
            .map(value -> URI.create("https://test.nva.aws.unit.no/cristin/organization/" + value))
            .map(uri -> new Organization.Builder().withId(uri).build())
            .collect(Collectors.toList());
    }

    private static no.unit.nva.model.Identity createIdentity(Contributor contributor) {
        var personUri =
            URI.create("https://test.nva.aws.unit.no/cristin/person/" + contributor.getIdentity().getIdentifier());
        return new no.unit.nva.model.Identity.Builder().withName(contributor.getIdentity().getName())
                   .withId(personUri)
                   .build();
    }

    private Publication createCorrespondingNvaPublication(Builder builder) {
        return new Publication.Builder()
                   .withEntityDescription(createEntityDescription(builder))
                   .withCreatedDate(convertPublishedDateToInstant(builder))
                   .withPublishedDate(convertPublishedDateToInstant(builder))
                   .withStatus(PublicationStatus.PUBLISHED)
                   .withPublisher(new Organization.Builder().withId(HARDCODED_NTNU_CUSTOMER_VALUE).build())
                   .withAssociatedArtifacts(builder.getAssociatedArtifacts())
                   .withResourceOwner(new no.unit.nva.model.ResourceOwner(new Username(HARDCODED_NTNU_USERNAME), HARDCODED_NTNU_CRISTIN_ID))
                   .withAdditionalIdentifiers(generateAdditionalIdentifiers(builder))
                   .withRightsHolder(builder.getRightsHolder())
                   .withSubjects(builder.subjects.stream().toList())
                   .withFundings(List.of(convertProjectToFunding(builder.getProject())))
                   .build();
    }

    private static Funding convertProjectToFunding(Project project) {
        return new FundingBuilder()
                   .withIdentifier(project.identifier())
                   .withLabels(Map.of("nb", project.name()))
                   .build();
    }

    private EntityDescription createEntityDescription(Builder builder) {
        return new EntityDescription.Builder().withLanguage(builder.getLanguage().getNva())
                   .withContributors(builder.noContributors
                                         ? List.of()
                                         : List.of(createCorrespondingContributor(builder.getContributor())))
                   .withReference(ReferenceGenerator.generateReference(builder))
                   .withDescription(builder.getDescriptionsForPublication())
                   .withAbstract(builder.getEntityAbstractsForPublication())
                   .withAlternativeTitles(builder.getAlternativeTitlesMap())
                   .withMainTitle(builder.getMainTitle())
                   .withPublicationDate(builder.getPublicationDateForPublication())
                   .withTags(List.of())
                   .build();
    }

    private Record createRecord(Builder builder) {
        var brageRecord = new Record();
        brageRecord.setSpatialCoverage(builder.getSpatialCoverage());
        brageRecord.setCustomer(builder.getCustomer());
        brageRecord.setDoi(builder.getDoi());
        brageRecord.setId(builder.getHandle());
        brageRecord.setEntityDescription(createBrageEntityDescription(builder));
        brageRecord.setType(builder.getType());
        brageRecord.setBrageLocation(createRandomBrageLocation());
        brageRecord.setContentBundle(builder.getResourceContent());
        brageRecord.setPublication(builder.getPublication());
        brageRecord.setPublishedDate(builder.getPublishedDate());
        brageRecord.setCristinId(builder.getCristinIdentifier());
        brageRecord.setWiseflowIdentifier(builder.getWiseflowIdentifier());
        brageRecord.setInsperaIdentifier(builder.getInsperaIdentifier());
        brageRecord.setRightsHolder(builder.getRightsHolder());
        brageRecord.setLink(builder.getLink());
        brageRecord.setSubjects(builder.getSubjects());
        brageRecord.setSubjectCode(builder.getSubjectCode());
        brageRecord.setPart(Optional.ofNullable(builder.getHasPart())
                                .orElse(List.of()).stream().toList());
        brageRecord.setAccessCode(builder.getAccessCode());
        brageRecord.setProjects(List.of(builder.getProject()));
        return brageRecord;
    }

    private String createRandomBrageLocation() {
        return randomInteger() + "/" + randomInteger(100);
    }

    private no.unit.nva.model.Contributor createCorrespondingContributor(Contributor contributor) {
        return new no.unit.nva.model.Contributor.Builder().withIdentity(createIdentity(contributor))
                   .withAffiliations(createAffiliationList(contributor))
                   .withRole(new RoleType(Role.parse(contributor.getRole())))
                   .build();
    }

    private no.sikt.nva.brage.migration.record.EntityDescription createBrageEntityDescription(Builder builder) {
        var entityDescription = new no.sikt.nva.brage.migration.record.EntityDescription();
        entityDescription.setMainTitle(builder.getMainTitle());
        entityDescription.setAlternativeTitles(builder.getAlternativeTitles());
        entityDescription.setContributors(builder.noContributors ? List.of() : List.of(builder.getContributor()));
        entityDescription.setDescriptions(builder.getDescriptions());
        entityDescription.setAbstracts(builder.getAbstracts());
        entityDescription.setAlternativeTitles(builder.getAlternativeTitles());
        entityDescription.setPublicationDate(builder.getPublicationDate());
        entityDescription.setPublicationInstance(createPublicationInstance(builder));
        entityDescription.setLanguage(builder.getLanguage());
        entityDescription.setTags(List.of());
        return entityDescription;
    }

    private PublicationInstance createPublicationInstance(Builder builder) {
        var publicationInstance = new PublicationInstance();
        publicationInstance.setPageNumber(builder.getPages());
        publicationInstance.setVolume(builder.volume);
        publicationInstance.setIssue(builder.issue);
        publicationInstance.setArticleNumber(builder.articleNumber);
        return publicationInstance;
    }

    public static class Builder {

        public static final URI RESOURCE_OWNER_URI = URI.create("https://api.nva.unit.no/customer/test");
        public ResourceOwner resourceOwner;
        private boolean noContributors;
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
        private PartOfSeries partOfSeries;
        private String seriesNumberPublication;
        private no.unit.nva.model.PublicationDate publicationDateForPublication;
        private Pages pages;
        private MonographPages monographPages;
        private List<String> spatialCoverage;
        private Customer customer;
        private Organization organization;
        private List<AssociatedArtifact> associatedArtifacts;
        private String publisherId;
        private String journalId;
        private String seriesId;
        private String isbn;
        private List<String> issnList;
        private String journal;
        private String seriesTitle;
        private no.sikt.nva.brage.migration.record.Publication publication;
        private String cristinIdentifier;
        private String rightsHolder;
        private URI link;
        private List<URI> subjects;
        private String volume;
        private String issue;
        private String articleNumber;
        private String subjectCode;
        private List<String> hasPart;
        private List<String> ismnList;
        private String accessCode;
        private Project project;
        private String insperaIdentifier;
        private String wiseflowIdentifier;

        private static URI randomHandle() {
            return UriWrapper.fromUri("http://hdl.handle.net/11250/" + randomInteger()).getUri();
        }

        public List<String> getHasPart() {
            return hasPart;
        }

        public String getRightsHolder() {
            return rightsHolder;
        }

        public Builder withRightsHolder(String rightsHolder) {
            this.rightsHolder = rightsHolder;
            return this;
        }

        public String getCristinIdentifier() {
            return cristinIdentifier;
        }

        public String getWiseflowIdentifier() {
            return wiseflowIdentifier;
        }

        public String getInsperaIdentifier() {
            return insperaIdentifier;
        }

        public Builder withCristinIdentifier(String cristinIdentifier) {
            this.cristinIdentifier = cristinIdentifier;
            return this;
        }

        public Builder withSubjectCode(String value) {
            this.subjectCode = value;
            return this;
        }

        public String getSeriesTitle() {
            return seriesTitle;
        }

        public Builder withSeriesTitle(String seriesTitle) {
            this.seriesTitle = seriesTitle;
            return this;
        }

        public String getJournalTitle() {
            return journal;
        }

        public Builder withJournalTitle(String journal) {
            this.journal = journal;
            return this;
        }

        public List<String> getIssnList() {
            return issnList;
        }

        public Builder withIssn(List<String> issn) {
            this.issnList = issn;
            return this;
        }

        public Builder withNoContributors(boolean noContributors) {
            this.noContributors = noContributors;
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

        public List<AssociatedArtifact> getAssociatedArtifacts() {
            var artifacts = new ArrayList<AssociatedArtifact>();
            if (nonNull(link)) {
                artifacts.add(new AssociatedLink(link, null, null));
            }
            if (nonNull(associatedArtifacts)) {
                artifacts.addAll(this.associatedArtifacts);
            }
            if (!subjects.isEmpty()) {
                subjects.stream()
                    .map(uri -> new AssociatedLink(uri, null, null))
                    .forEach(artifacts::add);
            }
            return new AssociatedArtifactList(artifacts);
        }

        public Builder withAssociatedArtifacts(List<AssociatedArtifact> associatedArtifacts) {
            this.associatedArtifacts = associatedArtifacts;
            return this;
        }

        public Builder withHasPart(List<String> hasPart) {
            this.hasPart = hasPart;
            return this;
        }

        public Builder withHandle(URI handle) {
            this.handle = handle;
            return this;
        }

        public List<String> getAbstracts() {
            return abstracts;
        }

        public Builder withAbstracts(List<String> abstracts) {
            this.abstracts = abstracts;
            return this;
        }

        public Builder withArticleNumber(String articleNumber) {
            this.articleNumber = articleNumber;
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

        public Builder withLink(URI link) {
            this.link = link;
            return this;
        }

        public Builder withAccessCode(String accessCode) {
            this.accessCode = accessCode;
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

        public PartOfSeries getSeriesNumberRecord() {
            return partOfSeries;
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
            var filteredDescription = descriptions.stream().filter(value -> !value.isBlank()).toList();
            return isNull(filteredDescription) || filteredDescription.isEmpty() ? null
                       : filteredDescription.stream().collect(Collectors.joining(System.lineSeparator()));
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

        public Builder withSeriesNumberRecord(PartOfSeries partOfSeries) {
            this.partOfSeries = partOfSeries;
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

        public Builder withVolume(String volume) {
            this.volume = volume;
            return this;
        }

        public Builder withSubjects(List<URI> subjects) {
            this.subjects = subjects;
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

        public Builder withIssue(String issue) {
            this.issue = issue;
            return this;
        }

        public Builder withInsperaIdentifier(String value) {
            this.insperaIdentifier = value;
            return this;
        }

        public Builder withWiseflowIdentifier(String value) {
            this.wiseflowIdentifier = value;
            return this;
        }

        public Builder withIsmn(List<String> ismnList) {
            this.ismnList = ismnList;
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
                customer = new Customer("ntnu", null);
            }
            if (isNull(resourceOwner)) {
                resourceOwner = new ResourceOwner("institution@someOwner", RESOURCE_OWNER_URI);
            }
            if (isNull(alternativeTitlesMap)) {
                alternativeTitlesMap = createCorrespondingMap();
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
            } else if (nonNull(publicationDate) && isNull(publicationDateForPublication)) {
                this.publicationDateForPublication = createPublicationDateForPublication(publicationDate);
            }
            if (isNull(contributor)) {
                contributor = createContributor();
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
            if (isNull(issnList)) {
                issnList = List.of(randomIssn(), randomIssn());
            }
            if (isNull(rightsHolder)) {
                rightsHolder = randomString();
            }
            if (isNull(publication)) {
                publication = createPublication();
            }
            if (isNull(subjects) || subjects.isEmpty()) {
                subjects = List.of(randomUri());
            }
            if (isNull(subjectCode)) {
                subjectCode = randomString();
            }
            if (isNull(project)) {
                project = new Project(randomString(), randomString(), null);
            }
            return new NvaBrageMigrationDataGenerator(this);
        }

        public String getEntityAbstractsForPublication() {
            var filteredAbstracts = abstracts.stream().filter(value -> !value.isBlank()).toList();
            return isNull(filteredAbstracts) || filteredAbstracts.isEmpty() ? null
                       : filteredAbstracts.stream().collect(Collectors.joining(System.lineSeparator()));
        }

        public URI getLink() {
            return link;
        }

        public List<URI> getSubjects() {
            return subjects;
        }

        public String getVolume() {
            return volume;
        }

        public String getIssue() {
            return issue;
        }

        public String getArticleNumber() {
            return articleNumber;
        }

        public String getSubjectCode() {
            return subjectCode;
        }

        public void setAccessCode(String accessCode) {
            this.accessCode = accessCode;
        }

        public String getAccessCode() {
            return accessCode;
        }

        public Project getProject() {
            return project;
        }

        public void setProject(Project project) {
            this.project = project;
        }

        private static no.unit.nva.model.PublicationDate createPublicationDateForPublication(
            PublicationDate publicationDate) {
            return new no.unit.nva.model.PublicationDate.Builder().withYear(publicationDate.getNva().getYear())
                       .withMonth(publicationDate.getNva().getMonth())
                       .withDay(publicationDate.getNva().getDay())
                       .build();
        }

        private PublicationDate createPublicationDate() {
            return new PublicationDate("2020", new PublicationDateNva.Builder().withYear("2020").build());
        }

        private Map<String, String> createCorrespondingMap() {
            return Map.of("no", "Noe på norsk språk", "en", "Something in English", "fr", "Une chose en Francais", "lv",
                          "Labdien, mans nezināmais draugs", "de", "Ein ding aus Deutsch", "is",
                          "Mér likar þessir hestar");
        }

        private List<String> notRandomAlternativeTitle() {
            return List.of("Noe på norsk språk", "Something in English", "One more title in English",
                           "Une chose en Francais", "Labdien, mans nezināmais draugs", "Ein ding aus Deutsch",
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
            return switch (someWeirdNess) {
                case 0 ->
                    new Language(List.of("nob"), UriWrapper.fromUri(LanguageMapper.LEXVO_URI_PREFIX + "nob").getUri());
                case 1 -> new Language(null, UriWrapper.fromUri(LanguageMapper.LEXVO_URI_UNDEFINED).getUri());
                default -> new Language(List.of("norsk", "svensk"),
                                        UriWrapper.fromUri(LanguageMapper.LEXVO_URI_UNDEFINED).getUri());
            };
        }

        private no.sikt.nva.brage.migration.record.Publication createPublication() {
            var publication = new no.sikt.nva.brage.migration.record.Publication();
            publication.setPublicationContext(new PublicationContext());
            publication.getPublicationContext().setPublisher(new Publisher(publisherId));
            publication.getPublicationContext().setJournal(new Journal(journalId));
            publication.getPublicationContext().setSeries(new Series(seriesId));
            publication.setPartOfSeries(partOfSeries);
            publication.setIsbnList(List.of(isbn));
            publication.setIsmnList(ismnList);
            publication.setIssnList(issnList);
            publication.setJournal(journal);
            return publication;
        }
    }

    private static Contributor createContributor() {
        return new Contributor(new Identity("Ola", "123", null), "Creator", "author",
                               List.of(new Affiliation("12345", "someAffiliation", "handle")));
    }
}
