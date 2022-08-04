package no.unit.nva.publication.model.business;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIn.in;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.fasterxml.jackson.core.JsonProcessingException;
import no.unit.nva.commons.json.JsonUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class DoiRequestStatusTest {

    public static final String UNKNOWN_VALUE = "ObviouslyUnknownValue";

    @ParameterizedTest(name = "should accept textual value {0} for enum")
    @ValueSource(strings = {"REQUESTED", "Pending", "APPROVED", "Completed", "REJECTED", "Closed"})
    void shouldAcceptTextualValueForEnum(String textualValue) throws JsonProcessingException {
        var jsonString = String.format("\"%s\"", textualValue);
        var actualValue = JsonUtils.dtoObjectMapper.readValue(jsonString, DoiRequestStatus.class);
        assertThat(actualValue, is(in(DoiRequestStatus.values())));
    }

    @Test
    void shouldThrowExceptionWhenInputIsUnknownValue() {
        Executable executable = () -> DoiRequestStatus.parse(UNKNOWN_VALUE);
        assertThrows(IllegalArgumentException.class, executable);
    }

    @ParameterizedTest
    // ExistingState , RequestedChange, ExpectedState
    @CsvSource({
        "PENDING,COMPLETED,COMPLETED",
        "PENDING,CLOSED,CLOSED",
        "CLOSED,COMPLETED,COMPLETED",
    })
    @DisplayName("Should follow business rules for valid status changes on DoiRequestStatus")
    void validStatusChanges(DoiRequestStatus existingState,
                            DoiRequestStatus requestedChange,
                            DoiRequestStatus expectedState) {
        assertThat(existingState.changeStatus(requestedChange), is(equalTo(expectedState)));
    }
    
    @ParameterizedTest
    // ExistingState, RequestedChange
    @CsvSource({
        "PENDING,PENDING",
        "COMPLETED,PENDING",
        "COMPLETED,COMPLETED",
        "COMPLETED,CLOSED",
        "CLOSED,CLOSED",
        "CLOSED,PENDING"
    })
    void invalidStatusChanges(DoiRequestStatus existingState,
                              DoiRequestStatus requestedChange) {
        var actualException = assertThrows(IllegalArgumentException.class,
            () -> existingState.changeStatus(requestedChange));
        assertThat(actualException.getMessage(), is(equalTo(
            existingState.getErrorMessageForNotAllowedStatusChange(requestedChange))));
    }
}