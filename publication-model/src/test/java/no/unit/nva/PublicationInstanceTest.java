package no.unit.nva;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import no.unit.nva.model.instancetypes.journal.AcademicLiteratureReview;
import no.unit.nva.model.pages.Range;
import org.junit.jupiter.api.Test;

public class PublicationInstanceTest {

    @Test
    void twoDifferentAcademicLiteratureReviewsShouldNotBeEqual() {
        var one = new AcademicLiteratureReview(new Range(randomString(), randomString()), randomString(),
                                                    randomString(), randomString());
        var two = new AcademicLiteratureReview(new Range(randomString(), randomString()), randomString(),
                                                    randomString(), randomString());
        assertNotEquals(one, two);
    }
}
