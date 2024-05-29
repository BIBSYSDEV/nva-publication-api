package no.unit.nva.expansion.model;

import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.expansion.JournalExpansionServiceImpl;
import no.unit.nva.expansion.PublisherExpansionServiceImpl;
import no.unit.nva.expansion.model.cristin.CristinOrganization;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.ContributorVerificationStatus;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Organization;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.Reference;
import no.unit.nva.model.associatedartifacts.AssociatedArtifact;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import no.unit.nva.model.contexttypes.Book;
import no.unit.nva.model.contexttypes.Journal;
import no.unit.nva.model.contexttypes.PublicationContext;
import no.unit.nva.model.contexttypes.PublishingHouse;
import no.unit.nva.model.contexttypes.Report;
import no.unit.nva.model.contexttypes.UnconfirmedJournal;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.pages.Pages;
import no.unit.nva.publication.external.services.RawContentRetriever;
import no.unit.nva.publication.model.business.importcandidate.ImportCandidate;
import no.unit.nva.publication.model.business.importcandidate.ImportStatus;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"PMD.GodClass", "PMD.ExcessivePublicCount", "PMD.TooManyFields"})
@JsonTypeName(ExpandedImportCandidate.TYPE)
public class ExpandedImportCandidate implements ExpandedDataEntry {

    public static final String TYPE = "ImportCandidateSummary";
    private static final Logger logger = LoggerFactory.getLogger(ExpandedImportCandidate.class);
    public static final String API_HOST = new Environment().readEnv("API_HOST");
    public static final String PUBLICATION = "publication";
    public static final String ID_FIELD = "id";
    public static final String ADDITIONAL_IDENTIFIERS_FIELD = "additionalIdentifiers";
    public static final String DOI_FIELD = "doi";
    public static final String MAIN_TITLE_FIELD = "mainTitle";
    public static final String PUBLISHER_FIELD = "publisher";
    public static final String JOURNAL_FIELD = "journal";
    public static final String VERIFIED_CONTRIBUTORS_NUMBER_FIELD = "totalVerifiedContributors";
    public static final String CONTRIBUTORS_NUMBER_FIELD = "totalContributors";
    public static final String ORGANIZATIONS_FIELD = "organizations";
    public static final String IMPORT_STATUS_FIELD = "importStatus";
    public static final String PUBLICATION_YEAR_FIELD = "publicationYear";
    public static final String PUBLICATION_INSTANCE_FIELD = "publicationInstance";
    public static final String CREATED_DATE_FIELD = "createdDate";
    public static final String CONTRIBUTORS_FIELD = "contributors";
    public static final String IMPORT_CANDIDATE = "import-candidate";
    public static final String ASSOCIATED_ARTIFACTS_FIELD = "associatedArtifacts";
    public static final String COLLABORATION_TYPE_FIELD = "collaborationType";
    public static final String PRINT_ISSN_FIELD = "printIssn";
    public static final String ONLINE_ISSN_FIELD = "onlineIssn";
    private static final String CONTENT_TYPE = "application/json";
    public static final String CRISTIN = "cristin";
    public static final String ORGANIZATION = "organization";
    public static final String DEPTH = "depth";
    public static final String TOP = "top";
    public static final String CUSTOMER = "customer";
    public static final String CRISTIN_ID = "cristinId";
    public static final String IS_CUSTOMER_MESSAGE = "Cristin organization {} is nva customer: {}";
    public static final String HAS_FILE_FIELD = "filesStatus";
    @JsonProperty(ID_FIELD)
    private URI identifier;
    @JsonProperty(ADDITIONAL_IDENTIFIERS_FIELD)
    private Set<AdditionalIdentifier> additionalIdentifiers;
    @JsonProperty(DOI_FIELD)
    private URI doi;
    @JsonProperty(PUBLICATION_INSTANCE_FIELD)
    private PublicationInstance<? extends Pages> publicationInstance;
    @JsonProperty(MAIN_TITLE_FIELD)
    private String mainTitle;
    @JsonProperty(PUBLISHER_FIELD)
    private ExpandedPublisher publisher;
    @JsonProperty(JOURNAL_FIELD)
    private ExpandedJournal journal;
    @JsonProperty(VERIFIED_CONTRIBUTORS_NUMBER_FIELD)
    private int numberOfVerifiedContributors;
    @JsonProperty(CONTRIBUTORS_NUMBER_FIELD)
    private int totalNumberOfContributors;
    @Getter
    @JsonProperty(CONTRIBUTORS_FIELD)
    private List<Contributor> contributors;
    @JsonProperty(ORGANIZATIONS_FIELD)
    private Set<ExpandedImportCandidateOrganization> organizations;
    @JsonProperty(COLLABORATION_TYPE_FIELD)
    private CollaborationType collaborationType;
    @JsonProperty(IMPORT_STATUS_FIELD)
    private ImportStatus importStatus;
    @JsonProperty(PUBLICATION_YEAR_FIELD)
    private String publicationYear;
    @JsonProperty(CREATED_DATE_FIELD)
    private Instant createdDate;
    @JsonProperty(ASSOCIATED_ARTIFACTS_FIELD)
    private List<AssociatedArtifact> associatedArtifacts;
    @JsonProperty(PRINT_ISSN_FIELD)
    private String printIssn;
    @JsonProperty(ONLINE_ISSN_FIELD)
    private String onlineIssn;
    @JsonProperty(HAS_FILE_FIELD)
    private FilesStatus filesStatus;

