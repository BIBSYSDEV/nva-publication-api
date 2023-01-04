package no.unit.nva.cristin.mapper.nva;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import no.unit.nva.cristin.CristinDataGenerator;
import no.unit.nva.cristin.mapper.CristinObject;
import no.unit.nva.model.contexttypes.Book;
import no.unit.nva.model.contexttypes.PublicationContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

public class NvaBookLikeBuilderTest {

    public static final String ONLY_VOLUME_EXPECTED = "^Volume:[^;]*$";
    public static final String ONLY_ISSUE_EXPECTED = "^Issue:[^;]*$";

    @ParameterizedTest(name = "nvaBookLikeBuilder returns Series that does contain blank String represetations"
                              + "when issue is blank")
    @NullAndEmptySource
    public void nvaBookLikeBuilderReturnsSeriesThatDoesNotContainBlankStringRepresentationsWhenIssueIsNull(
        String issueNumber) {
        CristinObject randomBook = CristinDataGenerator.randomBook();
        randomBook.getBookOrReportMetadata().setIssue(issueNumber);
        PublicationContext context = randomBook.toPublication()
                                         .getEntityDescription()
                                         .getReference()
                                         .getPublicationContext();
        Book book = (Book) context;
        assertThat(book.getSeriesNumber(), matchesPattern(ONLY_VOLUME_EXPECTED));
    }

    @ParameterizedTest(name = "nvaBookLikeBuilder returns Series that does contain blank String represetations"
                              + "when volume is blank")
    @NullAndEmptySource
    public void nvaBookLikeBuilderReturnsSeriesThatDoesNotContainBlankStringRepresentationsWhenVolumeIsNull(
        String volumeNumber) {
        CristinObject randomBook = CristinDataGenerator.randomBook();
        randomBook.getBookOrReportMetadata().setVolume(volumeNumber);
        PublicationContext context = randomBook.toPublication()
                                         .getEntityDescription()
                                         .getReference()
                                         .getPublicationContext();
        Book book = (Book) context;
        assertThat(book.getSeriesNumber(), matchesPattern(ONLY_ISSUE_EXPECTED));
    }
}