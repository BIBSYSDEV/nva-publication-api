package no.unit.nva.cristin.mapper.nva;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import no.unit.nva.cristin.mapper.CristinBookOrReportMetadata;
import no.unit.nva.cristin.mapper.CristinObject;
import no.unit.nva.model.contexttypes.BookSeries;

public class NvaBookLikeBuilder extends CristinMappingModule {

    public NvaBookLikeBuilder(CristinObject cristinObject) {
        super(cristinObject);
    }

    protected String constructSeriesNumber() {
        String volume = Optional.of(cristinObject)
            .map(CristinObject::getBookOrReportMetadata)
            .map(CristinBookOrReportMetadata::getVolume)
            .orElse(null);
        String issue = Optional.of(cristinObject)
            .map(CristinObject::getBookOrReportMetadata)
            .map(CristinBookOrReportMetadata::getIssue)
            .orElse(null);
        return String.format("Volume:%s;Issue:%s", volume, issue);
    }

    protected BookSeries buildSeries() {
        return new NvaBookSeriesBuilder(cristinObject).createBookSeries();
    }

    protected List<String> createIsbnList() {
        return extractIsbn().stream().collect(Collectors.toList());
    }
}
