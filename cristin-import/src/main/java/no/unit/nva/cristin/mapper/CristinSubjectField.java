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
        builderClassName = "CristinSubjectFieldBuilder",
        toBuilder = true,
        builderMethodName = "builder",
        buildMethodName = "build",
        setterPrefix = "with"
)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@JsonIgnoreProperties({"fagomradekode", "navn", "navn_engelsk"})
public class CristinSubjectField {

    public static final String SUBJECT_FIELD_CODE = "fagfeltkode";
    public static final String MISSING_SUBJECT_FIELD_CODE =
            "The value of the field \"fagfeltkode\" in the Cristin entry cant be null.";

    @JsonProperty(SUBJECT_FIELD_CODE)
    private Integer subjectFieldCode;

    public CristinSubjectField() {

    }

    @JacocoGenerated
    public CristinSubjectFieldBuilder copy() {
        return this.toBuilder();
    }
}
