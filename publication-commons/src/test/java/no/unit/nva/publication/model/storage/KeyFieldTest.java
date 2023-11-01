package no.unit.nva.publication.model.storage;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIn.in;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.fasterxml.jackson.core.JsonProcessingException;
import no.unit.nva.commons.json.JsonUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class KeyFieldTest {

    public static final String UNKNOWN_VALUE = "ObviouslyUnknownValue";

    @ParameterizedTest(name = "Should accept textual value {0} for enum")
    @ValueSource(strings = {"Resource", "Ticket", "Message", "DoiRequest"})
    void shouldAcceptTextualValueForEnum(String textualValue) throws JsonProcessingException {
        var jsonString = String.format("\"%s\"", textualValue);
        var actualValue = JsonUtils.dtoObjectMapper.readValue(jsonString, KeyField.class);
        assertThat(actualValue, is(in(KeyField.values())));
    }

    @Test
    void shouldThrowExceptionWhenInputIsUnknownValue() {
        Executable executable = () -> KeyField.parse(UNKNOWN_VALUE);
        assertThrows(IllegalArgumentException.class, executable);
    }
}