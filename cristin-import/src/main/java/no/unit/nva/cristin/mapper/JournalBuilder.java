package no.unit.nva.cristin.mapper;

import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.isAbstract;
import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.isJournalArticle;
import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.isJournalCorrigendum;
import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.isJournalLeader;
import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.isJournalLetter;
import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.isJournalReview;
import java.util.Optional;
import java.util.Set;
import no.unit.nva.cristin.mapper.nva.exceptions.UnsupportedSecondaryCategoryException;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.journal.AcademicArticle;
import no.unit.nva.model.instancetypes.journal.AcademicLiteratureReview;
import no.unit.nva.model.instancetypes.journal.ConferenceAbstract;
import no.unit.nva.model.instancetypes.journal.JournalCorrigendum;
import no.unit.nva.model.instancetypes.journal.JournalLeader;
import no.unit.nva.model.instancetypes.journal.JournalLetter;
import no.unit.nva.model.instancetypes.journal.JournalReview;
import no.unit.nva.model.instancetypes.journal.PopularScienceArticle;
import no.unit.nva.model.instancetypes.journal.ProfessionalArticle;
import no.unit.nva.model.pages.Pages;
import no.unit.nva.model.pages.Range;
import software.amazon.awssdk.services.s3.S3Client;

public class JournalBuilder extends AbstractPublicationInstanceBuilder {

    public JournalBuilder(CristinObject cristinObject, S3Client s3Client) {
        super(cristinObject, s3Client);
    }

    @Override
    public PublicationInstance<? extends Pages> build() {
        if (isJournalLetter(getCristinObject())) {
            return createJournalLetter();
        } else if (isJournalLeader(getCristinObject())) {
            return createJournalLeader();
        } else if (isJournalReview(getCristinObject())) {
            return createJournalReview();
        } else if (isJournalCorrigendum(getCristinObject())) {
            return createJournalCorrigendum();
        } else if (isJournalArticle(getCristinObject())) {
            return createJournalArticle();
        } else if (isAbstract(getCristinObject())) {
            return createAbstract();
        } else {
            throw unknownSecondaryCategory();
        }
    }

    private PublicationInstance<? extends Pages> createAbstract() {
        return new ConferenceAbstract(extractVolume(), extractIssue(), extractArticleNumber(),
                                      new Range(extractPagesBegin(), extractPagesEnd()));
    }

    @Override
    protected Set<CristinMainCategory> getExpectedType() {
        return Set.of(CristinMainCategory.JOURNAL);
    }

    private PublicationInstance<? extends Pages> createJournalLetter() {
        Range numberOfPages = new Range(extractPagesBegin(), extractPagesEnd());
        return new JournalLetter.Builder()
                   .withPages(numberOfPages)
                   .withIssue(extractIssue())
                   .withVolume(extractVolume())
                   .withArticleNumber(extractArticleNumber())
                   .build();
    }

    private PublicationInstance<? extends Pages> createJournalReview() {
        Range numberOfPages = new Range(extractPagesBegin(), extractPagesEnd());
        return new JournalReview.Builder()
                   .withPages(numberOfPages)
                   .withIssue(extractIssue())
                   .withVolume(extractVolume())
                   .withArticleNumber(extractArticleNumber())
                   .build();
    }

    private PublicationInstance<? extends Pages> createJournalLeader() {
        Range numberOfPages = new Range(extractPagesBegin(), extractPagesEnd());
        return new JournalLeader.Builder()
                   .withPages(numberOfPages)
                   .withIssue(extractIssue())
                   .withVolume(extractVolume())
                   .withArticleNumber(extractArticleNumber())
                   .build();
    }

    private PublicationInstance<? extends Pages> createJournalCorrigendum() {
        Range numberOfPages = new Range(extractPagesBegin(), extractPagesEnd());
        return new JournalCorrigendum.Builder()
                   .withPages(numberOfPages)
                   .withIssue(extractIssue())
                   .withVolume(extractVolume())
                   .withArticleNumber(extractArticleNumber())
                   .build();
    }

    private PublicationInstance<? extends Pages> createJournalArticle() {
        Range numberOfPages = new Range(extractPagesBegin(), extractPagesEnd());

        var secondaryCategory = getCristinObject().getSecondaryCategory();
        if (CristinSecondaryCategory.JOURNAL_ARTICLE.equals(secondaryCategory)) {
            return new ProfessionalArticle(numberOfPages, extractVolume(), extractIssue(), extractArticleNumber());
        } else if (CristinSecondaryCategory.POPULAR_ARTICLE.equals(secondaryCategory)) {
            return new PopularScienceArticle(numberOfPages, extractVolume(), extractIssue(), extractArticleNumber());
        } else if (CristinSecondaryCategory.ARTICLE.equals(secondaryCategory)) {
            return new AcademicArticle(numberOfPages, extractVolume(), extractIssue(), extractArticleNumber());
        } else if (CristinSecondaryCategory.ACADEMIC_REVIEW.equals(secondaryCategory)) {
            return new AcademicLiteratureReview(numberOfPages, extractVolume(), extractIssue(), extractArticleNumber());
        } else if (CristinSecondaryCategory.SHORT_COMMUNICATION.equals(secondaryCategory)) {
            return new AcademicArticle(numberOfPages, extractVolume(), extractIssue(), extractArticleNumber());
        } else {
            throw new UnsupportedSecondaryCategoryException();
        }
    }

    private String extractPagesBegin() {
        return Optional.ofNullable(getCristinObject())
                   .map(CristinObject::getJournalPublication)
                   .map(CristinJournalPublication::getPagesBegin)
                   .orElse(null);
    }

    private String extractPagesEnd() {
        return Optional.ofNullable(getCristinObject())
                   .map(CristinObject::getJournalPublication)
                   .map(CristinJournalPublication::getPagesEnd)
                   .orElse(null);
    }

    private String extractVolume() {
        return Optional.ofNullable(getCristinObject())
                   .map(CristinObject::getJournalPublication)
                   .map(CristinJournalPublication::getVolume)
                   .orElse(null);
    }

    private String extractIssue() {
        return Optional.ofNullable(getCristinObject())
                   .map(CristinObject::getJournalPublication)
                   .map(CristinJournalPublication::getIssue)
                   .orElse(null);
    }

    private String extractArticleNumber() {
        return Optional.ofNullable(getCristinObject())
                   .map(CristinObject::getJournalPublication)
                   .map(CristinJournalPublication::getArticleNumber)
                   .orElse(null);
    }
}
