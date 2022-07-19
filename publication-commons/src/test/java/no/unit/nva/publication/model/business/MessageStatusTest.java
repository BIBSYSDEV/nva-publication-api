package no.unit.nva.publication.model.business;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import no.unit.nva.publication.model.business.MessageStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

public class MessageStatusTest {
    
    @Test
    public void parseThrowsIllegalArgumentExceptionOnInvalidStatus() {
        String invalidStatus = "invalidStatus";
        Executable action = () -> MessageStatus.parse(invalidStatus);
        var exception = assertThrows(IllegalArgumentException.class, action);
        assertThat(exception.getMessage(), containsString(MessageStatus.INVALID_MESSAGE_STATUS));
        assertThat(exception.getMessage(), containsString(invalidStatus));
    }
    
    @Test
    public void parseReturnsStatusForValidInputIgnoringCase() {
        String validStatus = "UnReaD";
        var actualStatus = MessageStatus.parse(validStatus);
        
        assertThat(actualStatus, is(equalTo(MessageStatus.UNREAD)));
    }
    
    @Test
    public void toStringReturnsEnumValue() {
        assertThat(MessageStatus.READ.toString(), is(equalTo("READ")));
    }
}