package no.unit.nva.publication.storage.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.*;

class PublicationRequestStatusTest {

    @Test
    public void parseThrowsIllegalArgumentExceptionOnInvalidStatus() {
        String invalidStatus = "invalidStatus";
        Executable action = () -> PublicationRequestStatus.parse(invalidStatus);
        var exception = assertThrows(IllegalArgumentException.class, action);
        assertThat(exception.getMessage(),
                containsString(PublicationRequestStatus.INVALID_APPROVE_PUBLICATION_REQUEST_STATUS_ERROR));
        assertThat(exception.getMessage(), containsString(invalidStatus));
    }

    @Test
    public void parseReturnsStatusForValidInputIgnoringCase() {
        String validStatus = "pEnDiNg";
        var actualStatus = PublicationRequestStatus.parse(validStatus);

        assertThat(actualStatus, is(equalTo(PublicationRequestStatus.PENDING)));
    }

    @Test
    public void toStringReturnsEnumValue() {
        assertThat(PublicationRequestStatus.APPROVED.toString(), is(equalTo("APPROVED")));
    }


}
