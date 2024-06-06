package no.unit.nva.cristin.mapper.nva;

import static java.util.Objects.isNull;
import java.util.Optional;
import no.unit.nva.cristin.mapper.CristinBookOrReportMetadata;
import no.unit.nva.cristin.mapper.CristinBookOrReportPartMetadata;
import no.unit.nva.cristin.mapper.CristinJournalPublication;
import no.unit.nva.cristin.mapper.CristinJournalPublicationJournal;
import no.unit.nva.cristin.mapper.CristinObject;
import no.unit.nva.cristin.mapper.channelregistry.ChannelRegistryMapper;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Class containing common functionality for the different modules implementing the mapping logic of Cristin entries to
 * NVA entries.
 */
public class CristinMappingModule {

    private static final String NOT_DIGITS_OR_X_REGEX = "[^\\dxX]";
    private static final String ONLYE_0_OR_DASHES_REGEX = "[0-]+";

    protected final CristinObject cristinObject;

    protected final ChannelRegistryMapper channelRegistryMapper;
    protected final S3Client s3Client;

    public CristinMappingModule(CristinObject cristinObject, ChannelRegistryMapper channelRegistryMapper, S3Client s3Client) {
        this.cristinObject = cristinObject;
        this.channelRegistryMapper = channelRegistryMapper;
        this.s3Client = s3Client;
    }

    protected CristinJournalPublication extractCristinJournalPublication() {
        return Optional.ofNullable(cristinObject)
                   .map(CristinObject::getJournalPublication)
                   .orElse(null);
    }

    protected Optional<String> extractIsbn() {
        return Optional.ofNullable(cleanCristinIsbn(extractCristinBookReport().getIsbn()));
    }

    protected CristinBookOrReportMetadata extractCristinBookReport() {
        return Optional.ofNullable(cristinObject)
                   .map(CristinObject::getBookOrReportMetadata)
                   .orElse(null);
    }

    protected CristinBookOrReportPartMetadata extractCristinBookReportPart() {
        return Optional.ofNullable(cristinObject)
                   .map(CristinObject::getBookOrReportPartMetadata)
                   .orElse(null);
    }

    protected Integer extractYearReportedInNvi() {
        return Optional.ofNullable(cristinObject.getYearReported()).orElseGet(cristinObject::getPublicationYear);
    }

    protected String extractIssn() {
        return Optional.ofNullable(extractCristinJournalPublication())
                   .map(CristinJournalPublication::getJournal)
                   .map(CristinJournalPublicationJournal::getIssn)
                   .orElse(null);
    }

    protected String extractIssnOnline() {
        return Optional.ofNullable(extractCristinJournalPublication())
                   .map(CristinJournalPublication::getJournal)
                   .map(CristinJournalPublicationJournal::getIssnOnline)
                   .orElse(null);
    }

    protected String extractPublisherTitle() {
        return Optional.ofNullable(extractCristinJournalPublication())
                   .map(CristinJournalPublication::getJournal)
                   .map(CristinJournalPublicationJournal::getJournalTitle)
                   .orElse(null);
    }

    protected String extractSeriesTitle() {
        return Optional.ofNullable(extractCristinBookReport())
                   .map(CristinBookOrReportMetadata::getBookSeries)
                   .map(CristinJournalPublicationJournal::getJournalTitle)
                   .orElse(null);
    }

    protected String extractSeriesPrintIssn() {
        return Optional.ofNullable(extractCristinBookReport())
                   .map(CristinBookOrReportMetadata::getBookSeries)
                   .map(CristinJournalPublicationJournal::getIssn)
                   .orElse(null);
    }

    protected String extractSeriesOnlineIssn() {
        return Optional.ofNullable(extractCristinBookReport())
                   .map(CristinBookOrReportMetadata::getBookSeries)
                   .map(CristinJournalPublicationJournal::getIssnOnline)
                   .orElse(null);
    }

    private String cleanCristinIsbn(String isbn) {
        return isNull(isbn) || isEmptyIsbn(isbn) ? null : isbn.replaceAll(NOT_DIGITS_OR_X_REGEX, "");
    }

    private boolean isEmptyIsbn(String isbn) {
        return isbn.matches(ONLYE_0_OR_DASHES_REGEX);
    }
}
