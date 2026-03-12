package no.unit.nva.model.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Map;

import static java.util.Objects.isNull;

@SuppressWarnings("rawtypes")
public class ValidTextContentMapValuesValidator
    implements ConstraintValidator<ValidTextContentMapValues, Map> {

    @Override
    @SuppressWarnings("unchecked")
    public boolean isValid(Map value, ConstraintValidatorContext context) {
        if (isNull(value)) {
            return true;
        }
        return ((Map<?, ?>) value).values().stream()
                   .filter(String.class::isInstance)
                   .map(String.class::cast)
                   .allMatch(ValidTextContentMapValuesValidator::isValidMapValue);
    }

    private static boolean isValidMapValue(String mapValue) {
        return isNull(mapValue) || ValidTextContentValidator.containsOnlyValidXmlCharacters(mapValue);
    }
}
