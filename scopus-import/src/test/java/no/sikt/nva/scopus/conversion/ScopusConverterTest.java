package no.sikt.nva.scopus.conversion;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import no.sikt.nva.scopus.ScopusConverter;
import no.sikt.nva.scopus.conversion.files.ScopusFileConverter;
import no.sikt.nva.scopus.utils.ScopusGenerator;
import org.junit.jupiter.api.Test;

public class ScopusConverterTest {

    @Test
    void shouldReturnImportCandidateWithoutTitleWhenTitleListFromScopusXmlISEmpty() {
        var generator = new ScopusGenerator();
        generator.getDocument().getItem().getItem().getBibrecord().getHead().getCitationTitle().getTitletext().clear();
        var customerConnection = mock(NvaCustomerConnection.class);
        var converter = new ScopusConverter(generator.getDocument(), mock(PiaConnection.class),
                                            mock(CristinConnection.class), mock(PublicationChannelConnection.class),
                                            customerConnection,
                                            mock(ScopusFileConverter.class));
        when(customerConnection.atLeastOneNvaCustomerPresent(any())).thenReturn(true);
        var candidate = converter.generateImportCandidate();

        assertThat(candidate.getEntityDescription().getMainTitle(), is(nullValue()));
    }

    @Test
    void shouldReturnImportCandidateWithoutTitleWhenTitleFromXmlIsNull() {
        var generator = new ScopusGenerator();
        setNullTitle(generator);
        var customerConnection = mock(NvaCustomerConnection.class);
        var converter = new ScopusConverter(generator.getDocument(), mock(PiaConnection.class),
                                            mock(CristinConnection.class), mock(PublicationChannelConnection.class),
                                            customerConnection,
                                            mock(ScopusFileConverter.class));
        when(customerConnection.atLeastOneNvaCustomerPresent(any())).thenReturn(true);
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
