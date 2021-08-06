package no.unit.nva.cristin.mapper;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder(
    builderClassName = "CristinObjectBuilder",
    toBuilder = true,
    builderMethodName = "builder",
    buildMethodName = "build",
    setterPrefix = "with"
)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class CristinTitle {

    public static final String TITLE = "titteltekst";
    public static final String LANGUAGE_CODE = "sprakkode";
    public static final String ABSTRACT = "sammendragtekst";
    public static final String ORIGINAL_TITLE = "J"; //probably means "Ja"
    public static final String NOT_ORIGINAL_TITLE = "N"; //probably means "Nei"
    public static final String STATUS_ORIGINAL = "status_original";

    @JsonProperty(LANGUAGE_CODE)
    private String languagecode;
    @JsonProperty(TITLE)
    private String title;
    @JsonProperty(STATUS_ORIGINAL)
    private String statusOriginal;
    @JsonProperty(ABSTRACT)
    private String abstractText;

    public CristinTitle() {
    }

    @JsonIgnore
    public boolean isMainTitle() {
        return ORIGINAL_TITLE.equalsIgnoreCase(statusOriginal);
    }
}
