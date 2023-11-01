package no.unit.nva.cristin.mapper.nva;

import no.unit.nva.cristin.mapper.CristinObject;
import no.unit.nva.cristin.mapper.channelregistry.ChannelRegistryMapper;
import no.unit.nva.model.contexttypes.Degree;
import no.unit.nva.model.exceptions.InvalidIsbnException;
import no.unit.nva.model.exceptions.InvalidUnconfirmedSeriesException;

public class NvaDegreeBuilder extends NvaBookLikeBuilder {

    public NvaDegreeBuilder(CristinObject cristinObject, ChannelRegistryMapper channelRegistryMapper) {
        super(cristinObject, channelRegistryMapper);
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
