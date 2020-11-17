package no.unit.nva.publication.doi.dto;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import nva.commons.utils.JacocoGenerated;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ValidatableTest {

    public static final String SAMPLE_VALUE = "SampleValue";
    public static final String SIMPLE_MANDATORY_FIELD = "simpleMandatoryField";
    public static final String MANDATORY_VALIDATABLE_FIELD = "mandatoryValidatableField";
    public static final String SIMPLE_MANDATORY_FIELD_OF_VALIDATABLE_CHILD = "simpleMandatoryField";

    private ValidatableParentObject invalidObjectWithNullValidatableMandatoryFields() {
        ValidatableChildObject childObject = new ValidatableChildObject(SAMPLE_VALUE);
        return new ValidatableParentObject(
            SAMPLE_VALUE,
            SAMPLE_VALUE,
            null,
            childObject
        );
    }

    @Test
    public void validateDoesNotThrowExceptionWhenValidatableIsValid() {
        ValidatableParentObject validValidatable = validObjectWithAllNoNullFields();
        assertDoesNotThrow(validValidatable::validate);
    }

    @Test
    public void validateDoesNotThrowExceptionWhenValidatableHasOptionalFieldsWithNullValues() {
        ValidatableParentObject validValidatableWithNullFields = validObjectWithAllNoNullFields();
        assertDoesNotThrow(validValidatableWithNullFields::validate);
    }

    @Test
    public void validateThrowsExceptionWhenValidatableHasMandatoryFieldsWithNullValues() {
        ValidatableParentObject invalidObject = invalidObjectWithEmptySimpleMandatoryFields();

        Executable action = invalidObject::validate;
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, action);
        String expectedMessage = invalidObject.errorMessagePrefix;
        assertThat(exception.getMessage(), containsString(expectedMessage));
        assertThat(exception.getMessage(), containsString(SIMPLE_MANDATORY_FIELD));
    }



    @Test
    public void validateThrowsExceptionWhenValidatableHasInvalidMandatoryValidatableFields() {
        ValidatableParentObject invalidObject = objectWithInvalidMandatoryField();

        Executable action = invalidObject::validate;
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, action);

        String expectedMessage = invalidObject.getMandatoryValidatableField().errorMessagePrefix;
        assertThat(exception.getMessage(), containsString(expectedMessage));
        assertThat(exception.getMessage(), containsString(SIMPLE_MANDATORY_FIELD_OF_VALIDATABLE_CHILD));
    }

    @Test
    public void validateThrowsExceptionWhenValidatableHasInvalidOptionalValidatableFields() {
        ValidatableParentObject invalidObject = objectWithInvalidOptionalField();

        Executable action = invalidObject::validate;
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, action);

        String expectedMessage = invalidObject.getOptionalValidatableField().errorMessagePrefix;
        assertThat(exception.getMessage(), containsString(expectedMessage));
        assertThat(exception.getMessage(), containsString(SIMPLE_MANDATORY_FIELD_OF_VALIDATABLE_CHILD));
    }

    private ValidatableParentObject objectWithInvalidOptionalField() {
        ValidatableChildObject invalidChild = new ValidatableChildObject(null);
        ValidatableChildObject validChild = new ValidatableChildObject(SAMPLE_VALUE);
        return new ValidatableParentObject(SAMPLE_VALUE, SAMPLE_VALUE, validChild, invalidChild);
    }

    private ValidatableParentObject objectWithInvalidMandatoryField() {
        ValidatableChildObject invalidChild = new ValidatableChildObject(null);
        ValidatableChildObject validChild = new ValidatableChildObject(SAMPLE_VALUE);
        return new ValidatableParentObject(SAMPLE_VALUE, SAMPLE_VALUE, invalidChild, validChild);
    }

    private ValidatableParentObject invalidObjectWithEmptySimpleMandatoryFields() {
        ValidatableChildObject childObject = new ValidatableChildObject(SAMPLE_VALUE);
        return new ValidatableParentObject(
            null,
            SAMPLE_VALUE,
            childObject,
            childObject);
    }

    private ValidatableParentObject validObjectWithAllNoNullFields() {
        ValidatableChildObject childObject = new ValidatableChildObject(SAMPLE_VALUE);
        return new ValidatableParentObject(
            SAMPLE_VALUE,
            SAMPLE_VALUE,
            childObject,
            childObject);
    }

    private ValidatableParentObject createValidValidatableWithEmptyFields() {
        ValidatableChildObject childObject = new ValidatableChildObject(SAMPLE_VALUE);
        return new ValidatableParentObject(
            SAMPLE_VALUE,
            null,
            childObject,
            null);
    }

    private static class ValidatableParentObject extends Validatable {

        private static final Logger logger = LoggerFactory.getLogger(ValidatableParentObject.class);

        @MandatoryField
        private final String simpleMandatoryField;

        private final String simpleOptionalField;

        @MandatoryField
        private final ValidatableChildObject mandatoryValidatableField;

        private final ValidatableChildObject optionalValidatableField;

        public ValidatableParentObject(String simpleMandatoryField,
                                       String simpleOptionalField,
                                       ValidatableChildObject mandatoryValidatableField,
                                       ValidatableChildObject optionalValidatableField) {
            this.simpleMandatoryField = simpleMandatoryField;
            this.simpleOptionalField = simpleOptionalField;
            this.mandatoryValidatableField = mandatoryValidatableField;
            this.optionalValidatableField = optionalValidatableField;
        }

        public String getSimpleMandatoryField() {
            return simpleMandatoryField;
        }

        public String getSimpleOptionalField() {
            return simpleOptionalField;
        }

        public ValidatableChildObject getMandatoryValidatableField() {
            return mandatoryValidatableField;
        }

        public ValidatableChildObject getOptionalValidatableField() {
            return optionalValidatableField;
        }

        @Override
        @JacocoGenerated
        protected Logger logger() {
            return logger;
        }
    }

    private static class ValidatableChildObject extends Validatable {

        private static final Logger logger = LoggerFactory.getLogger(ValidatableChildObject.class);
        @MandatoryField
        private String simpleMandatoryField;

        public ValidatableChildObject(String simpleMandatoryField) {
            this.simpleMandatoryField = simpleMandatoryField;
        }

        public String getSimpleMandatoryField() {
            return simpleMandatoryField;
        }

        @Override
        @JacocoGenerated
        protected Logger logger() {
            return logger;
        }
    }
}