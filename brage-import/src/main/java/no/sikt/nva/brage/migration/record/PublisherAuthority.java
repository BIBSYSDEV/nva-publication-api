package no.sikt.nva.brage.migration.record;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Objects;
import no.unit.nva.model.associatedartifacts.file.PublisherVersion;
import nva.commons.core.JacocoGenerated;

public class PublisherAuthority {

    private List<String> brage;
    private PublisherVersion nva;

    @JacocoGenerated
    @JsonCreator
    public PublisherAuthority(@JsonProperty("brage") List<String> brage,
                              @JsonProperty("nva") PublisherVersion nva) {
        this.nva = nva;
        this.brage = brage;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(brage, nva);
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PublisherAuthority that = (PublisherAuthority) o;
        return Objects.equals(brage, that.brage) && Objects.equals(nva, that.nva);
    }

    @JacocoGenerated
    @JsonProperty("brage")
    public List<String> getBrage() {
        return brage;
    }

    @JacocoGenerated
    public void setBrage(List<String> brage) {
        this.brage = brage;
    }

    @JacocoGenerated
    @JsonProperty("nva")
    public PublisherVersion getNva() {
        return nva;
    }

    @JacocoGenerated
    public void setNva(PublisherVersion nva) {
        this.nva = nva;
    }
}
