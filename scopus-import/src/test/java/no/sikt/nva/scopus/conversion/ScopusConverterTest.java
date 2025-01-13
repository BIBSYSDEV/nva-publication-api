package no.sikt.nva.scopus.conversion;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import no.scopus.generated.OpenAccessType;
import no.sikt.nva.scopus.ScopusConverter;
import no.sikt.nva.scopus.conversion.files.ScopusFileConverter;
import no.sikt.nva.scopus.utils.ScopusGenerator;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.publication.model.business.importcandidate.ImportCandidate;
import org.junit.jupiter.api.Test;

public class ScopusConverterTest {

    @Test
    void shouldReturnImportCandidateWithoutTitleWhenTitleListFromScopusXmlISEmpty() {
        var generator = new ScopusGenerator();
        generator.getDocument().getItem().getItem().getBibrecord().getHead().getCitationTitle().getTitletext().clear();
        var candidate = generateImportCandidate(generator);

        assertThat(candidate.getEntityDescription().getMainTitle(), is(nullValue()));
    }

    @Test
    void shouldReturnImportCandidateWithoutTitleWhenTitleFromXmlIsNull() {
        var generator = new ScopusGenerator();
        setNullTitle(generator);
        var candidate = generateImportCandidate(generator);

        assertThat(candidate.getEntityDescription().getMainTitle(), is(nullValue()));
    }

    @Test
    void shouldReturnImportCandidateWithPublicationDateFromOaAccessEffectiveDateWhenDateIsPresent() {
        var generator = new ScopusGenerator();
        generator.getDocument().getMeta().setOpenAccess(new OpenAccessType());
        generator.getDocument().getMeta().getOpenAccess().setOaAccessEffectiveDate("2024-10-16");

        var candidate = generateImportCandidate(generator);

        var expectedPublicationDate = new PublicationDate.Builder().withYear("2024").withMonth("10").withDay("16").build();

        assertEquals(expectedPublicationDate, candidate.getEntityDescription().getPublicationDate());
    }

    @Test
    void shouldReturnImportCandidateWithPublicationDateFromDateSortWhenOaAccessEffectiveDateIsMissingDateParts() {
        var generator = new ScopusGenerator();
        generator.getDocument().getMeta().setOpenAccess(new OpenAccessType());
        generator.getDocument().getMeta().getOpenAccess().setOaAccessEffectiveDate("2024");

        var candidate = generateImportCandidate(generator);

        var dateSort = generator.getDocument().getItem().getItem().getProcessInfo().getDateSort();
        var expectedPublicationDate =
            new PublicationDate.Builder().withYear(dateSort.getYear()).withMonth(dateSort.getMonth()).withDay(dateSort.getDay()).build();

        assertEquals(expectedPublicationDate, candidate.getEntityDescription().getPublicationDate());
    }

    @Test
    void shouldReturnImportCandidateFromDateSortWhenOaAccessEffectiveDateIsNotPresent() {
        var generator = new ScopusGenerator();

        var candidate = generateImportCandidate(generator);
        var dateSort = generator.getDocument().getItem().getItem().getProcessInfo().getDateSort();
        var expectedPublicationDate =
            new PublicationDate.Builder().withYear(dateSort.getYear()).withMonth(dateSort.getMonth()).withDay(dateSort.getDay()).build();

        assertEquals(expectedPublicationDate, candidate.getEntityDescription().getPublicationDate());
    }

    private static ImportCandidate generateImportCandidate(ScopusGenerator generator) {
        var customerConnection = mock(NvaCustomerConnection.class);
        var converter = new ScopusConverter(generator.getDocument(), mock(PiaConnection.class),
                                            mock(CristinConnection.class), mock(PublicationChannelConnection.class),
                                            customerConnection,
                                            mock(ScopusFileConverter.class));
        when(customerConnection.atLeastOneNvaCustomerPresent(any())).thenReturn(true);
        var candidate = converter.generateImportCandidate();
        return candidate;
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
