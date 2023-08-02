package no.unit.nva.expansion.model;

import static java.util.Objects.nonNull;
import static no.unit.nva.expansion.ResourceExpansionServiceImpl.CONTENT_TYPE;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.expansion.model.cristin.CristinOrganization;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.ContributorVerificationStatus;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Organization;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.Reference;
import no.unit.nva.model.contexttypes.Book;
import no.unit.nva.model.contexttypes.Journal;
import no.unit.nva.model.contexttypes.PublicationContext;
import no.unit.nva.model.contexttypes.PublishingHouse;
import no.unit.nva.model.contexttypes.Report;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.pages.Pages;
import no.unit.nva.publication.external.services.AuthorizedBackendUriRetriever;
import no.unit.nva.publication.model.business.importcandidate.ImportCandidate;
import no.unit.nva.publication.model.business.importcandidate.ImportStatus;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;
import org.joda.time.DateTime;

@SuppressWarnings({"PMD.GodClass", "PMD.ExcessivePublicCount"})
@JacocoGenerated
@JsonTypeName(ExpandedImportCandidate.TYPE)
public class ExpandedImportCandidate implements ExpandedDataEntry {

    public static final String TYPE = "ImportCandidate";
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
    public static final String CREATED_DATE = "createdDate";
    public static final String CRISTIN = "cristin";
    public static final String ORGANIZATION = "organization";
    public static final String CONTRIBUTORS = "contributors";
    public static final String IMPORT_CANDIDATE = "import-candidate";
    private static final String CUSTOMER = "customer";
    private static final String CRISTIN_ID = "cristinId";
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
    private PublishingHouse publisher;
    @JsonProperty(JOURNAL_FIELD)
    private Journal journal;
    @JsonProperty(VERIFIED_CONTRIBUTORS_NUMBER_FIELD)
    private int numberOfVerifiedContributors;
    @JsonProperty(CONTRIBUTORS_NUMBER_FIELD)
    private int totalNumberOfContributors;
    @Getter
    @JsonProperty(CONTRIBUTORS)
    private List<Contributor> contributors;
    @JsonProperty(ORGANIZATIONS_FIELD)
    private List<Organization> organizations;
    @JsonProperty(IMPORT_STATUS_FIELD)
    private ImportStatus importStatus;
    @JsonProperty(PUBLICATION_YEAR_FIELD)
    private String publicationYear;
    @JsonProperty(CREATED_DATE)
    private Instant createdDate;

