package no.unit.nva.cristin.mapper.nva;

import static java.util.Objects.isNull;
import java.util.Optional;
import no.unit.nva.cristin.mapper.CristinBookOrReportMetadata;
import no.unit.nva.cristin.mapper.CristinJournalPublication;
import no.unit.nva.cristin.mapper.CristinObject;
import no.unit.nva.cristin.mapper.channelregistry.ChannelRegistryMapper;

/**
 * Class containing common functionality for the different modules implementing the mapping logic of Cristin entries to
 * NVA entries.
 */
public class CristinMappingModule {

    private static final String NOT_DIGITS_OR_X_REGEX = "[^\\dxX]";
    private static final String ONLYE_0_OR_DASHES_REGEX = "[0-]+";

    protected final CristinObject cristinObject;

    protected final ChannelRegistryMapper channelRegistryMapper;

    public CristinMappingModule(CristinObject cristinObject, ChannelRegistryMapper channelRegistryMapper) {
        this.cristinObject = cristinObject;
        this.channelRegistryMapper = channelRegistryMapper;
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

    protected Integer extractYearReportedInNvi() {
        return Optional.ofNullable(cristinObject.getYearReported()).orElseGet(cristinObject::getPublicationYear);
    }

    private String cleanCristinIsbn(String isbn) {
        return isNull(isbn) || isEmptyIsbn(isbn) ? null : isbn.replaceAll(NOT_DIGITS_OR_X_REGEX, "");
    }

    private boolean isEmptyIsbn(String isbn) {
        return isbn.matches(ONLYE_0_OR_DASHES_REGEX);
    }
}
