package no.unit.nva.cristin.mapper.nva;

import no.unit.nva.cristin.mapper.CristinObject;
import no.unit.nva.model.contexttypes.Book;
import no.unit.nva.model.exceptions.InvalidIsbnException;

public class NvaBookBuilder extends NvaBookLikeBuilder {
    
    public NvaBookBuilder(CristinObject cristinObject) {
        super(cristinObject);
    }
    
    public Book buildBookForPublicationContext() throws InvalidIsbnException {
        return new Book(buildSeries(), constructSeriesNumber(), buildPublisher(), createIsbnList());
    }
}
