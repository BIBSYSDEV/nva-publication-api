package no.unit.nva.cristin.mapper.nva;

import no.unit.nva.cristin.mapper.CristinObject;
import no.unit.nva.cristin.mapper.channelregistry.ChannelRegistryMapper;
import no.unit.nva.model.contexttypes.Book;
import software.amazon.awssdk.services.s3.S3Client;

public class NvaBookBuilder extends NvaBookLikeBuilder {

    public NvaBookBuilder(CristinObject cristinObject, ChannelRegistryMapper channelRegistryMapper, S3Client s3Client) {
        super(cristinObject, channelRegistryMapper, s3Client);
    }

    public Book buildBookForPublicationContext() {
        return new Book(buildSeries(), constructSeriesNumber(), buildPublisher(), createIsbnList(), lookupRevision());
    }
}
