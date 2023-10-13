package no.unit.nva.cristin.mapper.exhibition;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

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

    private String eventCode;

    @JsonIgnore
    public boolean isMuseumExhibition() {
        return "UTSTILLING".equals(eventCode);
    }
}
