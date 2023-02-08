package no.unit.nva.cristin.mapper;

import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.isFeatureArticle;
import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.isJournalArticle;
import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.isJournalCorrigendum;
import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.isJournalLeader;
import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.isJournalLetter;
import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.isJournalReview;
import java.util.Set;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.journal.AcademicArticle;
import no.unit.nva.model.instancetypes.journal.AcademicLiteratureReview;
import no.unit.nva.model.instancetypes.journal.FeatureArticle;
import no.unit.nva.model.instancetypes.journal.JournalCorrigendum;
import no.unit.nva.model.instancetypes.journal.JournalLeader;
import no.unit.nva.model.instancetypes.journal.JournalLetter;
import no.unit.nva.model.instancetypes.journal.JournalReview;
import no.unit.nva.model.instancetypes.journal.PopularScienceArticle;
import no.unit.nva.model.instancetypes.journal.ProfessionalArticle;
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
    protected Set<CristinMainCategory> getExpectedType() {
        return Set.of(CristinMainCategory.JOURNAL);
    }

    private PublicationInstance<? extends Pages> createFeatureArticle() {
        Range numberOfPages = new Range(extractPagesBegin(), extractPagesEnd());
        return new FeatureArticle.Builder()
                   .withPages(numberOfPages)
                   .withVolume(extractVolume())
                   .withIssue(extractIssue())
                   .build();
    }

    private PublicationInstance<? extends Pages> createJournalLetter() {
        Range numberOfPages = new Range(extractPagesBegin(), extractPagesEnd());
        return new JournalLetter.Builder()
                   .withPages(numberOfPages)
                   .withIssue(extractIssue())
                   .withVolume(extractVolume())
                   .build();
    }

    private PublicationInstance<? extends Pages> createJournalReview() {
        Range numberOfPages = new Range(extractPagesBegin(), extractPagesEnd());
        return new JournalReview.Builder()
                   .withPages(numberOfPages)
                   .withIssue(extractIssue())
                   .withVolume(extractVolume())
                   .build();
    }

    private PublicationInstance<? extends Pages> createJournalLeader() {
        Range numberOfPages = new Range(extractPagesBegin(), extractPagesEnd());
        return new JournalLeader.Builder()
                   .withPages(numberOfPages)
                   .withIssue(extractIssue())
                   .withVolume(extractVolume())
                   .build();
    }

    private PublicationInstance<? extends Pages> createJournalCorrigendum() {
        Range numberOfPages = new Range(extractPagesBegin(), extractPagesEnd());
        return new JournalCorrigendum.Builder()
                   .withPages(numberOfPages)
                   .withIssue(extractIssue())
                   .withVolume(extractVolume())
                   .build();
    }

    private PublicationInstance<? extends Pages> createJournalArticle() {
        Range numberOfPages = new Range(extractPagesBegin(), extractPagesEnd());

        var secondaryCategory = getCristinObject().getSecondaryCategory();
        if (CristinSecondaryCategory.JOURNAL_ARTICLE.equals(secondaryCategory)) {
            return new ProfessionalArticle(numberOfPages, extractVolume(), extractIssue(), null);
        } else if (CristinSecondaryCategory.POPULAR_ARTICLE.equals(secondaryCategory)) {
            return new PopularScienceArticle(numberOfPages, extractVolume(), extractIssue(), null);
        } else if (CristinSecondaryCategory.ARTICLE.equals(secondaryCategory)) {
            return new AcademicArticle(numberOfPages, extractVolume(), extractIssue(), null);
        } else if (CristinSecondaryCategory.ACADEMIC_REVIEW.equals(secondaryCategory)) {
            return new AcademicLiteratureReview(numberOfPages, extractVolume(), extractIssue(), null);
        } else if (CristinSecondaryCategory.SHORT_COMMUNICATION.equals(secondaryCategory)) {
            return new AcademicArticle(numberOfPages, extractVolume(), extractIssue(), null);
        } else {
            throw new UnsupportedOperationException("The journal article subtype is unknown");
        }
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

    private String extractIssue() {
        return getCristinObject().getJournalPublication().getIssue();
    }
}
