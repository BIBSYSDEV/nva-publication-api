package no.unit.nva.cristin.mapper;

import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.book.BookAnthology;
import no.unit.nva.model.instancetypes.book.BookMonograph;
import no.unit.nva.model.instancetypes.chapter.ChapterArticle;
import no.unit.nva.model.instancetypes.degree.DegreeMaster;
import no.unit.nva.model.instancetypes.degree.DegreePhd;
import no.unit.nva.model.instancetypes.journal.FeatureArticle;
import no.unit.nva.model.instancetypes.journal.JournalArticle;
import no.unit.nva.model.instancetypes.journal.JournalCorrigendum;
import no.unit.nva.model.instancetypes.journal.JournalLeader;
import no.unit.nva.model.instancetypes.journal.JournalLetter;
import no.unit.nva.model.instancetypes.journal.JournalReview;
import no.unit.nva.model.instancetypes.report.ReportResearch;
import no.unit.nva.model.pages.MonographPages;
import no.unit.nva.model.pages.Pages;
import no.unit.nva.model.pages.Range;

import static no.unit.nva.cristin.lambda.constants.HardcodedValues.HARDCODED_BOOK_PEER_REVIEWED;
import static no.unit.nva.cristin.lambda.constants.HardcodedValues.HARDCODED_BOOK_TEXTBOOK_CONTENT;
import static no.unit.nva.cristin.lambda.constants.HardcodedValues.HARDCODED_JOURNAL_PEER_REVIEWED;
import static no.unit.nva.cristin.mapper.CristinMainCategory.isBook;
import static no.unit.nva.cristin.mapper.CristinMainCategory.isChapter;
import static no.unit.nva.cristin.mapper.CristinMainCategory.isJournal;
import static no.unit.nva.cristin.mapper.CristinMainCategory.isReport;
import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.isChapterArticle;
import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.isDegreeMaster;
import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.isDegreePhd;
import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.isFeatureArticle;
import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.isJournalArticle;
import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.isJournalCorrigendum;
import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.isJournalLeader;
import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.isJournalLetter;
import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.isJournalReview;
import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.isMonograph;
import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.isResearchReport;

@SuppressWarnings("PMD.GodClass")
public class PublicationInstanceBuilder {

    public static final String ERROR_PARSING_SECONDARY_CATEGORY = "Error parsing secondary category";
    public static final String ERROR_PARSING_MAIN_CATEGORY = "Error parsing main category";
    public static final String ERROR_PARSING_MAIN_OR_SECONDARY_CATEGORIES = "Error parsing main or secondary "
            + "categories";

    private final CristinObject cristinObject;
    private final CristinMapper mapper;

    public PublicationInstanceBuilder(CristinObject cristinObject) {
        this.cristinObject = cristinObject;
        this.mapper = new CristinMapper(cristinObject);
    }

    public PublicationInstance<? extends Pages> buildPublicationInstance() {
        if (isBook(cristinObject) && CristinSecondaryCategory.isAnthology(cristinObject)) {
            return createBookAnthology();
        } else if (isBook(cristinObject) && isMonograph(cristinObject)) {
            return createBookMonograph();
        } else if (isJournal(cristinObject) && isFeatureArticle(cristinObject)) {
            return createFeatureArticle();
        } else if (isJournal(cristinObject) && isJournalLetter(cristinObject)) {
            return createJournalLetter();
        } else if (isJournal(cristinObject) && isJournalLeader(cristinObject)) {
            return createJournalLeader();
        } else if (isJournal(cristinObject) && isJournalReview(cristinObject)) {
            return createJournalReview();
        } else if (isJournal(cristinObject) && isJournalCorrigendum(cristinObject)) {
            return createJournalCorrigendum();
        } else if (isJournal(cristinObject) && isJournalArticle(cristinObject)) {
            return createJournalArticle();
        } else if (isReport(cristinObject) && isResearchReport(cristinObject)) {
            return createReportResearch();
        } else if (isReport(cristinObject) && isDegreePhd(cristinObject)) {
            return createDegreePhd();
        } else if (isReport(cristinObject) && isDegreeMaster(cristinObject)) {
            return createDegreeMaster();
        } else if (isChapter(cristinObject) && isChapterArticle(cristinObject)) {
            return createChapterArticle();
        } else if (cristinObject.getMainCategory().isUnknownCategory()) {
            throw new UnsupportedOperationException(ERROR_PARSING_MAIN_CATEGORY);
        } else if (cristinObject.getSecondaryCategory().isUnknownCategory()) {
            throw new UnsupportedOperationException(ERROR_PARSING_SECONDARY_CATEGORY);
        }
        throw new RuntimeException(ERROR_PARSING_MAIN_OR_SECONDARY_CATEGORIES);
    }

