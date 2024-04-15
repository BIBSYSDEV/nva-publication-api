package no.unit.nva.cristin.mapper.nva;

import no.unit.nva.cristin.mapper.CristinObject;
import no.unit.nva.cristin.mapper.channelregistry.ChannelRegistryMapper;
import no.unit.nva.model.contexttypes.Report;
import no.unit.nva.model.exceptions.InvalidIsbnException;
import no.unit.nva.model.exceptions.InvalidIssnException;
import no.unit.nva.model.exceptions.InvalidUnconfirmedSeriesException;
import software.amazon.awssdk.services.s3.S3Client;

public class NvaReportBuilder extends NvaBookLikeBuilder {

    public NvaReportBuilder(CristinObject cristinObject, ChannelRegistryMapper channelRegistryMapper, S3Client s3Client) {
        super(cristinObject, channelRegistryMapper, s3Client);
    }

    public Report buildNvaReport()
        throws InvalidIssnException, InvalidIsbnException, InvalidUnconfirmedSeriesException {
        return new Report.Builder()
                   .withPublisher(buildPublisher())
                   .withIsbnList(createIsbnList())
                   .withSeries(buildSeries())
                   .withSeriesNumber(constructSeriesNumber())
                   .build();
    }
}
