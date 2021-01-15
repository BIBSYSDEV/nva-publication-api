package no.unit.nva.publication.doi.dto;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PublicationTypeTest {

    public static final String UNDEFINED = "UNKNOWN";
    public static final String NO_ENUM_CONSTANT_FOR = "No enum constant for";

    @ParameterizedTest
    @CsvSource({
        "JOURNAL_ARTICLE,JournalArticle",
        "JOURNAL_LETTER,JournalLetter",
        "JOURNAL_LEADER,JournalLeader",
        "JOURNAL_REVIEW,JournalReview",
        "JOURNAL_SHORT_COMMUNICATION,JournalShortCommunication",
        "BOOK_MONOGRAPH,BookMonograph",
        "BOOK_ANTHOLOGY,BookAnthology",
        "DEGREE_BACHELOR,DegreeBachelor",
        "DEGREE_MASTER,DegreeMaster",
        "DEGREE_PHD,DegreePhd",
        "REPORT_BASIC,ReportBasic",
        "REPORT_POLICY,ReportPolicy",
        "REPORT_RESEARCH,ReportResearch",
        "REPORT_WORKING_PAPER,ReportWorkingPaper",
        "CHAPTER_ARTICLE,ChapterArticle"
    })
    void toStringReturnsCorrespondingNameValue(PublicationType enumValue, String expectedToString) {
        assertThat(enumValue.toString(), is(equalTo(expectedToString)));
    }

    @ParameterizedTest
    @CsvSource({
        "JOURNAL_ARTICLE,JournalArticle",
        "JOURNAL_LETTER,JournalLetter",
        "JOURNAL_LEADER,JournalLeader",
        "JOURNAL_REVIEW,JournalReview",
        "JOURNAL_SHORT_COMMUNICATION,JournalShortCommunication",
        "BOOK_MONOGRAPH,BookMonograph",
        "BOOK_ANTHOLOGY,BookAnthology",
        "DEGREE_BACHELOR,DegreeBachelor",
        "DEGREE_MASTER,DegreeMaster",
        "DEGREE_PHD,DegreePhd",
        "REPORT_BASIC,ReportBasic",
        "REPORT_POLICY,ReportPolicy",
        "REPORT_RESEARCH,ReportResearch",
        "REPORT_WORKING_PAPER,ReportWorkingPaper",
        "CHAPTER_ARTICLE,ChapterArticle"
    })
    void findByNameReturnsPublicationTyupeWithCorrespondingNameValue(PublicationType expectedEnum,
                                                                     String findByNameValue) {
        assertThat(PublicationType.findByName(findByNameValue), is(equalTo(expectedEnum)));
    }

    @Test
    void findByNameThrowsExceptionWhenUnknownTypeInserted() {
        var actualException = assertThrows(IllegalArgumentException.class,
            () -> PublicationType.findByName(UNDEFINED));
        assertThat(actualException.getMessage(), containsString(NO_ENUM_CONSTANT_FOR));
    }
}