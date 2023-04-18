package no.unit.nva.publication.model.business;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

public class PublishingWorkflowTest {

    @Test
    void shouldThrowErrorWhenLookingUpNonExistingEnum() {
        var input = randomString();
        assertThrows(IllegalArgumentException.class, () -> PublishingWorkflow.lookUp(input));
    }
}
