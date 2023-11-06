package no.unit.nva.cristin.mapper;

import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.isInterview;
import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.isMediaFeatureArticle;
import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.isProgramParticipation;
import java.util.Set;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.media.MediaFeatureArticle;
import no.unit.nva.model.instancetypes.media.MediaInterview;
import no.unit.nva.model.instancetypes.media.MediaParticipationInRadioOrTv;
import no.unit.nva.model.pages.Pages;
import no.unit.nva.model.pages.Range;

public class MediaContributionBuilder extends AbstractPublicationInstanceBuilder {

    public MediaContributionBuilder(CristinObject cristinObject) {
        super(cristinObject);
    }

    @Override
    public PublicationInstance<? extends Pages> build() {
        if (isInterview(getCristinObject())) {
            return createMediaInterview();
        } else if (isProgramParticipation(getCristinObject())) {
            return createProgramParticipation();
        } else if (isMediaFeatureArticle(getCristinObject())) {
            return createMediaFeatureArticle();
        } else {
            throw unknownSecondaryCategory();
        }
    }

    private MediaFeatureArticle createMediaFeatureArticle() {
        Range numberOfPages = new Range(extractPagesBegin(), extractPagesEnd());
        return new MediaFeatureArticle(extractVolume(), extractIssue(), extractArticleNumber(),  numberOfPages);
    }

    @Override
    protected Set<CristinMainCategory> getExpectedType() {
        return Set.of(CristinMainCategory.MEDIA_CONTRIBUTION, CristinMainCategory.JOURNAL);
    }

    private static MediaInterview createMediaInterview() {
        return new MediaInterview();
    }

    private PublicationInstance<? extends Pages> createProgramParticipation() {
        return new MediaParticipationInRadioOrTv();
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

    private String extractArticleNumber() {
        return getCristinObject().getJournalPublication().getArticleNumber();
    }
}
