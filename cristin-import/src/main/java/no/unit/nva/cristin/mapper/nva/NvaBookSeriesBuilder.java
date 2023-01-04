package no.unit.nva.cristin.mapper.nva;

import static java.util.Objects.nonNull;
import static no.unit.nva.cristin.mapper.nva.exceptions.ExceptionHandling.handlePublicationContextFailure;
import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import java.util.Optional;
import no.unit.nva.cristin.mapper.CristinBookOrReportMetadata;
import no.unit.nva.cristin.mapper.CristinJournalPublicationJournal;
import no.unit.nva.cristin.mapper.CristinObject;
import no.unit.nva.cristin.mapper.Nsd;
import no.unit.nva.model.contexttypes.BookSeries;
import no.unit.nva.model.contexttypes.Series;
import no.unit.nva.model.contexttypes.UnconfirmedSeries;

public class NvaBookSeriesBuilder extends CristinMappingModule {

    public NvaBookSeriesBuilder(CristinObject cristinObject) {
        super(cristinObject);
    }
    
    public BookSeries createBookSeries() {
        return Optional.of(cristinObject)
                   .map(CristinObject::getBookOrReportMetadata)
                   .map(CristinBookOrReportMetadata::getBookSeries)
                   .map(this::toNvaBookSeries)
                   .orElse(null);
    }

    private BookSeries toNvaBookSeries(CristinJournalPublicationJournal bookSeries) {
        if (nonNull(bookSeries.getNsdCode())) {
            return createConfirmedBookSeries(bookSeries);
        } else {
            return createUnconfirmedBookSeries(bookSeries);
        }
    }

    private BookSeries createUnconfirmedBookSeries(CristinJournalPublicationJournal bookSeries) {
        return attempt(
            () -> new UnconfirmedSeries(bookSeries.getJournalTitle(), bookSeries.getIssn(), bookSeries.getIssnOnline()))
                   .orElseThrow(failure -> handlePublicationContextFailure(failure.getException()));
    }

    private BookSeries createConfirmedBookSeries(CristinJournalPublicationJournal b) {
        int nsdCode = b.getNsdCode();
        int publicationYear = cristinObject.getPublicationYear();
        URI seriesUri = new Nsd(nsdCode, publicationYear).createJournalOrSeriesUri();
        return new Series(seriesUri);
    }
}
