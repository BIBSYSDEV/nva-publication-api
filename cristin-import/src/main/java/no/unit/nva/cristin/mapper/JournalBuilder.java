package no.unit.nva.cristin.mapper;

import static no.unit.nva.cristin.lambda.constants.HardcodedValues.HARDCODED_JOURNAL_PEER_REVIEWED;
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

public class JournalBuilder extends AbstractPublicationInstanceBuilder {

    public JournalBuilder(CristinObject cristinObject) {
        super(cristinObject);
    }

    @Override
    public PublicationInstance<? extends Pages> build() {
        if (isFeatureArticle(getCristinObject())) {
            return createFeatureArticle();
        } else if (isJournalLetter(getCristinObject())) {
            return createJournalLetter();
        } else if (isJournalLeader(getCristinObject())) {
            return createJournalLeader();
        } else if (isJournalReview(getCristinObject())) {
            return createJournalReview();
        } else if (isJournalCorrigendum(getCristinObject())) {
            return createJournalCorrigendum();
        } else if (isJournalArticle(getCristinObject())) {
            return createJournalArticle();
        } else {
            throw unknownSecondaryCategory();
        }
    }

    @Override
    protected CristinMainCategory getExpectedType() {
        return CristinMainCategory.JOURNAL;
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
            .withContent(getCristinObject().getSecondaryCategory().toJournalArticleContentType())
            .withPages(numberOfPages)
            .withPeerReviewed(HARDCODED_JOURNAL_PEER_REVIEWED)
            .withVolume(extractVolume())
            .build();
    }

    private String extractPagesBegin() {
        return getCristinObject().getJournalPublication().getPagesBegin();
    }

    private String extractPagesEnd() {
        return getCristinObject().getJournalPublication().getPagesEnd();
    }

    private String extractVolume() {
        return getCristinObject().getJournalPublication().getVolume();
    }
}
