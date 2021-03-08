package no.unit.nva.publication.doi.update.dto;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.Reference;
import no.unit.nva.model.instancetypes.PublicationInstance;
import nva.commons.core.JacocoGenerated;

public class DoiRegistrarEntryFields {

    private SortableIdentifier resourceIdentifier;
    private List<Contributor> contributors;
    private PublicationDate publicationDate;
    private URI publisherId;
    private String title;
    private PublicationInstance<?> publicationInstance;
    private URI doi;

    public DoiRegistrarEntryFields() {

    }

    public static DoiRegistrarEntryFields fromPublication(Publication publication) {
        DoiRegistrarEntryFields doiRegistrarEntryFields = new DoiRegistrarEntryFields();
        doiRegistrarEntryFields.resourceIdentifier = publication.getIdentifier();
        doiRegistrarEntryFields.contributors = extractContributors(publication);
        doiRegistrarEntryFields.publicationDate = extractPublicationDate(publication);
        doiRegistrarEntryFields.publisherId = extractPublisherId(publication);
        doiRegistrarEntryFields.title = extractTitle(publication);
        doiRegistrarEntryFields.publicationInstance = extractPublicationInstance(publication);
        doiRegistrarEntryFields.doi = extractPublicationDoi(publication);

        return doiRegistrarEntryFields;
    }

    public URI getDoi() {
        return doi;
    }

    public SortableIdentifier getResourceIdentifier() {
        return resourceIdentifier;
    }

    public List<Contributor> getContributors() {
        return contributors;
    }

    public PublicationDate getPublicationDate() {
        return publicationDate;
    }

    public URI getPublisherId() {
        return publisherId;
    }

    public String getTitle() {
        return title;
    }

    public PublicationInstance<?> getPublicationInstance() {
        return publicationInstance;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getResourceIdentifier(), getContributors(), getPublicationDate(), getPublisherId(),
            getTitle(),
            getPublicationInstance(), getDoi());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DoiRegistrarEntryFields)) {
            return false;
        }
        DoiRegistrarEntryFields that = (DoiRegistrarEntryFields) o;
        return Objects.equals(getResourceIdentifier(), that.getResourceIdentifier())
               && Objects.equals(getContributors(), that.getContributors())
               && Objects.equals(getPublicationDate(), that.getPublicationDate())
               && Objects.equals(getPublisherId(), that.getPublisherId())
               && Objects.equals(getTitle(), that.getTitle())
               && Objects.equals(getPublicationInstance(), that.getPublicationInstance())
               && Objects.equals(getDoi(), that.getDoi());
    }

    private static URI extractPublicationDoi(Publication publication) {
        return Optional.of(publication).map(Publication::getDoi).orElse(null);
    }

    private static PublicationInstance<?> extractPublicationInstance(Publication publication) {
        return extractEntityDescription(publication)
                   .map(EntityDescription::getReference)
                   .map(Reference::getPublicationInstance)
                   .orElse(null);
    }

    private static String extractTitle(Publication publication) {
        return extractEntityDescription(publication)
                   .map(EntityDescription::getMainTitle)
                   .orElse(null);
    }

    private static URI extractPublisherId(Publication publication) {
        return Optional.of(publication)
                   .map(Publication::getPublisher)
                   .map(Organization::getId)
                   .orElse(null);
    }

    private static PublicationDate extractPublicationDate(Publication publication) {
        return extractEntityDescription(publication)
                   .map(EntityDescription::getDate)
                   .orElse(null);
    }

    private static List<Contributor> extractContributors(Publication publication) {
        return extractEntityDescription(publication)
                   .map(EntityDescription::getContributors)
                   .orElse(Collections.emptyList());
    }

    private static Optional<EntityDescription> extractEntityDescription(Publication publication) {
        return Optional.of(publication).map(Publication::getEntityDescription);
    }
}
