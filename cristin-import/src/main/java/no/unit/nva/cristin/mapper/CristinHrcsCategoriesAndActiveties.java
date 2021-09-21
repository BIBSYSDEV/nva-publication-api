package no.unit.nva.cristin.mapper;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import nva.commons.core.JacocoGenerated;

@Data
@Builder(
        builderClassName = "CristinHrcsBuilder",
        toBuilder = true,
        builderMethodName = "builder",
        buildMethodName = "build",
        setterPrefix = "with"
)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class CristinHrcsCategoriesAndActiveties {

    private static final String CATEGORY = "helsekategorikode";
    private static final String ACTIVITY = "aktivitetskode";

    public static final String HRCS_CATEGORY_URI = "https://nva.unit.no/hrcs/category/";
    public static final String HRCS_ACTIVITY_URI = "https://nva.unit.no/hrcs/activity/";

    @JsonProperty(CATEGORY)
    private String category;
    @JsonProperty(ACTIVITY)
    private String activity;

    public static String insertCategoryIdIntoUriString(String categoryId) {
        //NVA uses 0-indexing while Cristin starts at 1...
        String categoryIdSubtractedByOne = subtractNumericValueOfStringByOne(categoryId);
        String categoryIdSubtractedByOneWithLeadingZeroes = addRequiredNumberOfLeadingZeros(categoryIdSubtractedByOne);
        return HRCS_CATEGORY_URI + categoryIdSubtractedByOneWithLeadingZeroes;
    }

    private static String subtractNumericValueOfStringByOne(String originalNumericString) {
        return String.valueOf(Integer.parseInt(originalNumericString) - 1);
    }

    private static String addRequiredNumberOfLeadingZeros(String numberAsString) {
        return numberAsString.length() == 1 ? "00" + numberAsString : "0" + numberAsString;
    }

    @JacocoGenerated
    public CristinHrcsCategoriesAndActiveties.CristinHrcsBuilder copy() {
        return this.toBuilder();
    }
}
