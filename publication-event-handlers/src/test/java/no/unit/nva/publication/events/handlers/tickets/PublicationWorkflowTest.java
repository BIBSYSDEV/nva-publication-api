package no.unit.nva.publication.events.handlers.tickets;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import no.unit.nva.publication.events.handlers.tickets.identityservice.PublicationWorkflow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class PublicationWorkflowTest {
    
    @Test
    void shouldThrowExceptionWhenInputIsUnknownValue() {
        var invalidString = randomString();
        var exception = assertThrows(Exception.class, () -> PublicationWorkflow.lookUp(invalidString));
        assertThat(exception.getMessage(), containsString(invalidString));
    }
    
    @ParameterizedTest(name = "should accept value:{0}")
    @EnumSource(PublicationWorkflow.class)
    void shouldAcceptKnownValues(PublicationWorkflow publicationWorkflow) {
        assertThat(publicationWorkflow, is(equalTo(PublicationWorkflow.lookUp(publicationWorkflow.getValue()))));
    }
}