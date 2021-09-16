package no.unit.nva.cristin.mapper.nva;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import no.unit.nva.cristin.mapper.CristinBookOrReportMetadata;
import no.unit.nva.cristin.mapper.CristinObject;
import no.unit.nva.model.contexttypes.Book;
import no.unit.nva.model.contexttypes.BookSeries;
import no.unit.nva.model.exceptions.InvalidIsbnException;

public class NvaBookBuilder extends CristinMappingModule {

    public NvaBookBuilder(CristinObject cristinObject) {
        super(cristinObject);
    }

    public Book buildBookForPublicationContext() throws InvalidIsbnException {
        List<String> isbnList = extractIsbn().stream().collect(Collectors.toList());
        String seriesNumber = constructSeriesNumber();

        return new Book(createBookSeries(), seriesNumber, buildUnconfirmedPublisher(), isbnList);
    }

    private String constructSeriesNumber() {
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

    private BookSeries createBookSeries() {
        return new NvaBookSeriesBuilder(cristinObject).createBookSeries();
    }
}
