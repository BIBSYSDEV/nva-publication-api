package no.unit.nva.publication.storage.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.*;

class ApprovePublicationRequestStatusTest {

    @Test
    public void parseThrowsIllegalArgumentExceptionOnInvalidStatus() {
        String invalidStatus = "invalidStatus";
        Executable action = () -> ApprovePublicationRequestStatus.parse(invalidStatus);
        var exception = assertThrows(IllegalArgumentException.class, action);
        assertThat(exception.getMessage(),
                containsString(ApprovePublicationRequestStatus.INVALID_APPROVE_PUBLICATION_REQUEST_STATUS_ERROR));
        assertThat(exception.getMessage(), containsString(invalidStatus));
    }

    @Test
    public void parseReturnsStatusForValidInputIgnoringCase() {
        String validStatus = "pEnDiNg";
        var actualStatus = ApprovePublicationRequestStatus.parse(validStatus);

        assertThat(actualStatus, is(equalTo(ApprovePublicationRequestStatus.PENDING)));
    }

    @Test
    public void toStringReturnsEnumValue() {
        assertThat(ApprovePublicationRequestStatus.APPROVED.toString(), is(equalTo("APPROVED")));
    }


}
