package no.unit.nva.cristin.mapper.nva;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.mockito.Mockito.mock;
import no.unit.nva.auth.uriretriever.UriRetriever;
import no.unit.nva.cristin.CristinDataGenerator;
import no.unit.nva.cristin.mapper.CristinMapper;
import no.unit.nva.cristin.mapper.CristinObject;
import no.unit.nva.model.Publication;
import no.unit.nva.model.contexttypes.Book;
import no.unit.nva.publication.model.utils.CustomerService;
import no.unit.nva.publication.utils.CristinUnitsUtilImpl;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import software.amazon.awssdk.services.s3.S3Client;

public class NvaBookLikeBuilderTest {

    public static final String ONLY_VOLUME_EXPECTED = "^[^;]*$";
    public static final String ONLY_ISSUE_EXPECTED = "^[^;]*$";

    @ParameterizedTest(name = "nvaBookLikeBuilder returns Series that does contain blank String represetations"
                              + "when issue is blank")
    @NullAndEmptySource
    public void nvaBookLikeBuilderReturnsSeriesThatDoesNotContainBlankStringRepresentationsWhenIssueIsNull(
        String issueNumber) {
        CristinObject randomBook = CristinDataGenerator.randomBook();
        randomBook.getBookOrReportMetadata().setIssue(issueNumber);
        var context = mapToPublication(randomBook)
                                         .getEntityDescription()
                                         .getReference()
                                         .getPublicationContext();
        Book book = (Book) context;
        assertThat(book.getSeriesNumber(), matchesPattern(ONLY_VOLUME_EXPECTED));
    }

    private static Publication mapToPublication(CristinObject randomBook) {
        var cristinUnitsUtil = mock(CristinUnitsUtilImpl.class);
        return new CristinMapper(randomBook, cristinUnitsUtil, mock(S3Client.class), mock(UriRetriever.class),
                                 mock(CustomerService.class)).generatePublication();
    }

    @ParameterizedTest(name = "nvaBookLikeBuilder returns Series that does contain blank String represetations"
                              + "when volume is blank")
    @NullAndEmptySource
    public void nvaBookLikeBuilderReturnsSeriesThatDoesNotContainBlankStringRepresentationsWhenVolumeIsNull(
        String volumeNumber) {
        CristinObject randomBook = CristinDataGenerator.randomBook();
        randomBook.getBookOrReportMetadata().setVolume(volumeNumber);
        var context = mapToPublication(randomBook)
                                         .getEntityDescription()
                                         .getReference()
                                         .getPublicationContext();
        Book book = (Book) context;
        assertThat(book.getSeriesNumber(), matchesPattern(ONLY_ISSUE_EXPECTED));
    }
}