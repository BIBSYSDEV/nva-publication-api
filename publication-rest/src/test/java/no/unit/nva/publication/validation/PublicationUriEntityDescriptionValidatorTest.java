package no.unit.nva.publication.validation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import java.net.URI;
import org.junit.jupiter.api.Test;

public class PublicationUriEntityDescriptionValidatorTest {

    public static final String HOST = "localhost";
    private static final String INVALID_SORTABLE_IDENTIFIER = "0193cfbef040-20d8938d-1096-44e9-8a9e-XXX";
    private static final String PROTOCOL = "https://";
    private static final String PUBLICATION = "/publication/";

    @Test
    void shouldReturnFalseWithInvalidSortableId() {
        assertThat(
            PublicationUriValidator.isValid(
                URI.create(PROTOCOL + HOST + PUBLICATION + INVALID_SORTABLE_IDENTIFIER),
                HOST), is(false));
    }
}
