package no.unit.nva.publication.model.business;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DoiRequestStatusTest {
    
    @ParameterizedTest(name = "parse returns DoiRequestStatus '{1}' for input: '{0}'")
    @CsvSource({
        "requested,REQUESTED",
        "Requested,REQUESTED",
        "reQueSted,REQUESTED",
        "REjeCted,REJECTED",
        "rejected,REJECTED",
        "approved,APPROVED",
        "Approved,APPROVED",
    })
    public void parseReturnsDoiRequestStatusIgnoringCase(String input, DoiRequestStatus expected) {
        DoiRequestStatus actual = DoiRequestStatus.parse(input);
        assertThat(actual, is(equalTo(expected)));
    }
    
    @ParameterizedTest
    // ExistingState , RequestedChange, ExpectedState
    @CsvSource({
        "REQUESTED,APPROVED,APPROVED",
        "REQUESTED,REJECTED,REJECTED",
        "REJECTED,APPROVED,APPROVED",
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
        "REQUESTED,REQUESTED",
        "APPROVED,REQUESTED",
        "APPROVED,APPROVED",
        "APPROVED,REJECTED",
        "REJECTED,REJECTED",
        "REJECTED,REQUESTED"
    })
    void invalidStatusChanges(DoiRequestStatus existingState,
                              DoiRequestStatus requestedChange) {
        var actualException = assertThrows(IllegalArgumentException.class,
            () -> existingState.changeStatus(requestedChange));
        assertThat(actualException.getMessage(), is(equalTo(
            existingState.getErrorMessageForNotAllowedStatusChange(requestedChange))));
    }
}