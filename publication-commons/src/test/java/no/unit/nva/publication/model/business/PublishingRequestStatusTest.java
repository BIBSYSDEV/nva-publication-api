package no.unit.nva.publication.model.business;

import static no.unit.nva.publication.model.business.TicketStatus.TicketStatusConstants.CLOSED_STATUS;
import static no.unit.nva.publication.model.business.TicketStatus.TicketStatusConstants.COMPLETED_STATUS;
import static no.unit.nva.publication.model.business.TicketStatus.TicketStatusConstants.PENDING_STATUS;
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

//TODO: test from handler
class PublishingRequestStatusTest {
    
    private static final String UNKNOWN_VALUE = "ObviouslyUnknownValue";
    
    @ParameterizedTest
    @ValueSource(strings = {PENDING_STATUS, COMPLETED_STATUS, CLOSED_STATUS})
    void shouldAcceptTextualValueForEnum(String textualValue) throws JsonProcessingException {
        var jsonString = String.format("\"%s\"", textualValue);
        var actualValue = JsonUtils.dtoObjectMapper.readValue(jsonString, TicketStatus.class);
        assertThat(actualValue, is(in(TicketStatus.values())));
    }
    
    @Test
    void shouldThrowExceptionWhenInputIsUnknownValue() {
        Executable executable = () -> TicketStatus.parse(UNKNOWN_VALUE);
        assertThrows(IllegalArgumentException.class, executable);
    }
    
    @Test
    void shouldNotAllowApprovedToChange() {
        final Executable executable =
            () -> TicketStatus.COMPLETED.changeStatus(TicketStatus.CLOSED);
        assertThrows(IllegalArgumentException.class, executable);
    }
}
