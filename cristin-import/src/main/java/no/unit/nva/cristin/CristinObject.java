package no.unit.nva.cristin;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class CristinObject {

    public static String IDENTIFIER_ORIGIN = "Cristin";
    @JsonProperty("id")
    private String id;
    @JsonProperty("arstall")
    private String publicationYear;
    @JsonProperty("dato_opprettet")
    private LocalDate entryCreationDate;
    @JsonProperty("VARBEID_SPRAK")
    private List<CristinTitle> cristinTitles;

    public CristinObject() {

    }
}
