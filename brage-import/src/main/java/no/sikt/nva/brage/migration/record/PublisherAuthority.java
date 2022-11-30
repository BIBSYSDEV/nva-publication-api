package no.sikt.nva.brage.migration.record;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

@JacocoGenerated
public class PublisherAuthority {

    private String brage;
    private Boolean nva;

    @JsonCreator
    public PublisherAuthority(@JsonProperty("brage") String brage,
                              @JsonProperty("nva") Boolean nva) {
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

    @JsonProperty("brage")
    public String getBrage() {
        return brage;
    }

    public void setBrage(String brage) {
        this.brage = brage;
    }

    @JsonProperty("nva")
    public Boolean getNva() {
        return nva;
    }

    public void setNva(Boolean nva) {
        this.nva = nva;
    }
}
