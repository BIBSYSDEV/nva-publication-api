package no.unit.nva.model;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import no.unit.nva.model.contexttypes.PublicationContext;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.pages.Pages;
import nva.commons.core.JacocoGenerated;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class Reference {
    private PublicationContext publicationContext;
    private URI doi;
    private PublicationInstance<? extends Pages> publicationInstance;

    public Reference() {
    }

    private Reference(Builder builder) {
        this.publicationContext = builder.publicationContext;
        this.doi = builder.doi;
        this.publicationInstance = builder.publicationInstance;
    }

    public PublicationContext getPublicationContext() {
        return publicationContext;
    }

    public void setPublicationContext(PublicationContext publicationContext) {
        this.publicationContext = publicationContext;
    }

    public URI getDoi() {
        return doi;
    }

    public void setDoi(URI doi) {
        this.doi = doi;
    }

    public PublicationInstance<? extends Pages> getPublicationInstance() {
        return publicationInstance;
    }

    public void setPublicationInstance(PublicationInstance<? extends Pages> publicationInstance) {
        this.publicationInstance = publicationInstance;
    }

    public static final class Builder {
        private PublicationInstance<? extends Pages> publicationInstance;
        private PublicationContext publicationContext;
        private URI doi;

        public Builder withPublishingContext(PublicationContext publicationContext) {
            this.publicationContext = publicationContext;
            return this;
        }

        public Builder withDoi(URI doi) {
            this.doi = doi;
            return this;
        }

        public Builder withPublicationInstance(PublicationInstance<? extends Pages> publicationInstance) {
            this.publicationInstance = publicationInstance;
            return this;
        }

        public Reference build() {
            return new Reference(this);
        }
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Reference reference = (Reference) o;
        return Objects.equals(publicationContext, reference.getPublicationContext())
               && Objects.equals(Optional.ofNullable(publicationContext).map(PublicationContext::getClass),
                                 Optional.ofNullable(reference.getPublicationContext()).map(PublicationContext::getClass))
               && Objects.equals(doi, reference.doi)
               && Objects.equals(publicationInstance, reference.getPublicationInstance())
               && Objects.equals(Optional.ofNullable(publicationInstance).map(PublicationInstance::getClass),
                      Optional.ofNullable(reference.getPublicationInstance()).map(PublicationInstance::getClass));
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(publicationContext, doi, publicationInstance);
    }
}
