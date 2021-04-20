package no.unit.nva.cristin;

import com.fasterxml.jackson.annotation.JsonProperty;
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

    public CristinObject() {

    }
}
