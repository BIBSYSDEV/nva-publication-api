package no.unit.nva.cristin.mapper.nva;

import static java.util.Objects.nonNull;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import no.unit.nva.cristin.lambda.ErrorReport;
import no.unit.nva.cristin.mapper.CristinBookOrReportMetadata;
import no.unit.nva.cristin.mapper.CristinJournalPublicationJournal;
import no.unit.nva.cristin.mapper.CristinObject;
import no.unit.nva.cristin.mapper.PublishingChannelEntryResolver;
import no.unit.nva.cristin.mapper.channelregistry.ChannelRegistryMapper;
import no.unit.nva.cristin.mapper.nva.exceptions.UnconfirmedSeriesException;
import no.unit.nva.model.contexttypes.BookSeries;
import no.unit.nva.model.contexttypes.Series;
import no.unit.nva.model.contexttypes.UnconfirmedSeries;
import no.unit.nva.model.exceptions.InvalidIssnException;
import software.amazon.awssdk.services.s3.S3Client;

public class NvaBookSeriesBuilder extends CristinMappingModule {

    protected static final String UNCONFIRMED_SERIES = "Unconfirmed series";

    public NvaBookSeriesBuilder(CristinObject cristinObject,
                                ChannelRegistryMapper channelRegistryMapper,
                                S3Client s3Client) {
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
        return Optional.ofNullable(createConfirmedBookSeries(bookSeries))
            .orElseGet(() -> createUnconfirmedBookSeries(bookSeries));
    }

    private BookSeries createUnconfirmedBookSeries(CristinJournalPublicationJournal bookSeries) {
        var issn = bookSeries.getIssn();
        var issnOnline = bookSeries.getIssnOnline();
        try {
            var journalTitle = bookSeries.getJournalTitle();
            var unconfirmedSeries = new UnconfirmedSeries(journalTitle, issn, issnOnline);
            ErrorReport.exceptionName(UnconfirmedSeriesException.name())
                .withCristinId(cristinObject.getId())
                .withBody(journalTitle)
                .persist(s3Client);
            return unconfirmedSeries;
        } catch (InvalidIssnException e) {
            ErrorReport.exceptionName(e.getClass().getSimpleName())
                .withCristinId(cristinObject.getId())
                .withBody(String.join(":","ISSN", issn))
                .persist(s3Client);
            return null;
        }
    }

    private BookSeries createConfirmedBookSeries(CristinJournalPublicationJournal b) {
        var nsdCode = Optional.ofNullable(b).map(CristinJournalPublicationJournal::getNsdCode).orElse(null);
        int publicationYear = cristinObject.getPublicationYear();
        var channelNames = nonNull(extractSeriesTitle()) ? List.of(extractSeriesTitle()) : List.<String>of();
        var seriesUri = new PublishingChannelEntryResolver(nsdCode, publicationYear, channelNames,
                                                           extractSeriesIssnList(),
                                                           channelRegistryMapper, s3Client,
                                                           cristinObject.getId())
                            .createSeries();
        return nonNull(seriesUri) ? new Series(seriesUri) : null;
    }

    private List<String> extractSeriesIssnList() {
        return Stream.of(extractSeriesPrintIssn(), extractSeriesOnlineIssn()).filter(Objects::nonNull).toList();
    }
}
