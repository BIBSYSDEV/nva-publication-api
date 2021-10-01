package no.unit.nva.cristin.mapper;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import nva.commons.core.JacocoGenerated;

@Data
@Builder(
        builderClassName = "CristinLectureOrPosterMetaDataBuilder",
        toBuilder = true,
        builderMethodName = "builder",
        buildMethodName = "build",
        setterPrefix = "with"
)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@JsonIgnoreProperties({"status_invitert", "status_referee_ordning", "status_plenar", "hendelse"})
public class CristinLectureOrPosterMetaData {

    public static final String NUMBER_OF_PAGES = "antall_sider_totalt";

    @JsonProperty(NUMBER_OF_PAGES)
    private String numberOfPages;

    @JacocoGenerated
    public CristinLectureOrPosterMetaData() {
    }

    public String getNumberOfPages() {
        return numberOfPages;
    }

    @JacocoGenerated
    public CristinLectureOrPosterMetaData.CristinLectureOrPosterMetaDataBuilder copy() {
        return this.toBuilder();
    }
}
