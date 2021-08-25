package no.unit.nva.cristin.mapper;

import static no.unit.nva.cristin.lambda.constants.HardcodedValues.HARDCODED_JOURNAL_PEER_REVIEWED;
import static no.unit.nva.cristin.mapper.CristinMainCategory.isJournal;
import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.isFeatureArticle;
import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.isJournalArticle;
import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.isJournalCorrigendum;
import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.isJournalLeader;
import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.isJournalLetter;
import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.isJournalReview;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.journal.FeatureArticle;
import no.unit.nva.model.instancetypes.journal.JournalArticle;
import no.unit.nva.model.instancetypes.journal.JournalCorrigendum;
import no.unit.nva.model.instancetypes.journal.JournalLeader;
import no.unit.nva.model.instancetypes.journal.JournalLetter;
import no.unit.nva.model.instancetypes.journal.JournalReview;
import no.unit.nva.model.pages.Pages;
import no.unit.nva.model.pages.Range;

public class JournalBuilder implements PublicationInstanceBuilder {

    public static final String MAIN_CATEGORY_JOURNAL = "Journal (TIDSSKRIFTPUBL)";

    private final CristinObject cristinObject;

    public JournalBuilder(CristinObject cristinObject) {
        if (!isJournal(cristinObject)) {
            throw new IllegalStateException(
                    String.format(ERROR_NOT_CORRECT_TYPE, this.getClass().getSimpleName(), MAIN_CATEGORY_JOURNAL)
            );
        }
        this.cristinObject = cristinObject;
    }

    @Override
    public PublicationInstance<? extends Pages> build() {
        if (isFeatureArticle(cristinObject)) {
            return createFeatureArticle();
        } else if (isJournalLetter(cristinObject)) {
            return createJournalLetter();
        } else if (isJournalLeader(cristinObject)) {
            return createJournalLeader();
        } else if (isJournalReview(cristinObject)) {
            return createJournalReview();
        } else if (isJournalCorrigendum(cristinObject)) {
            return createJournalCorrigendum();
        } else if (isJournalArticle(cristinObject)) {
            return createJournalArticle();
        } else {
            throw unknownSecondaryCategory();
        }
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

    private String extractPagesBegin() {
        return cristinObject.getJournalPublication().getPagesBegin();
    }

    private String extractPagesEnd() {
        return cristinObject.getJournalPublication().getPagesEnd();
    }

    private String extractVolume() {
        return cristinObject.getJournalPublication().getVolume();
    }
}
