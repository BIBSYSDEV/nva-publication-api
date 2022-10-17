package no.unit.nva.cristin.mapper.nva;

import no.unit.nva.cristin.mapper.CristinObject;
import no.unit.nva.model.contexttypes.Degree;
import no.unit.nva.model.exceptions.InvalidIsbnException;
import no.unit.nva.model.exceptions.InvalidUnconfirmedSeriesException;

public class NvaDegreeBuilder extends NvaBookLikeBuilder {
    
    public NvaDegreeBuilder(CristinObject cristinObject) {
        super(cristinObject);
    }
    
    public Degree buildDegree() throws InvalidIsbnException, InvalidUnconfirmedSeriesException {
        return new Degree.Builder()
                   .withPublisher(buildPublisher())
                   .withIsbnList(createIsbnList())
                   .withSeries(buildSeries())
                   .withSeriesNumber(constructSeriesNumber())
                   .build();
    }
}
