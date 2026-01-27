package no.unit.nva.model.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import static java.util.Objects.isNull;

public class ValidTextContentValidator implements ConstraintValidator<ValidTextContent, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (isNull(value)) {
            return true;
        }
        return containsOnlyValidXmlCharacters(value);
    }

    public static boolean containsOnlyValidXmlCharacters(String value) {
        return value.codePoints().allMatch(ValidTextContentValidator::isValidXml10Character);
    }

    private static boolean isValidXml10Character(int codePoint) {
        return codePoint == 0x9
               || codePoint == 0xA
               || codePoint == 0xD
               || codePoint >= 0x20 && codePoint <= 0xD7FF
               || codePoint >= 0xE000 && codePoint <= 0xFFFD
               || codePoint >= 0x10000 && codePoint <= 0x10FFFF;
    }
}
