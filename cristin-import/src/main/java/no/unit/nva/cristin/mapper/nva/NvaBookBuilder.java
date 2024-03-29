package no.unit.nva.cristin.mapper.nva;

import no.unit.nva.cristin.mapper.CristinObject;
import no.unit.nva.cristin.mapper.channelregistry.ChannelRegistryMapper;
import no.unit.nva.model.contexttypes.Book;

public class NvaBookBuilder extends NvaBookLikeBuilder {

    public NvaBookBuilder(CristinObject cristinObject, ChannelRegistryMapper channelRegistryMapper) {
        super(cristinObject, channelRegistryMapper);
    }

    public Book buildBookForPublicationContext() {
        return new Book(buildSeries(), constructSeriesNumber(), buildPublisher(), createIsbnList(), lookupRevision());
    }
}
