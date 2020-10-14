package no.unit.nva.publication.doi.dto;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PublicationTypeTest {

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
        "REPORT,Report",
        "REPORT_POLICY,ReportPolicy",
        "REPORT_RESEARCH,ReportResearch",
        "REPORT_WORKING_PAPER,ReportWorkingPaper",
        "CHAPTER_ARTICLE,ChapterArticle"
    })
    void testToString(PublicationType enumValue, String expectedToString) {
        assertThat(enumValue.toString(), is(equalTo(expectedToString)));
    }
}