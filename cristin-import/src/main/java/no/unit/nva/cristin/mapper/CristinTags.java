package no.unit.nva.cristin.mapper;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import nva.commons.core.JacocoGenerated;

@Data
@Builder(
    builderClassName = "CristinTagsBuilder",
    toBuilder = true,
    builderMethodName = "builder",
    buildMethodName = "build",
    setterPrefix = "with"
)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class CristinTags {

    private static final String BOKMAL = "emneordnavn_bokmal";
    private static final String ENGLISH = "emneordnavn_engelsk";
    private static final String NYNORSK = "emneordnavn_nynorsk";

    private static final String TAG_SERIAL_NUMBER = "emneordlopenr";

    @JsonProperty(BOKMAL)
    private String bokmal;
    @JsonProperty(ENGLISH)
    private String english;
    @JsonProperty(NYNORSK)
    private String nynorsk;

    @JsonProperty(TAG_SERIAL_NUMBER)
    private int tagSerialNumber;

    @JacocoGenerated
    public CristinTags() {

    }

    @JacocoGenerated
    public CristinTagsBuilder copy() {
        return this.toBuilder();
    }
}
