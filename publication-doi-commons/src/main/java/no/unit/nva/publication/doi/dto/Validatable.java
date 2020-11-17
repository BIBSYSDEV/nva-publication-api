package no.unit.nva.publication.doi.dto;

import static nva.commons.utils.attempt.Try.attempt;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Objects;
import nva.commons.utils.attempt.Failure;
import nva.commons.utils.attempt.Try;
import org.slf4j.Logger;

public abstract class Validatable {

    public static final String MANDATORY_FIELD_ERROR_PREFIX = "Mandatory field is missing from";
    protected final String errorMessagePrefix =
        String.format(MANDATORY_FIELD_ERROR_PREFIX + " %s: ", this.getClass().getSimpleName());

    /**
     * A {@link Validatable} is valid only if the fields annotated with {@link MandatoryField} annotation are not null
     * and if mandatory or non-mandatory fields that are also {@link Validatable} are valid.
     *
     * @return the object itself if it is valid.
     * @throws IllegalArgumentException if the object is not valid.
     */
    public final Validatable validate() {
        Field[] fields = this.getClass().getDeclaredFields();
        requiredFieldsAreNotNull(fields);
        validatableFieldsAreValid(fields);
        return this;
    }

    protected abstract Logger logger();

    private static <T> Void requireNonNull(T fieldValue) {
        Objects.requireNonNull(fieldValue);
        return null;
    }

    private void validatableFieldsAreValid(Field[] fields) {
        Arrays.stream(fields)
            .map(this::accessibleField)
            .map(attempt(field -> field.get(this)))
            .map(Try::orElseThrow)
            .filter(this::isValidatable)
            .map(this::toValidatable)
            .forEach(Validatable::validate);
    }

    private Field accessibleField(Field field) {
        field.setAccessible(true);
        return field;
    }

    private boolean isValidatable(Object fieldValue) {
        return fieldValue instanceof Validatable;
    }

    private Validatable toValidatable(Object validatable) {
        return (Validatable) validatable;
    }

    private boolean fieldIsMandatory(Field field) {
        return field.isAnnotationPresent(MandatoryField.class);
    }

    private void requiredFieldsAreNotNull(Field[] fields) {
        Arrays.stream(fields)
            .filter(this::fieldIsMandatory)
            .forEach(this::requireFieldIsNotNull);
    }

    private <T> void requireFieldIsNotNull(Field field) {
        field.setAccessible(true);
        Object value = attempt(() -> field.get(this)).orElseThrow();
        attempt(() -> requireNonNull(value))
            .orElseThrow(fail -> missingFieldError(fail, field.getName()));
    }

    private IllegalArgumentException missingFieldError(Failure<Void> fail,
                                                       String missingFieldName) {
        String errorMessage = errorMessagePrefix + missingFieldName;
        logger().error(errorMessage);
        return new IllegalArgumentException(errorMessage, fail.getException());
    }
}
