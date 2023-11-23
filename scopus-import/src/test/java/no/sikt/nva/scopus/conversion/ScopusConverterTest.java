package no.sikt.nva.scopus.conversion;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import no.sikt.nva.scopus.ScopusConverter;
import no.sikt.nva.scopus.conversion.files.ScopusFileConverter;
import no.sikt.nva.scopus.utils.ScopusGenerator;
import org.junit.jupiter.api.Test;

public class ScopusConverterTest {

    @Test
    void shouldReturnImportCandidateWithoutTitleWhenTitleListFromScopusXmlISEmpty() {
        var generator = new ScopusGenerator();
        generator.getDocument().getItem().getItem().getBibrecord().getHead().getCitationTitle().getTitletext().clear();
        var converter = new ScopusConverter(generator.getDocument(), mock(PiaConnection.class),
                                            mock(CristinConnection.class), mock(PublicationChannelConnection.class),
                                            mock(ScopusFileConverter.class));
        var candidate = converter.generateImportCandidate();

        assertThat(candidate.getEntityDescription().getMainTitle(), is(nullValue()));
    }

    @Test
    void shouldReturnImportCandidateWithoutTitleWhenTitleFromXmlIsNull() {
        var generator = new ScopusGenerator();
        setNullTitle(generator);
        var converter = new ScopusConverter(generator.getDocument(), mock(PiaConnection.class),
                                            mock(CristinConnection.class), mock(PublicationChannelConnection.class),
                                            mock(ScopusFileConverter.class));
        var candidate = converter.generateImportCandidate();

        assertThat(candidate.getEntityDescription().getMainTitle(), is(nullValue()));
    }

    private static void setNullTitle(ScopusGenerator generator) {
        generator.getDocument().getItem().getItem().getBibrecord().getHead().getCitationTitle().getTitletext().clear();
        generator.getDocument()
            .getItem()
            .getItem()
            .getBibrecord()
            .getHead()
            .getCitationTitle()
            .getTitletext()
            .add(null);
    }
}
