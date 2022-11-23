package no.sikt.nva.brage.migration.model.entitydescription;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.List;
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

    @JsonProperty("nva")
    public URI getNva() {
        return nva;
    }

    @JacocoGenerated
    public void setNva(URI nva) {
        this.nva = nva;
    }

}
