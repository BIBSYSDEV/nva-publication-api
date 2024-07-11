package no.unit.nva;

import java.net.URI;
import java.time.Instant;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Organization;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.ResourceOwner;

public interface WithInternal extends PublicationBase {

    Instant getCreatedDate();

    void setCreatedDate(Instant createdDate);

    PublicationStatus getStatus();

    void setStatus(PublicationStatus status);

    URI getHandle();

    void setHandle(URI handle);

    Instant getPublishedDate();

    void setPublishedDate(Instant publishedDate);

    Instant getModifiedDate();

    void setModifiedDate(Instant modifiedDate);

    ResourceOwner getResourceOwner();

    void setResourceOwner(ResourceOwner resourceOwner);

    Instant getIndexedDate();

    void setIndexedDate(Instant indexedDate);

    SortableIdentifier getIdentifier();

    void setIdentifier(SortableIdentifier identifier);

    URI getLink();

    void setLink(URI link);

    Organization getPublisher();

    void setPublisher(Organization publisher);

    URI getDoi();

    void setDoi(URI doi);
    
}
