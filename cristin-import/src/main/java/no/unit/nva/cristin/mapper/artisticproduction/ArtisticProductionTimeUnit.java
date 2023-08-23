package no.unit.nva.cristin.mapper.artisticproduction;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import nva.commons.core.JacocoGenerated;

import java.util.Locale;

@Data
@Builder(
    builderClassName = "ArtisticProductionTimeUnitBuilder",
    toBuilder = true,
    builderMethodName = "builder",
    buildMethodName = "build",
    setterPrefix = "with"
)
@AllArgsConstructor(access = AccessLevel.PUBLIC)
@JsonIgnoreProperties({"tidsenhetnavn", "tidsenhetnavn_nynorsk", "tidsenhetnavn_engelsk"})


public class ArtisticProductionTimeUnit {


    //The only time codes that exist in the dataset are minutes or weeks.
    @JsonIgnore
    public static final String MINUTE = "MINUTT";

    @JsonProperty("tidsenhetkode")
    private String timeUnitCode;

    @JacocoGenerated
    public ArtisticProductionTimeUnit() {

    }

    @JsonIgnore
    public boolean timeUnitIsInMinutes() {
        return MINUTE.equalsIgnoreCase(timeUnitCode.toUpperCase(Locale.ROOT));
    }



}
