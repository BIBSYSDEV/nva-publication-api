package no.unit.nva.cristin.mapper.nva;

import static java.util.Objects.nonNull;
import java.util.List;
import java.util.Optional;
import no.unit.nva.cristin.lambda.ErrorReport;
import no.unit.nva.cristin.mapper.CristinBookOrReportMetadata;
import no.unit.nva.cristin.mapper.CristinJournalPublicationJournal;
import no.unit.nva.cristin.mapper.CristinObject;
import no.unit.nva.cristin.mapper.Nsd;
import no.unit.nva.cristin.mapper.channelregistry.ChannelRegistryMapper;
import no.unit.nva.model.contexttypes.BookSeries;
import no.unit.nva.model.contexttypes.Series;
import no.unit.nva.model.contexttypes.UnconfirmedSeries;
import no.unit.nva.model.exceptions.InvalidIssnException;
import software.amazon.awssdk.services.s3.S3Client;

public class NvaBookSeriesBuilder extends CristinMappingModule {

    public NvaBookSeriesBuilder(CristinObject cristinObject, ChannelRegistryMapper channelRegistryMapper, S3Client s3Client) {
        super(cristinObject, channelRegistryMapper, s3Client);
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
        var issn = bookSeries.getIssn();
        var issnOnline = bookSeries.getIssnOnline();
        try {
            return new UnconfirmedSeries(bookSeries.getJournalTitle(), issn, issnOnline);
        } catch (InvalidIssnException e) {
            ErrorReport.exceptionName(e.getClass().getSimpleName())
                .withCristinId(cristinObject.getId())
                .withBody(String.join(":","ISSN", issn))
                .persist(s3Client);
            return null;
        }
    }

    private BookSeries createConfirmedBookSeries(CristinJournalPublicationJournal b) {
        int nsdCode = b.getNsdCode();
        int publicationYear = cristinObject.getPublicationYear();
        var seriesUri = new Nsd(nsdCode, publicationYear, List.of(), channelRegistryMapper, s3Client,
                                cristinObject.getId())
                            .createSeries();
        return nonNull(seriesUri) ? new Series(seriesUri) : null;
    }
}
