package no.unit.nva.model.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Collection;

import static java.util.Objects.isNull;

@SuppressWarnings("rawtypes")
public class ValidTextContentListValuesValidator
    implements ConstraintValidator<ValidTextContentListValues, Collection> {

    @Override
    @SuppressWarnings("unchecked")
    public boolean isValid(Collection value, ConstraintValidatorContext context) {
        if (isNull(value)) {
            return true;
        }
        return ((Collection<?>) value).stream()
                   .filter(String.class::isInstance)
                   .map(String.class::cast)
                   .allMatch(ValidTextContentListValuesValidator::isValidListElement);
    }

    private static boolean isValidListElement(String element) {
        return isNull(element) || ValidTextContentValidator.containsOnlyValidXmlCharacters(element);
    }
}
