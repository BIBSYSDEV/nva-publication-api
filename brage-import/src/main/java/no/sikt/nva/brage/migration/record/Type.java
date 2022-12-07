package no.sikt.nva.brage.migration.record;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

@SuppressWarnings("PMD.ShortClassName")
public class Type {

    private final List<String> brage;
    private final String nva;

    @JacocoGenerated
    @JsonCreator
    public Type(@JsonProperty("brage") List<String> brage,
                @JsonProperty("nva") String nva) {
        this.brage = brage;
        this.nva = nva;
    }

    @JacocoGenerated
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
        Type type = (Type) o;
        return Objects.equals(getBrage(), type.getBrage()) && Objects.equals(getNva(), type.getNva());
    }

    public List<String> getBrage() {
        return brage;
    }
}