    private MonographPages createMonographPages() {
        return new MonographPages.Builder()
                .withPages(extractNumberOfPages())
                .build();
    }

    private BookAnthology createBookAnthology() {
        return new BookAnthology.Builder()
                .withPeerReviewed(HARDCODED_BOOK_PEER_REVIEWED)
                .withPages(createMonographPages())
                .withTextbookContent(HARDCODED_BOOK_TEXTBOOK_CONTENT)
                .build();
    }

    private BookMonograph createBookMonograph() {
        return new BookMonograph.Builder()
                .withPeerReviewed(HARDCODED_BOOK_PEER_REVIEWED)
                .withPages(createMonographPages())
                .withTextbookContent(HARDCODED_BOOK_TEXTBOOK_CONTENT)
                .build();
    }

    private PublicationInstance<? extends Pages> createFeatureArticle() {
        Range numberOfPages = new Range(extractPagesBegin(), extractPagesEnd());
        return new FeatureArticle.Builder()
                .withPages(numberOfPages)
                .withVolume(extractVolume())
                .build();
    }

    private PublicationInstance<? extends Pages> createJournalLetter() {
        Range numberOfPages = new Range(extractPagesBegin(), extractPagesEnd());
        return new JournalLetter.Builder()
                .withPages(numberOfPages)
                .withVolume(extractVolume())
                .build();
    }

    private PublicationInstance<? extends Pages> createJournalReview() {
        Range numberOfPages = new Range(extractPagesBegin(), extractPagesEnd());
        return new JournalReview.Builder()
                .withPages(numberOfPages)
                .withVolume(extractVolume())
                .build();
    }

    private PublicationInstance<? extends Pages> createJournalLeader() {
        Range numberOfPages = new Range(extractPagesBegin(), extractPagesEnd());
        return new JournalLeader.Builder()
                .withPages(numberOfPages)
                .withVolume(extractVolume())
                .build();
    }

    private PublicationInstance<? extends Pages> createJournalCorrigendum() {
        Range numberOfPages = new Range(extractPagesBegin(), extractPagesEnd());
        return new JournalCorrigendum.Builder()
                .withPages(numberOfPages)
                .withVolume(extractVolume())
                .build();
    }

    private PublicationInstance<? extends Pages> createJournalArticle() {
        Range numberOfPages = new Range(extractPagesBegin(), extractPagesEnd());
        return new JournalArticle.Builder()
                .withContent(cristinObject.getSecondaryCategory().toJournalArticleContentType())
                .withPages(numberOfPages)
                .withPeerReviewed(HARDCODED_JOURNAL_PEER_REVIEWED)
                .withVolume(extractVolume())
                .build();
    }

    private PublicationInstance<? extends Pages> createReportResearch() {
        return new ReportResearch.Builder().build();
    }

    private PublicationInstance<? extends Pages> createDegreePhd() {
        return new DegreePhd
                .Builder()
                .withPages(createMonographPages())
                .build();
    }

    private PublicationInstance<? extends Pages> createDegreeMaster() {
        return new DegreeMaster
                .Builder()
                .withPages(createMonographPages())
                .build();
    }

    private PublicationInstance<? extends Pages> createChapterArticle() {
        return new ChapterArticle.Builder().build();
    }

    private String extractPagesBegin() {
        return mapper.extractCristinJournalPublication().getPagesBegin();
    }

    private String extractPagesEnd() {
        return mapper.extractCristinJournalPublication().getPagesEnd();
    }

    private String extractVolume() {
        return mapper.extractCristinJournalPublication().getVolume();
    }

    private String extractNumberOfPages() {
        return mapper.extractCristinBookReport().getNumberOfPages();
    }

}
