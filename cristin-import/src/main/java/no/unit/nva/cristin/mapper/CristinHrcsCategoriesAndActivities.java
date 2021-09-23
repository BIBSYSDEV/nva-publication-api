package no.unit.nva.cristin.mapper;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import no.unit.nva.cristin.mapper.nva.exceptions.HrcsException;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.ioutils.IoUtils;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;

@Data
@Builder(
        builderClassName = "CristinHrcsBuilder",
        toBuilder = true,
        builderMethodName = "builder",
        buildMethodName = "build",
        setterPrefix = "with"
)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class CristinHrcsCategoriesAndActivities {

    private static final String CATEGORY = "helsekategorikode";
    private static final String ACTIVITY = "aktivitetskode";

    public static final String HRCS_CATEGORY_URI = "https://nva.unit.no/hrcs/category/";
    public static final String HRCS_ACTIVITY_URI = "https://nva.unit.no/hrcs/activity/";

    private static final String CATEGORY_OR_ACTIVITY_NOT_VALID_EXCEPTION = "The %s Id %s is not a valid entry.";


    @JsonProperty(CATEGORY)
    private String category;
    @JsonProperty(ACTIVITY)
    private String activity;

    public static URI insertCategoryIdIntoUriString(String categoryId) {
        int categoryIdAsInt = Integer.parseInt(categoryId);
        int categoryIdSubtractedByOne = adjustCategoryIndex(categoryIdAsInt);
        String categoryIdSubtractedByOneWithLeadingZeroes = addRequiredNumberOfLeadingZeros(categoryIdSubtractedByOne);
        return URI.create(HRCS_CATEGORY_URI + categoryIdSubtractedByOneWithLeadingZeroes);
    }

    public static boolean validateCategory(String category) {
        List<String> validCategories = IoUtils.linesfromResource(Path.of("hrcsCategories.txt"));
        if (!validCategories.contains(category)) {
            throw new HrcsException(String.format(CATEGORY_OR_ACTIVITY_NOT_VALID_EXCEPTION, "category", category));
        }
        return true;
    }

    public static boolean validateActivity(String activity) {
        List<String> validCategories = IoUtils.linesfromResource(Path.of("hrcsActivities.txt"));
        if (!validCategories.contains(activity)) {
            throw new HrcsException(String.format(CATEGORY_OR_ACTIVITY_NOT_VALID_EXCEPTION, "activity", activity));
        }
        return true;
    }

    private static int adjustCategoryIndex(int originalId) {
        //NVA uses 0-indexing while Cristin starts at 1...
        return originalId - 1;
    }

    private static String addRequiredNumberOfLeadingZeros(int categoryId) {
        return String.format("%03d", categoryId);
    }


    @JacocoGenerated
    public CristinHrcsCategoriesAndActivities.CristinHrcsBuilder copy() {
        return this.toBuilder();
    }
}
