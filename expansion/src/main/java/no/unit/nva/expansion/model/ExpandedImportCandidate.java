package no.unit.nva.expansion.model;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Reference;
import no.unit.nva.model.contexttypes.Book;
import no.unit.nva.model.contexttypes.Journal;
import no.unit.nva.model.contexttypes.MediaContributionPeriodical;
import no.unit.nva.model.contexttypes.PublicationContext;
import no.unit.nva.model.contexttypes.PublishingHouse;
import no.unit.nva.model.contexttypes.Report;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.pages.Pages;
import no.unit.nva.publication.model.business.ImportCandidate;
import no.unit.nva.publication.model.business.ImportStatus;
import nva.commons.core.JacocoGenerated;

@JsonTypeName(ExpandedImportCandidate.TYPE)
public class ExpandedImportCandidate {

    public static final String TYPE = "ImportCandidate";
    private SortableIdentifier identifier;
    private Set<AdditionalIdentifier> additionalIdentifiers;
    private URI doi;
    private PublicationInstance<? extends Pages> publicationInstance;
    private String mainTitle;
    private PublishingHouse publisher;
    private Journal journal;
    private int numberOfVerifiedContributors;
    private int totalNumberOfContributors;
    private List<Organization> organizations;
    private ImportStatus importStatus;
    private int publicationYear;

    public static ExpandedImportCandidate fromImportCandidate(ImportCandidate importCandidate) {
        return new ExpandedImportCandidate.Builder()
                   .withIdentifier(importCandidate.getIdentifier())
                   .withAdditionalIdentifiers(importCandidate.getAdditionalIdentifiers())
                   .withPublicationInstance(extractPublicationInstance(importCandidate))
                   .withImportStatus(importCandidate.getImportStatus())
                   .withPublicationYear(extractPublicationYear(importCandidate))
                   .withOrganizations(extractOrganizations(importCandidate))
                   .withDoi(extractDoi(importCandidate))
                   .withMainTitle(extractMainTitle(importCandidate))
                   .withTotalNumberOfContributors(extractNumberOfContributors(importCandidate))
                   .withNumberOfVerifiedContributors(extractNumberOfContributors(importCandidate))
                   .withJournal(extractJournal(importCandidate))
                   .withPublisher(extractPublisher(importCandidate))
                   .build();
    }

    @JacocoGenerated
    public SortableIdentifier getIdentifier() {
        return identifier;
    }

    public void setIdentifier(SortableIdentifier identifier) {
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

    public void setPublicationInstance(
        PublicationInstance<? extends Pages> publicationInstance) {
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
    public int getPublicationYear() {
        return publicationYear;
    }

    public void setPublicationYear(int publicationYear) {
        this.publicationYear = publicationYear;
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
     * For now importCandidate is an object constructed by scopusConverter only, which supports only two
     * PublicationContext types where PublishingHouse is present: Book and Report.
     */

    private static PublishingHouse extractPublishingHouse(PublicationContext publicationContext) {
        return isBook(publicationContext)
                   ? ((Book) publicationContext).getPublisher()
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
        return isOfTypeMediaContribution(importCandidate)
                   ? getPublicationContext(importCandidate)
                   : null;
    }

    private static MediaContributionPeriodical getPublicationContext(ImportCandidate importCandidate) {
        return Optional.ofNullable(importCandidate.getEntityDescription())
                   .map(EntityDescription::getReference)
                   .map(Reference::getPublicationContext)
                   .map(MediaContributionPeriodical.class::cast)
                   .orElse(null);
    }

    private static boolean isOfTypeMediaContribution(ImportCandidate importCandidate) {
        return Optional.ofNullable(importCandidate.getEntityDescription())
                   .map(EntityDescription::getReference)
                   .map(Reference::getPublicationContext)
                   .map(MediaContributionPeriodical.class::isInstance)
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

    private static int extractPublicationYear(ImportCandidate importCandidate) {
        return Integer.parseInt(importCandidate.getEntityDescription().getPublicationDate().getYear());
    }

    private static List<Organization> extractOrganizations(ImportCandidate importCandidate) {
        return importCandidate.getEntityDescription().getContributors().stream()
                   .map(Contributor::getAffiliations)
                   .flatMap(List::stream)
                   .collect(Collectors.toList());
    }

    public static final class Builder {

        private final ExpandedImportCandidate expandedImportCandidate;

        public Builder() {
            expandedImportCandidate = new ExpandedImportCandidate();
        }

        public Builder withIdentifier(SortableIdentifier identifier) {
            expandedImportCandidate.setIdentifier(identifier);
            return this;
        }

        public Builder withAdditionalIdentifiers(
            Set<AdditionalIdentifier> additionalIdentifiers) {
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

        public Builder withPublicationYear(int publicationYear) {
            expandedImportCandidate.setPublicationYear(publicationYear);
            return this;
        }

        public ExpandedImportCandidate build() {
            return expandedImportCandidate;
        }
    }
}
