package no.unit.nva.cristin.mapper.exhibition;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Set;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import nva.commons.core.JacocoGenerated;

@Builder(
    builderClassName = "MuseumEventCategoryBuilder",
    toBuilder = true,
    builderMethodName = "builder",
    buildMethodName = "build",
    setterPrefix = "with"
)
@Getter
@Setter
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@JsonIgnoreProperties({"hendelsestypenavn", "hendelsestypenavn_engelsk",
    "hendelsestypenavn_nynorsk"})
public class MuseumEventCategory {

    public static final String SUPPORTED_MUSEUM_TYPE = "UTSTILLING";
    private static final Set<String> SUPPORTED_MUSEUM_TYPES = Set.of("UTSTILLING");
    @JsonProperty("hendelsestypekode")
    private String eventCode;

    @JacocoGenerated
    public MuseumEventCategory() {

    }

    @JsonIgnore
    public boolean isMuseumExhibition() {
        return SUPPORTED_MUSEUM_TYPES.contains(eventCode);
    }
}
