package no.unit.nva.cristin.mapper;

import static no.unit.nva.cristin.lambda.constants.MappingConstants.HRCS_ACTIVITIES_MAP;
import static no.unit.nva.cristin.lambda.constants.MappingConstants.HRCS_CATEGORIES_MAP;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import no.unit.nva.cristin.mapper.nva.exceptions.HrcsException;
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
public class CristinHrcsCategoriesAndActivities {
    
    public static final String HRCS_CATEGORY_URI = "https://nva.unit.no/hrcs/category/";
    public static final String HRCS_ACTIVITY_URI = "https://nva.unit.no/hrcs/activity/";
    private static final String CATEGORY = "helsekategorikode";
    private static final String ACTIVITY = "aktivitetskode";
    private static final String CATEGORY_OR_ACTIVITY_NOT_VALID_EXCEPTION = "The %s Id %s is not a valid entry.";
    
    @JsonProperty(CATEGORY)
    private String category;
    @JsonProperty(ACTIVITY)
    private String activity;
    
    public static boolean validateCategory(String category) {
        if (!HRCS_CATEGORIES_MAP.containsKey(category)) {
            throw new HrcsException(String.format(CATEGORY_OR_ACTIVITY_NOT_VALID_EXCEPTION, "category", category));
        }
        return true;
    }
    
    public static boolean validateActivity(String activity) {
        if (!HRCS_ACTIVITIES_MAP.containsKey(activity)) {
            throw new HrcsException(String.format(CATEGORY_OR_ACTIVITY_NOT_VALID_EXCEPTION, "activity", activity));
        }
        return true;
    }
    
    @JacocoGenerated
    public CristinHrcsCategoriesAndActivities.CristinHrcsBuilder copy() {
        return this.toBuilder();
    }
}
