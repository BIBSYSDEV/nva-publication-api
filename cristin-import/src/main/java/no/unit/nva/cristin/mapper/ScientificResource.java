package no.unit.nva.cristin.mapper;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import nva.commons.core.JacocoGenerated;

@Builder(
    builderClassName = "ScientificResourceBuilder",
    toBuilder = true,
    builderMethodName = "builder",
    buildMethodName = "build",
    setterPrefix = "with"
)
@Getter
@Setter
@Data
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@JsonIgnoreProperties({"seqdbh", "arstall_online", "arstall_trykket", "varbeidlopenr", "titteltekst",
    "varbeidhovedkatkode", "varbeidunderkatkode", "pubidnr_itar", "publikasjonsform", "publikasjonsformnavn",
    "publiseringskanal", "publiseringskanalnavn", "publiseringskanaltype", "publtypenavn", "issn", "isbn", "merknad",
    "fagomradekode_npi", "fagomradenavn_npi", "fagfeltkode_npi", "fagfeltnavn_npi", "sprakkode", "spraknavn", "doi"})
public class ScientificResource {

    @JsonProperty("h_dbh_forskres_forfatter")
    private List<ScientificPerson> scientificPeople;
    @JsonProperty("kvalitetsnivakode")
    private String qualityCode;
    @JsonProperty("arstall")
    private String reportedYear;

    @JacocoGenerated
    @JsonCreator
    private ScientificResource() {
    }
}



