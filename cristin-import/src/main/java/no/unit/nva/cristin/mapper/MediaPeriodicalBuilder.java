package no.unit.nva.cristin.mapper;

import static no.unit.nva.cristin.mapper.nva.exceptions.ExceptionHandling.handlePublicationContextFailure;
import static nva.commons.core.attempt.Try.attempt;
import java.util.Optional;
import no.unit.nva.cristin.mapper.nva.CristinMappingModule;
import no.unit.nva.model.contexttypes.MediaContributionPeriodical;
import no.unit.nva.model.contexttypes.Periodical;
import no.unit.nva.model.contexttypes.PublicationContext;
import no.unit.nva.model.contexttypes.UnconfirmedMediaContributionPeriodical;

public class MediaPeriodicalBuilder extends CristinMappingModule {

    public MediaPeriodicalBuilder(CristinObject cristinObject) {
        super(cristinObject);
    }

    public PublicationContext buildMediaPeriodicalForPublicationContext() {
        return Optional.ofNullable(cristinObject)
                   .map(CristinObject::getJournalPublication)
                   .map(CristinJournalPublication::getJournal)
                   .map(CristinJournalPublicationJournal::getNsdCode)
                   .map(nsdCodeExists -> createMediaContributionPeriodical())
                   .orElseGet(this::createUnconfirmedMediaContributionPeriodical);
    }

    private Periodical createMediaContributionPeriodical() {
        Integer nsdCode = cristinObject.getJournalPublication().getJournal().getNsdCode();
        int publicationYear = extractYearReportedInNvi();
        var journalUri = new Nsd(nsdCode, publicationYear).createJournalOrSeriesUri();
        return new MediaContributionPeriodical(journalUri);
    }

    private Periodical createUnconfirmedMediaContributionPeriodical() {
        return attempt(() -> new UnconfirmedMediaContributionPeriodical(extractPublisherTitle(),
                                                                        extractIssn(),
                                                                        extractIssnOnline()))
                   .orElseThrow(failure -> handlePublicationContextFailure(failure.getException()));
    }
}
