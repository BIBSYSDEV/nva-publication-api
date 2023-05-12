package no.unit.nva.publication.model.business;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ImportStatusTest {

    @Test
    void shouldReturnRightImportStatus() {
        var expectedImportStatus = ImportStatus.NOT_IMPORTED;
        var actualImportStatus = ImportStatus.lookup("NOT_IMPORTED");
        assertThat(actualImportStatus, is(equalTo(expectedImportStatus)));

    }

    @Test
    void shouldThrowExceptionWhenInvalidImportStatus() {
        assertThrows(IllegalArgumentException.class, () -> ImportStatus.lookup("notSupportedStatus"));
    }
}
