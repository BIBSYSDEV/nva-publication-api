package no.unit.nva.cristin;

import static java.util.Objects.nonNull;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class CristinTitle implements Comparable<CristinTitle> {

    public static final String MAIN_TITLE = "titteltekst";
    public static final String LANGUAGE_CODE = "sprakkode";
    public static final String ABSTRACT = "abstract";

    @JsonProperty(LANGUAGE_CODE)
    private String languagecode;
    @JsonProperty(MAIN_TITLE)
    private String title;
    @JsonProperty(ABSTRACT)
    private String abstractText;
    @JsonProperty("status_original")
    private String originalStatus;

    public CristinTitle() {

    }

    @Override
    public int compareTo(CristinTitle that) {
        Integer thisLength = calculateTitleLength(this);
        Integer thatLength = calculateTitleLength(that);
        return thisLength.compareTo(thatLength);
    }

    private Integer calculateTitleLength(CristinTitle cristinTitle) {
        return nonNull(cristinTitle.getTitle()) ? cristinTitle.getTitle().length() : 0;
    }
}
