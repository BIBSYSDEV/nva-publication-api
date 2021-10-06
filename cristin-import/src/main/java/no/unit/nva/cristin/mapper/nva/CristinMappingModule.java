package no.unit.nva.cristin.mapper.nva;

import java.util.Optional;
import no.unit.nva.cristin.mapper.CristinBookOrReportMetadata;
import no.unit.nva.cristin.mapper.CristinJournalPublication;
import no.unit.nva.cristin.mapper.CristinObject;

import static java.util.Objects.isNull;

/**
 * Class containing common functionality for the different modules implementing the mapping logic of Cristin entries to
 * NVA entries.
 */
public class CristinMappingModule {

    private static final String NOT_DIGITS_OR_X_REGEX = "[^\\dxX]";

    protected final CristinObject cristinObject;

    public CristinMappingModule(CristinObject cristinObject) {
        this.cristinObject = cristinObject;
    }

    protected CristinJournalPublication extractCristinJournalPublication() {
        return Optional.ofNullable(cristinObject)
            .map(CristinObject::getJournalPublication)
            .orElse(null);
    }

    protected Optional<String> extractIsbn() {
        return Optional.ofNullable(cleanCristinIsbn(extractCristinBookReport().getIsbn()));
    }

    private String cleanCristinIsbn(String isbn) {
        return isNull(isbn) ? null : isbn.replaceAll(NOT_DIGITS_OR_X_REGEX, "");
    }

    protected CristinBookOrReportMetadata extractCristinBookReport() {
        return Optional.ofNullable(cristinObject)
            .map(CristinObject::getBookOrReportMetadata)
            .orElse(null);
    }

    protected Integer extractYearReportedInNvi() {
        return Optional.ofNullable(cristinObject.getYearReported()).orElseGet(cristinObject::getPublicationYear);
    }
}
