package no.sikt.nva.brage.migration.lambda.cleanup;

import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import no.unit.nva.model.PublicationStatus;
import org.junit.jupiter.api.Test;

public class DeleteImportedBragePublicationHandlerTest {

    @Test
    void shouldDeleteImportedPublication() {
        var expectedPublication = randomPublication().setStatus(PublicationStatus.);
    }
}
