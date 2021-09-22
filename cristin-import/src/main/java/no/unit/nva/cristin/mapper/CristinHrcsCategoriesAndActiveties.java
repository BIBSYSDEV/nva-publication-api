package no.unit.nva.cristin.mapper;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import no.unit.nva.cristin.mapper.nva.exceptions.HrcsException;
import nva.commons.core.JacocoGenerated;

import java.net.URI;

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

    private static final int ACCEPTED_ACTIVITY_STRING_LENGTH = 3;
    private static final String CATEGORY_OUT_OF_RANGE_EXCEPTION = "CategoryId %s is out of range, "
            + "it needs to be between 1 and 21.";
    private static final String ACTIVITY_OUT_OF_RANGE_EXCEPTION = "ActivityId %s is out of range, "
            + "it needs to be between 1 and 8.5";
    private static final String CATEGORY_OR_ACTIVITY_NOT_A_NUMBER_EXCEPTION = "The %s Id need to be a number, "
            + "this entry was: %s";
    private static final String ACTIVITY_WRONG_FORMAT_EXCEPTION = "The activityId needs to be on the double format "
            + "'x.y' with 1 digit and one decimal. This entry was %s";

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
        int categoryAsInt;
        try {
            categoryAsInt = Integer.parseInt(category);
        } catch (NumberFormatException e) {
            throw new HrcsException(String.format(CATEGORY_OR_ACTIVITY_NOT_A_NUMBER_EXCEPTION, "category", category));
        }
        if (categoryAsInt < 1 || categoryAsInt > 21) {
            throw new HrcsException(String.format(CATEGORY_OUT_OF_RANGE_EXCEPTION, category));
        }
        return true;
    }

    public static boolean validateActivity(String activity) {
        double activityAsInt;
        try {
            activityAsInt = Double.parseDouble(activity);
        } catch (NumberFormatException e) {
            throw new HrcsException(String.format(CATEGORY_OR_ACTIVITY_NOT_A_NUMBER_EXCEPTION, "activity", activity));
        }
        if (activity.length() != ACCEPTED_ACTIVITY_STRING_LENGTH) {
            throw new HrcsException(String.format(ACTIVITY_WRONG_FORMAT_EXCEPTION, activity));
        }
        if (activityAsInt < 1 || activityAsInt > 8.5) {
            throw new HrcsException(String.format(ACTIVITY_OUT_OF_RANGE_EXCEPTION, activity));
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
    public CristinHrcsCategoriesAndActiveties.CristinHrcsBuilder copy() {
        return this.toBuilder();
    }
}
