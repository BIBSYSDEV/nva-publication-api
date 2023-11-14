package no.sikt.nva.scopus.conversion;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import no.scopus.generated.IssnTp;
import no.sikt.nva.scopus.utils.ScopusGenerator;
import org.junit.jupiter.api.Test;

public class PublicationContextCreatorTest {

    @Test
    void shouldNotThrowExceptionWhenIssnHasNotApplicableValues() {
        var scopusGenerator = new ScopusGenerator();
        setNotApplicableIssnType(scopusGenerator);
        var contextCreator = new PublicationContextCreator(scopusGenerator.getDocument(),
                                                           mock(PublicationChannelConnection.class));

        assertDoesNotThrow(contextCreator::createJournal);
    }

    private static void setNotApplicableIssnType(ScopusGenerator scopusGenerator) {
        scopusGenerator.getDocument().getItem().getItem().getBibrecord().getHead().getSource().getIssn().clear();
        var issnTp = new IssnTp();
        issnTp.setType("electronic");
        issnTp.setContent("NA");
        scopusGenerator.getDocument().getItem().getItem().getBibrecord().getHead().getSource().getIssn().add(issnTp);
    }
}