    public static ExpandedImportCandidate fromImportCandidate(ImportCandidate importCandidate,
                                                              RawContentRetriever uriRetriever) {
        var organizations = extractOrganizations(importCandidate, uriRetriever);
        return new ExpandedImportCandidate.Builder().withIdentifier(generateIdentifier(importCandidate.getIdentifier()))
                   .withAdditionalIdentifiers(importCandidate.getAdditionalIdentifiers())
                   .withPublicationInstance(extractPublicationInstance(importCandidate))
                   .withImportStatus(importCandidate.getImportStatus())
                   .withPublicationYear(extractPublicationYear(importCandidate))
                   .withOrganizations(organizations)
                   .withDoi(extractDoi(importCandidate))
                   .withMainTitle(extractMainTitle(importCandidate))
                   .withTotalNumberOfContributors(extractNumberOfContributors(importCandidate))
                   .withNumberOfVerifiedContributors(extractNumberOfVerifiedContributors(importCandidate))
                   .withContributors(extractContributors(importCandidate))
                   .withJournal(extractJournal(importCandidate, uriRetriever))
                   .withPublisher(extractPublisher(importCandidate, uriRetriever))
                   .withCreatedDate(importCandidate.getCreatedDate())
                   .withCooperation(extractCorporation(organizations))
                   .withAssociatedArtifacts(importCandidate.getAssociatedArtifacts())
                   .withPrintIssn(extractPrintIssn(importCandidate))
                   .withOnlineIssn(extractOnlineIssn(importCandidate))
                   .withHasFile(FilesStatus.fromPublication(importCandidate))
                   .build();
    }

    public String getPrintIssn() {
        return printIssn;
    }

    public FilesStatus getFilesStatus() {
        return filesStatus;
    }

    public void setFilesStatus(FilesStatus hasFile) {
        this.filesStatus = hasFile;
    }

    private void setPrintIssn(String printIssn) {
        this.printIssn = printIssn;
    }

    public String getOnlineIssn() {
        return onlineIssn;
    }

    public void setOnlineIssn(String onlineIssn) {
        this.onlineIssn = onlineIssn;
    }

    public void setContributors(List<Contributor> contributors) {
        this.contributors = contributors;
    }

    @JacocoGenerated
    public Instant getCreatedDate() {
        return createdDate;
    }

