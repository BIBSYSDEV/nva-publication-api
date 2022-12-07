package no.sikt.nva.brage.migration.record;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

public class Language {

    private List<String> brage;
    private URI nva;

    @JsonCreator
    public Language(@JsonProperty("brage") List<String> brage,
                    @JsonProperty("nva") URI nva) {
        this.brage = brage;
        this.nva = nva;
    }

    @JsonProperty("brage")
    public List<String> getBrage() {
        return brage;
    }

    @JacocoGenerated
    public void setBrage(List<String> brage) {
        this.brage = brage;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getBrage(), getNva());
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
        Language language = (Language) o;
        return Objects.equals(getBrage(), language.getBrage()) && Objects.equals(getNva(),
                                                                                 language.getNva());
    }

    @JsonProperty("nva")
    public URI getNva() {
        return nva;
    }

    @JacocoGenerated
    public void setNva(URI nva) {
        this.nva = nva;
    }
}
