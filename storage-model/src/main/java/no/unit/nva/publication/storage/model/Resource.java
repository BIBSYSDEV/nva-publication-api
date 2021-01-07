package no.unit.nva.publication.storage.model;

import static java.util.Objects.hash;
import static nva.commons.utils.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.identifiers.SortableIdentifier;
import nva.commons.utils.JacocoGenerated;
import nva.commons.utils.JsonUtils;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@SuppressWarnings({"PMD.ExcessivePublicCount", "PMD.TooManyFields"})
public class Resource implements WithIdentifier {

    public static final String TYPE = Resource.class.getSimpleName();
    public static final Map<PublicationStatus, List<PublicationStatus>> validStatusTransitionsMap = Map.of(
        PublicationStatus.NEW, List.of(PublicationStatus.DRAFT),
        PublicationStatus.DRAFT, List.of(PublicationStatus.PUBLISHED, PublicationStatus.DRAFT_FOR_DELETION)
    );

    private SortableIdentifier identifier;
    private PublicationStatus status;
    private String owner;
    private Organization publisher;
    private Instant createdDate;
    private Instant modifiedDate;

    public Resource() {
    }

    public static Resource fromPublication(Publication publication){
        Resource resource = new Resource();

        if(publication.getIdentifier()==null){
            resource.setIdentifier(SortableIdentifier.next());
        }
        else {
            resource.setIdentifier(new SortableIdentifier(publication.getIdentifier().toString()));
        }
        resource.setCreatedDate(publication.getCreatedDate());
        resource.setModifiedDate(publication.getModifiedDate());
        resource.setPublisher(publication.getPublisher());
        resource.setOwner(publication.getOwner());
        resource.setStatus(publication.getStatus());
        return  resource;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }

    public PublicationStatus getStatus() {
        return status;
    }

    public void setStatus(PublicationStatus status) {
        this.status = status;
    }

    public Instant getModifiedDate() {
        return modifiedDate;
    }

    public void setModifiedDate(Instant modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    @Override
    public SortableIdentifier getIdentifier() {
        return identifier;
    }

    @Override
    public void setIdentifier(SortableIdentifier identifier) {
        this.identifier = identifier;
    }

    public Organization getPublisher() {
        return publisher;
    }

    public void setPublisher(Organization publisher) {
        this.publisher = publisher;
    }



    @JacocoGenerated
    @Override
    public int hashCode() {
        return hash(getIdentifier(), getStatus(), getOwner(), getPublisher(), getCreatedDate(), getModifiedDate());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Resource)) {
            return false;
        }
        Resource resource = (Resource) o;
        return Objects.equals(getIdentifier(), resource.getIdentifier()) && getStatus() == resource.getStatus()
            && Objects.equals(getOwner(), resource.getOwner()) && Objects.equals(getPublisher(),
            resource.getPublisher()) && Objects.equals(getCreatedDate(), resource.getCreatedDate())
            && Objects.equals(getModifiedDate(), resource.getModifiedDate());
    }

    @Override
    public String toString(){
        return attempt(()-> JsonUtils.objectMapper.writeValueAsString(this)).orElseThrow();
    }

}

