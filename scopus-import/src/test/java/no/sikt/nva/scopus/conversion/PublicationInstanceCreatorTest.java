package no.sikt.nva.scopus.conversion;

import static org.junit.jupiter.api.Assertions.assertThrows;
import no.scopus.generated.CitationTypeTp;
import no.scopus.generated.CitationtypeAtt;
import no.sikt.nva.scopus.exception.UnsupportedCitationTypeException;
import no.sikt.nva.scopus.utils.ScopusGenerator;
import no.unit.nva.model.contexttypes.Book.BookBuilder;
import org.junit.jupiter.api.Test;

public class PublicationInstanceCreatorTest {

    @Test
    void shouldThrowUnsupportedCitationTypeExceptionWhenUnsupportedCitationType() {
        var scopusGenerator = new ScopusGenerator();
        setUnsupportedCitationType(scopusGenerator);
        var creator = new PublicationInstanceCreator(scopusGenerator.getDocument(), new BookBuilder().build());

        assertThrows(UnsupportedCitationTypeException.class, creator::getPublicationInstance);
    }

    private static void setUnsupportedCitationType(ScopusGenerator scopusGenerator) {
        scopusGenerator.getDocument().getItem().getItem().getBibrecord().getHead().getCitationInfo().getCitationType()
            .clear();
        var citationTypeTp = new CitationTypeTp();
        citationTypeTp.setCode(CitationtypeAtt.DP);
        scopusGenerator.getDocument().getItem().getItem().getBibrecord().getHead().getCitationInfo().getCitationType()
            .add(citationTypeTp);
    }
}
