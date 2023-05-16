package no.unit.nva.expansion.model;

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

public class ExpandedImportCandidate {

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

    public ExpandedImportCandidate fromImportCandidate(ImportCandidate importCandidate) {
        return new ExpandedImportCandidate.Builder()
                   .withIdentifier(importCandidate.getIdentifier())
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

    public void setIdentifier(SortableIdentifier identifier) {
        this.identifier = identifier;
    }

    public void setAdditionalIdentifiers(Set<AdditionalIdentifier> additionalIdentifiers) {
        this.additionalIdentifiers = additionalIdentifiers;
    }

    public void setDoi(URI doi) {
        this.doi = doi;
    }

    public void setPublicationInstance(
        PublicationInstance<? extends Pages> publicationInstance) {
        this.publicationInstance = publicationInstance;
    }

    public void setMainTitle(String mainTitle) {
        this.mainTitle = mainTitle;
    }

    public void setPublisher(PublishingHouse publisher) {
        this.publisher = publisher;
    }

    public void setJournal(Journal journal) {
        this.journal = journal;
    }

    public void setNumberOfVerifiedContributors(int numberOfVerifiedContributors) {
        this.numberOfVerifiedContributors = numberOfVerifiedContributors;
    }

    public void setTotalNumberOfContributors(int totalNumberOfContributors) {
        this.totalNumberOfContributors = totalNumberOfContributors;
    }

    public void setOrganizations(List<Organization> organizations) {
        this.organizations = organizations;
    }

    public void setImportStatus(ImportStatus importStatus) {
        this.importStatus = importStatus;
    }

    public void setPublicationYear(int publicationYear) {
        this.publicationYear = publicationYear;
    }

    /**
     * For now importCandidate is an object constructed by scopusConverter only, which supports only two
     * PublicationContext types with publisher, it is Book and Report.
     */

    private PublishingHouse extractPublisher(ImportCandidate importCandidate) {
        return Optional.ofNullable(importCandidate.getEntityDescription())
                   .map(EntityDescription::getReference)
                   .map(Reference::getPublicationContext)
                   .filter(this::hasPublisher)
                   .map(this::extractPublishingHouse)
                   .orElse(null);
    }

    private PublishingHouse extractPublishingHouse(PublicationContext publicationContext) {
        return isBook(publicationContext)
                   ? ((Book) publicationContext).getPublisher()
                   : ((Report) publicationContext).getPublisher();
    }

    private boolean hasPublisher(PublicationContext publicationContext) {
        return isBook(publicationContext) || isReport(publicationContext);
    }

    private boolean isReport(PublicationContext publicationContext) {
        return publicationContext.getClass().equals(Report.class);
    }

    private boolean isBook(PublicationContext publicationContext) {
        return publicationContext.getClass().equals(Book.class);
    }

    private Journal extractJournal(ImportCandidate importCandidate) {
        return isOfTypeMediaContribution(importCandidate)
                   ? getPublicationContext(importCandidate)
                   : null;
    }

    private MediaContributionPeriodical getPublicationContext(ImportCandidate importCandidate) {
        return (MediaContributionPeriodical) importCandidate.getEntityDescription()
                                                 .getReference()
                                                 .getPublicationContext();
    }

    private boolean isOfTypeMediaContribution(ImportCandidate importCandidate) {
        return importCandidate.getEntityDescription()
                   .getReference()
                   .getPublicationContext()
                   .getClass()
                   .equals(MediaContributionPeriodical.class);
    }

    private int extractNumberOfContributors(ImportCandidate importCandidate) {
        return importCandidate.getEntityDescription().getContributors().size();
    }

    private String extractMainTitle(ImportCandidate importCandidate) {
        return importCandidate.getEntityDescription().getMainTitle();
    }

    private URI extractDoi(ImportCandidate importCandidate) {
        return importCandidate.getEntityDescription().getReference().getDoi();
    }

    private int extractPublicationYear(ImportCandidate importCandidate) {
        return Integer.parseInt(importCandidate.getEntityDescription().getPublicationDate().getYear());
    }

    private List<Organization> extractOrganizations(ImportCandidate importCandidate) {
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

        public Builder withPublicationInstance(
            PublicationInstance<? extends Pages> publicationInstance) {
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
