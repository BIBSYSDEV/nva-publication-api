package no.unit.nva.cristin.mapper;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import nva.commons.core.JacocoGenerated;

@Builder(
    builderClassName = "CristinLectureOrPosterMetaDataBuilder",
    toBuilder = true,
    builderMethodName = "builder",
    buildMethodName = "build",
    setterPrefix = "with"
)
@Getter
@Setter
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@JsonIgnoreProperties({"status_invitert", "status_referee_ordning", "status_plenar"})
public class CristinLectureOrPosterMetaData {

    public static final String NUMBER_OF_PAGES = "antall_sider_totalt";
    public static final String EVENT = "hendelse";

    @JsonProperty(NUMBER_OF_PAGES)
    private String numberOfPages;

    @JsonProperty(EVENT)
    private PresentationEvent event;

    @JacocoGenerated
    public CristinLectureOrPosterMetaData() {
    }

    @JacocoGenerated
    public CristinLectureOrPosterMetaData.CristinLectureOrPosterMetaDataBuilder copy() {
        return this.toBuilder();
    }
}
