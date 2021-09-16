package no.unit.nva.cristin.mapper.nva;

import java.util.Optional;
import no.unit.nva.cristin.mapper.CristinBookOrReportMetadata;
import no.unit.nva.cristin.mapper.CristinJournalPublication;
import no.unit.nva.cristin.mapper.CristinObject;
import no.unit.nva.model.contexttypes.PublishingHouse;
import no.unit.nva.model.contexttypes.UnconfirmedPublisher;

/**
 * Class containing common functionality for the different modules implementing the mapping logic of Cristin entries
 * to NVA entries.
 */
public class CristinMappingModule {

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
        return Optional.ofNullable(extractCristinBookReport().getIsbn());
    }

    protected CristinBookOrReportMetadata extractCristinBookReport() {
        return Optional.ofNullable(cristinObject)
            .map(CristinObject::getBookOrReportMetadata)
            .orElse(null);
    }

    protected PublishingHouse buildUnconfirmedPublisher() {
        return new UnconfirmedPublisher(extractPublisherName());
    }


    private String extractPublisherName() {
        return extractCristinBookReport().getPublisherName();
    }


}