    private void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }

    @JacocoGenerated
    public List<AssociatedArtifact> getAssociatedArtifacts() {
        return associatedArtifacts;
    }

    @JacocoGenerated
    public void setAssociatedArtifacts(List<AssociatedArtifact> associatedArtifacts) {
        this.associatedArtifacts = associatedArtifacts;
    }

    @JacocoGenerated
    public URI getIdentifier() {
        return identifier;
    }

    public void setIdentifier(URI identifier) {
        this.identifier = identifier;
    }

    @JacocoGenerated
    public Set<AdditionalIdentifier> getAdditionalIdentifiers() {
        return additionalIdentifiers;
    }

    public void setAdditionalIdentifiers(Set<AdditionalIdentifier> additionalIdentifiers) {
        this.additionalIdentifiers = additionalIdentifiers;
    }

    @JacocoGenerated
    public URI getDoi() {
        return doi;
    }

    public void setDoi(URI doi) {
        this.doi = doi;
    }

    @JacocoGenerated
    public PublicationInstance<? extends Pages> getPublicationInstance() {
        return publicationInstance;
    }

    public void setPublicationInstance(PublicationInstance<? extends Pages> publicationInstance) {
        this.publicationInstance = publicationInstance;
    }

    @JacocoGenerated
    public String getMainTitle() {
        return mainTitle;
    }

    public void setMainTitle(String mainTitle) {
        this.mainTitle = mainTitle;
    }

    @JacocoGenerated
    public ExpandedPublisher getPublisher() {
        return publisher;
    }

    public void setPublisher(ExpandedPublisher publisher) {
        this.publisher = publisher;
    }

    @JacocoGenerated
    public CollaborationType getCollaborationType() {
        return collaborationType;
    }

    public void setCollaborationType(CollaborationType collaborationType) {
        this.collaborationType = collaborationType;
    }

    @JacocoGenerated
    public ExpandedJournal getJournal() {
        return journal;
    }

    public void setJournal(ExpandedJournal journal) {
        this.journal = journal;
    }

    @JacocoGenerated
    public int getNumberOfVerifiedContributors() {
        return numberOfVerifiedContributors;
    }

    public void setNumberOfVerifiedContributors(int numberOfVerifiedContributors) {
        this.numberOfVerifiedContributors = numberOfVerifiedContributors;
    }

    @JacocoGenerated
    public int getTotalNumberOfContributors() {
        return totalNumberOfContributors;
    }

    public void setTotalNumberOfContributors(int totalNumberOfContributors) {
        this.totalNumberOfContributors = totalNumberOfContributors;
    }

    @JacocoGenerated
    public Set<ExpandedImportCandidateOrganization> getOrganizations() {
        return organizations;
    }

    public void setOrganizations(Set<ExpandedImportCandidateOrganization> organizations) {
        this.organizations = organizations;
    }

    @JacocoGenerated
    public ImportStatus getImportStatus() {
        return importStatus;
    }

    public void setImportStatus(ImportStatus importStatus) {
        this.importStatus = importStatus;
    }

    @JacocoGenerated
    public String getPublicationYear() {
        return publicationYear;
    }

    public void setPublicationYear(String publicationYear) {
        this.publicationYear = publicationYear;
    }

    @Override
    public String toJsonString() {
        return ExpandedDataEntry.super.toJsonString();
    }

    @Override
    public SortableIdentifier identifyExpandedEntry() {
        return new SortableIdentifier(UriWrapper.fromUri(identifier).getLastPathElement());
    }

    private static String extractOnlineIssn(ImportCandidate importCandidate) {
        return Optional.ofNullable(importCandidate.getEntityDescription().getReference())
                   .map(Reference::getPublicationContext)
                   .filter(publicationContext -> publicationContext instanceof UnconfirmedJournal)
                   .map(UnconfirmedJournal.class::cast)
                   .map(UnconfirmedJournal::getOnlineIssn)
                   .orElse(null);
    }

    private static String extractPrintIssn(ImportCandidate importCandidate) {
        return Optional.ofNullable(importCandidate.getEntityDescription().getReference())
                   .map(Reference::getPublicationContext)
                   .filter(publicationContext -> publicationContext instanceof UnconfirmedJournal)
                   .map(UnconfirmedJournal.class::cast)
                   .map(UnconfirmedJournal::getPrintIssn)
                   .orElse(null);
    }

    private static CollaborationType extractCorporation(Set<ExpandedImportCandidateOrganization> organizations) {
        return organizations.size() > 1 ? CollaborationType.COLLABORATIVE : CollaborationType.NON_COLLABORATIVE;
    }

    private static List<Contributor> extractContributors(ImportCandidate importCandidate) {
        var contributors = importCandidate.getEntityDescription().getContributors();
        return contributors.size() < 5 ? contributors.subList(0, contributors.size()) : contributors.subList(0, 5);
    }

    private static int extractNumberOfVerifiedContributors(ImportCandidate importCandidate) {
        return (int) importCandidate.getEntityDescription()
                         .getContributors()
                         .stream()
                         .filter(ExpandedImportCandidate::isVerifiedContributor)
                         .count();
    }

    private static boolean isVerifiedContributor(Contributor contributor) {
        return ContributorVerificationStatus.VERIFIED.equals(contributor.getIdentity().getVerificationStatus());
    }

    private static URI generateIdentifier(SortableIdentifier identifier) {
        return UriWrapper.fromHost(API_HOST)
                   .addChild(PUBLICATION)
                   .addChild(IMPORT_CANDIDATE)
                   .addChild(identifier.toString())
                   .getUri();
    }

    private static PublicationInstance<? extends Pages> extractPublicationInstance(ImportCandidate importCandidate) {
        return Optional.ofNullable(importCandidate.getEntityDescription())
                   .map(EntityDescription::getReference)
                   .map(Reference::getPublicationInstance)
                   .orElse(null);
    }

    private static ExpandedPublisher extractPublisher(ImportCandidate importCandidate, RawContentRetriever uriRetriever) {
        return Optional.ofNullable(importCandidate.getEntityDescription())
                   .map(EntityDescription::getReference)
                   .map(Reference::getPublicationContext)
                   .filter(ExpandedImportCandidate::hasPublisher)
                   .map(publicationContext -> extractPublishingHouse(publicationContext, uriRetriever))
                   .orElse(null);
    }

    /**
     * For now, importCandidate is an object constructed only by scopusConverter, which supports two PublicationContext
     * types where PublishingHouse is present: Book and Report.
     */

    private static ExpandedPublisher extractPublishingHouse(PublicationContext publicationContext, RawContentRetriever uriRetriever) {
        return isBook(publicationContext)
                   ? expandPublisher( ((Book) publicationContext).getPublisher(), uriRetriever)
                   : expandPublisher( ((Report) publicationContext).getPublisher(), uriRetriever);
    }

    private static ExpandedPublisher expandPublisher(PublishingHouse publisher, RawContentRetriever uriRetriever) {
        var publisherExpansionService = new PublisherExpansionServiceImpl(uriRetriever);
        return publisherExpansionService.createExpandedPublisher(publisher);
    }

    private static boolean hasPublisher(PublicationContext publicationContext) {
        return isBook(publicationContext) || isReport(publicationContext);
    }

    private static boolean isReport(PublicationContext publicationContext) {
        return publicationContext.getClass().equals(Report.class);
    }

    private static boolean isBook(PublicationContext publicationContext) {
        return publicationContext.getClass().equals(Book.class);
    }

    private static ExpandedJournal extractJournal(ImportCandidate importCandidate, RawContentRetriever uriRetriever) {
        return isJournalContent(importCandidate) ? getPublicationContext(importCandidate, uriRetriever) : null;
    }

    private static ExpandedJournal getPublicationContext(ImportCandidate importCandidate, RawContentRetriever uriRetriever) {
        return Optional.ofNullable(importCandidate.getEntityDescription())
                   .map(EntityDescription::getReference)
                   .map(Reference::getPublicationContext)
                   .map(Journal.class::cast)
                   .map(journal ->  expandJournal(journal, uriRetriever))
                   .orElse(null);
    }

    private static ExpandedJournal expandJournal(Journal journal, RawContentRetriever uriRetriever) {
        var journalExpansionService = new JournalExpansionServiceImpl(uriRetriever);
        return journalExpansionService.expandJournal(journal);
    }

    private static boolean isJournalContent(ImportCandidate importCandidate) {
        return Optional.ofNullable(importCandidate.getEntityDescription())
                   .map(EntityDescription::getReference)
                   .map(Reference::getPublicationContext)
                   .map(Journal.class::isInstance)
                   .orElse(false);
    }

    private static int extractNumberOfContributors(ImportCandidate importCandidate) {
        return importCandidate.getEntityDescription().getContributors().size();
    }

    private static String extractMainTitle(ImportCandidate importCandidate) {
        return importCandidate.getEntityDescription().getMainTitle();
    }

    private static URI extractDoi(ImportCandidate importCandidate) {
        return Optional.ofNullable(importCandidate.getEntityDescription())
                   .map(EntityDescription::getReference)
                   .map(Reference::getDoi)
                   .orElse(null);
    }

    private static String extractPublicationYear(ImportCandidate importCandidate) {
        return Optional.ofNullable(importCandidate.getEntityDescription())
                   .map(EntityDescription::getPublicationDate)
                   .map(PublicationDate::getYear)
                   .orElse(String.valueOf(new DateTime().getYear()));
    }

    private static Set<ExpandedImportCandidateOrganization> extractOrganizations(ImportCandidate importCandidate,
                                                                                 RawContentRetriever uriRetriever) {

        return getOrganizationIdList(importCandidate)
                   .map(id -> fetchCristinOrg(id, uriRetriever))
                   .filter(Optional::isPresent)
                   .map(Optional::get)
                   .distinct()
                   .filter(org -> isNvaCustomer(org, uriRetriever))
                   .map(ExpandedImportCandidateOrganization::fromCristinOrganization)
                   .collect(Collectors.toSet());

    }

    private static Stream<URI> getOrganizationIdList(ImportCandidate importCandidate) {
        return importCandidate.getEntityDescription()
                   .getContributors()
                   .stream()
                   .map(Contributor::getAffiliations)
                   .flatMap(List::stream)
                   .filter(Organization.class::isInstance)
                   .map(Organization.class::cast)
                   .map(Organization::getId)
                   .distinct()
                   .filter(Objects::nonNull);
    }

    private static boolean isNvaCustomer(CristinOrganization cristinOrganization, RawContentRetriever uriRetriever) {
        var isCustomer = Optional.ofNullable(cristinOrganization.id())
                             .map(ExpandedImportCandidate::toFetchCustomerByCristinIdUri)
                             .map(uri -> fetchCustomer(uriRetriever, uri))
                             .filter(Optional::isPresent)
                             .map(Optional::get)
                             .map(ExpandedImportCandidate::isHttpOk)
                             .orElse(false);
        logger.info(IS_CUSTOMER_MESSAGE, cristinOrganization.id(), isCustomer);
        return isCustomer;
    }

    private static boolean isHttpOk(HttpResponse<String> stringHttpResponse) {
        return stringHttpResponse.statusCode() == 200;
    }

    private static Optional<CristinOrganization> fetchCristinOrg(URI id, RawContentRetriever uriRetriever) {
        return Optional.ofNullable(getCristinIdentifier(id))
                   .map(ExpandedImportCandidate::toCristinOrgUri)
                   .map(uri -> fetch(uri, uriRetriever))
                   .filter(Optional::isPresent)
                   .map(Optional::get)
                   .map(ExpandedImportCandidate::toCristinOrganization)
                   .map(CristinOrganization::getTopLevelOrg);
    }

    private static Optional<HttpResponse<String>> fetchCustomer(RawContentRetriever uriRetriever, URI uri) {
        return uriRetriever.fetchResponse(uri, CONTENT_TYPE);
    }

    private static URI toFetchCustomerByCristinIdUri(URI topLevelOrganization) {
        var getCustomerEndpoint = UriWrapper.fromHost(API_HOST).addChild(CUSTOMER).addChild(CRISTIN_ID).getUri();
        return URI.create(
            getCustomerEndpoint + "/" + URLEncoder.encode(topLevelOrganization.toString(), StandardCharsets.UTF_8));
    }

    private static CristinOrganization toCristinOrganization(String response) {
        return attempt(() -> JsonUtils.dtoObjectMapper.readValue(response, CristinOrganization.class))
                   .orElseThrow();
    }

    private static Optional<String> fetch(URI uri, RawContentRetriever uriRetriever) {
        return uriRetriever.getRawContent(uri, CONTENT_TYPE);
    }

    private static String getCristinIdentifier(URI id) {
        return UriWrapper.fromUri(id).getLastPathElement();
    }

    private static URI toCristinOrgUri(String cristinId) {
        return UriWrapper.fromHost(API_HOST)
                   .addChild(CRISTIN)
                   .addChild(ORGANIZATION)
                   .addQueryParameter(DEPTH, TOP)
                   .addChild(cristinId).getUri();
    }

    public static final class Builder {

        private final ExpandedImportCandidate expandedImportCandidate;

        public Builder() {
            expandedImportCandidate = new ExpandedImportCandidate();
        }

        public Builder withIdentifier(URI identifier) {
            expandedImportCandidate.setIdentifier(identifier);
            return this;
        }

        public Builder withAdditionalIdentifiers(Set<AdditionalIdentifier> additionalIdentifiers) {
            expandedImportCandidate.setAdditionalIdentifiers(additionalIdentifiers);
            return this;
        }

        public Builder withDoi(URI doi) {
            expandedImportCandidate.setDoi(doi);
            return this;
        }

        public Builder withPublicationInstance(PublicationInstance<? extends Pages> publicationInstance) {
            expandedImportCandidate.setPublicationInstance(publicationInstance);
            return this;
        }

        public Builder withMainTitle(String mainTitle) {
            expandedImportCandidate.setMainTitle(mainTitle);
            return this;
        }

        public Builder withPublisher(ExpandedPublisher publisher) {
            expandedImportCandidate.setPublisher(publisher);
            return this;
        }

        public Builder withCooperation(CollaborationType collaborationType) {
            expandedImportCandidate.setCollaborationType(collaborationType);
            return this;
        }

        public Builder withJournal(ExpandedJournal journal) {
            expandedImportCandidate.setJournal(journal);
            return this;
        }

        public Builder withCreatedDate(Instant createdDate) {
            expandedImportCandidate.setCreatedDate(createdDate);
            return this;
        }

        public Builder withNumberOfVerifiedContributors(int numberOfVerifiedContributors) {
            expandedImportCandidate.setNumberOfVerifiedContributors(numberOfVerifiedContributors);
            return this;
        }

        public Builder withTotalNumberOfContributors(int totalNumberOfContributors) {
            expandedImportCandidate.setTotalNumberOfContributors(totalNumberOfContributors);
            return this;
        }

        public Builder withOrganizations(Set<ExpandedImportCandidateOrganization> organizations) {
            expandedImportCandidate.setOrganizations(organizations);
            return this;
        }

        public Builder withImportStatus(ImportStatus importStatus) {
            expandedImportCandidate.setImportStatus(importStatus);
            return this;
        }

        public Builder withPublicationYear(String publicationYear) {
            expandedImportCandidate.setPublicationYear(publicationYear);
            return this;
        }

        public Builder withContributors(List<Contributor> contributors) {
            expandedImportCandidate.setContributors(contributors);
            return this;
        }

        public Builder withAssociatedArtifacts(AssociatedArtifactList associatedArtifacts) {
            expandedImportCandidate.setAssociatedArtifacts(associatedArtifacts);
            return this;
        }

        public ExpandedImportCandidate build() {
            return expandedImportCandidate;
        }

        public Builder withPrintIssn(String printIssn) {
            expandedImportCandidate.setPrintIssn(printIssn);
            return this;
        }

        public Builder withOnlineIssn(String onlineIssn) {
            expandedImportCandidate.setOnlineIssn(onlineIssn);
            return this;
        }

        public Builder withHasFile(FilesStatus hasFile) {
            expandedImportCandidate.setFilesStatus(hasFile);
            return this;
        }
    }
}
