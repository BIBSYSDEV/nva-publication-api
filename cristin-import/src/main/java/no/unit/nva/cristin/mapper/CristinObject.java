package no.unit.nva.cristin.mapper;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import no.unit.nva.model.Publication;
import nva.commons.core.JsonSerializable;

@Data
@Builder(
    builderClassName = "CristinObjectBuilder",
    toBuilder = true,
    builderMethodName = "builder",
    buildMethodName = "build",
    setterPrefix = "with"
)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class CristinObject implements JsonSerializable {

    public static String IDENTIFIER_ORIGIN = "Cristin";
    @JsonProperty("id")
    private String id;
    @JsonProperty("arstall")
    private String publicationYear;
    @JsonProperty("dato_opprettet")
    private LocalDate entryCreationDate;
    @JsonProperty("VARBEID_SPRAK")
    private List<CristinTitle> cristinTitles;
    @JsonProperty("varbeidhovedkatkode")
    private String mainCategory;
    @JsonProperty("varbeidunderkatkode")
    private String secondaryCategory;
    @JsonProperty
    private String publicationOwner;

    public CristinObject() {

    }

    public Publication toPublication() {
        return new CristinMapper(this).generatePublication();
    }
}
