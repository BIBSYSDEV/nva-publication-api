package no.unit.nva.cristin.mapper.artisticproduction;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;


@Data
@Builder(
    builderClassName = "ArtisticGenreBuilder",
    toBuilder = true,
    builderMethodName = "builder",
    buildMethodName = "build",
    setterPrefix = "with"
)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@JsonIgnoreProperties({"sjangernavn", "sjangernavn_engelsk", "sjangernavn_nynorsk"})
public class ArtisticGenre {

    @JsonProperty("sjangerkode")
    private String genreCode;

    public ArtisticGenre() {

    }
}
