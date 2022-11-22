package no.sikt.nva.brage.migration.model.entitydescription;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

@JacocoGenerated
public class PublicationDate {
    private final String brage;
    private final String nva;

    @JacocoGenerated
    @JsonCreator
    public PublicationDate(@JsonProperty("brage") String brage,
                           @JsonProperty("nva") String nva) {
        this.brage = brage;
        this.nva = nva;
    }

    public String getNva() {
        return nva;
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
        PublicationDate that = (PublicationDate) o;
        return Objects.equals(brage, that.brage) && Objects.equals(nva, that.nva);
    }

    public String getBrage() {
        return brage;
    }

}
