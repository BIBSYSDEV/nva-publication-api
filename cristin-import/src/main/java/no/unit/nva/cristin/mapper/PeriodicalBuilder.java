package no.unit.nva.cristin.mapper;

import static no.unit.nva.cristin.mapper.nva.exceptions.ExceptionHandling.handlePublicationContextFailure;
import static nva.commons.core.attempt.Try.attempt;
import java.util.Optional;
import no.unit.nva.cristin.mapper.nva.CristinMappingModule;
import no.unit.nva.model.contexttypes.Journal;
import no.unit.nva.model.contexttypes.Periodical;
import no.unit.nva.model.contexttypes.UnconfirmedJournal;

public class PeriodicalBuilder extends CristinMappingModule {

    private final CristinObject cristinObject;

    public PeriodicalBuilder(CristinObject cristinObject) {
        super(cristinObject);
        this.cristinObject = cristinObject;
    }

    public Periodical buildPeriodicalForPublicationContext() {
        return Optional.ofNullable(cristinObject)
                   .map(CristinObject::getJournalPublication)
                   .map(CristinJournalPublication::getJournal)
                   .map(CristinJournalPublicationJournal::getNsdCode)
                   .map(nsdCodeExists -> createJournal())
                   .orElseGet(this::createUnconfirmedJournal);
    }

    private Periodical createUnconfirmedJournal() {
        return attempt(() -> new UnconfirmedJournal(extractPublisherTitle(), extractIssn(), extractIssnOnline()))
                   .orElseThrow(failure -> handlePublicationContextFailure(failure.getException()));
    }

    private Periodical createJournal() {
        Integer nsdCode = cristinObject.getJournalPublication().getJournal().getNsdCode();
        int publicationYear = extractYearReportedInNvi();
        var journalUri = new Nsd(nsdCode, publicationYear).createJournalOrSeriesUri();
        return new Journal(journalUri);
    }
}