    public static ExpandedImportCandidate fromImportCandidate(ImportCandidate importCandidate,
                                                              AuthorizedBackendUriRetriever uriRetriever) {
        return new ExpandedImportCandidate.Builder().withIdentifier(generateIdentifier(importCandidate.getIdentifier()))
                   .withAdditionalIdentifiers(importCandidate.getAdditionalIdentifiers())
                   .withPublicationInstance(extractPublicationInstance(importCandidate))
                   .withImportStatus(importCandidate.getImportStatus())
                   .withPublicationYear(extractPublicationYear(importCandidate))
                   .withOrganizations(extractOrganizations(importCandidate, uriRetriever))
                   .withDoi(extractDoi(importCandidate))
                   .withMainTitle(extractMainTitle(importCandidate))
                   .withTotalNumberOfContributors(extractNumberOfContributors(importCandidate))
                   .withNumberOfVerifiedContributors(extractNumberOfVerifiedContributors(importCandidate))
                   .withContributors(extractContributors(importCandidate))
                   .withJournal(extractJournal(importCandidate))
                   .withPublisher(extractPublisher(importCandidate))
                   .withCreatedDate(importCandidate.getCreatedDate())
                   .build();
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
    public PublishingHouse getPublisher() {
        return publisher;
    }

    public void setPublisher(PublishingHouse publisher) {
        this.publisher = publisher;
    }

    @JacocoGenerated
    public Journal getJournal() {
        return journal;
    }

    public void setJournal(Journal journal) {
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
    public List<Organization> getOrganizations() {
        return organizations;
    }

    public void setOrganizations(List<Organization> organizations) {
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

    private static PublishingHouse extractPublisher(ImportCandidate importCandidate) {
        return Optional.ofNullable(importCandidate.getEntityDescription())
                   .map(EntityDescription::getReference)
                   .map(Reference::getPublicationContext)
                   .filter(ExpandedImportCandidate::hasPublisher)
                   .map(ExpandedImportCandidate::extractPublishingHouse)
                   .orElse(null);
    }

    /**
     * For now, importCandidate is an object constructed only by scopusConverter, which supports two PublicationContext
     * types where PublishingHouse is present: Book and Report.
     */

    private static PublishingHouse extractPublishingHouse(PublicationContext publicationContext) {
        return isBook(publicationContext) ? ((Book) publicationContext).getPublisher()
                   : ((Report) publicationContext).getPublisher();
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

    private static Journal extractJournal(ImportCandidate importCandidate) {
        return isJournalContent(importCandidate) ? getPublicationContext(importCandidate) : null;
    }

    private static Journal getPublicationContext(ImportCandidate importCandidate) {
        return Optional.ofNullable(importCandidate.getEntityDescription())
                   .map(EntityDescription::getReference)
                   .map(Reference::getPublicationContext)
                   .map(Journal.class::cast)
                   .orElse(null);
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

    private static List<Organization> extractOrganizations(ImportCandidate importCandidate,
                                                           AuthorizedBackendUriRetriever uriRetriever) {
        return importCandidate.getEntityDescription()
                   .getContributors()
                   .stream()
                   .map(Contributor::getAffiliations)
                   .flatMap(List::stream)
                   .filter(organization -> nonNull(organization.getId()))
                   .filter(org -> attempt(() -> isNvaCustomer(org.getId(), uriRetriever)).orElseThrow())
                   .collect(Collectors.toList());
    }

    //TODO: should be refactored when we have updated commons version. Should return false if response status is 404
    // Should use getResponse() method of uriRetriever instead of getRawContent()
    private static boolean isNvaCustomer(URI id, AuthorizedBackendUriRetriever uriRetriever) {
        return attempt(() -> getCristinIdentifier(id)).map(ExpandedImportCandidate::toCristinOrgUri)
                   .map(uri -> fetchTopLevelOrg(uri, uriRetriever))
                   .map(Optional::get)
                   .map(ExpandedImportCandidate::toCristinOrganization)
                   .map(CristinOrganization::getPartOf)
                   .map(ExpandedImportCandidate::getId)
                   .map(ExpandedImportCandidate::toFetchCustomerByCristinIdUri)
                   .map(uri -> fetchCustomer(uriRetriever, uri))
                   .map(Optional::get)
                   .map(ExpandedImportCandidate::okResponse)
                   .orElse(failure -> false);
    }

    private static Optional<String> fetchCustomer(AuthorizedBackendUriRetriever uriRetriever, URI uri) {
        return uriRetriever.getRawContent(toFetchCustomerByCristinIdUri(uri), CONTENT_TYPE);
    }

    private static URI getId(List<Organization> list) {
        return list.get(0).getId();
    }

    private static CristinOrganization toCristinOrganization(String response) throws JsonProcessingException {
        return JsonUtils.dtoObjectMapper.readValue(response, CristinOrganization.class);
    }

    private static Optional<String> fetchTopLevelOrg(URI uri, AuthorizedBackendUriRetriever uriRetriever) {
        return uriRetriever.getRawContent(uri, CONTENT_TYPE);
    }

    private static String getCristinIdentifier(URI id) {
        return UriWrapper.fromUri(id).getLastPathElement();
    }

    private static URI toCristinOrgUri(String cristinId) {
        return UriWrapper.fromHost(API_HOST).addChild(CRISTIN).addChild(ORGANIZATION).addChild(cristinId).getUri();
    }

    private static boolean okResponse(String response) {
        return response.contains("200");
    }

    private static URI toFetchCustomerByCristinIdUri(URI topLevelOrganization) {
        var getCustomerEndpoint = UriWrapper.fromHost(API_HOST).addChild(CUSTOMER).addChild(CRISTIN_ID).getUri();
        return URI.create(
            getCustomerEndpoint + "/" + URLEncoder.encode(topLevelOrganization.toString(), StandardCharsets.UTF_8));
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

        public Builder withPublisher(PublishingHouse publisher) {
            expandedImportCandidate.setPublisher(publisher);
            return this;
        }

        public Builder withJournal(Journal journal) {
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

        public Builder withOrganizations(List<Organization> organizations) {
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

        public ExpandedImportCandidate build() {
            return expandedImportCandidate;
        }
    }
}
