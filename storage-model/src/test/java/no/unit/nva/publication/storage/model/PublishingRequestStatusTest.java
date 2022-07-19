package no.unit.nva.publication.storage.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

//TODO: test from handler
class PublishingRequestStatusTest {
    
    @Test
    public void parseReturnsStatusForValidInputIgnoringCase() {
        var validStatus = "pEnDiNg";
        var actualStatus = PublishingRequestStatus.parse(validStatus);
        
        assertThat(actualStatus, is(equalTo(PublishingRequestStatus.PENDING)));
    }
    
    @Test
    public void toStringReturnsEnumValue() {
        assertThat(PublishingRequestStatus.APPROVED.toString(), is(equalTo("APPROVED")));
    }
    
    @Test
    void parseThrowsIllegalArgumentExceptionOnInvalidStatus() {
        var invalidStatus = "invalidStatus";
        Executable action = () -> PublishingRequestStatus.parse(invalidStatus);
        var exception = assertThrows(IllegalArgumentException.class, action);
        assertThat(exception.getMessage(),
            containsString(PublishingRequestStatus.INVALID_APPROVE_PUBLISHING_REQUEST_STATUS_ERROR));
        assertThat(exception.getMessage(), containsString(invalidStatus));
    }
    
    @Test
    void shouldNotAllowApprovedToChange() {
        final Executable executable =
            () -> PublishingRequestStatus.APPROVED.changeStatus(PublishingRequestStatus.REJECTED);
        assertThrows(IllegalArgumentException.class, executable);
    }
}
