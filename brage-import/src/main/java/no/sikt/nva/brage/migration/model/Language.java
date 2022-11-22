package no.sikt.nva.brage.migration.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.List;
import nva.commons.core.JacocoGenerated;

@JacocoGenerated
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

    public void setBrage(List<String> brage) {
        this.brage = brage;
    }

    @JsonProperty("nva")
    public URI getNva() {
        return nva;
    }

    public void setNva(URI nva) {
        this.nva = nva;
    }

}
