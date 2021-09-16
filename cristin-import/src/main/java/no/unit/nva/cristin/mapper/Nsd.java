package no.unit.nva.cristin.mapper;

import static no.unit.nva.cristin.lambda.constants.MappingConstants.NSD_PROXY_PATH;
import static no.unit.nva.cristin.lambda.constants.MappingConstants.NSD_PROXY_PATH_JOURNAL;
import static no.unit.nva.cristin.lambda.constants.MappingConstants.NSD_PROXY_PATH_PUBLISHER;
import static no.unit.nva.cristin.lambda.constants.MappingConstants.NVA_API_DOMAIN;
import java.net.URI;

import no.unit.nva.publication.s3imports.UriWrapper;

public class Nsd {


    private final int nsdCode;
    private final int year;

    public Nsd(int nsdCode, int year) {
        this.nsdCode = nsdCode;
        this.year = year;
    }

    public URI createJournalOrSeriesUri(){
        return nvaProxyUri()
            .addChild(NSD_PROXY_PATH_JOURNAL)
            .addChild(Integer.toString(nsdCode))
            .addChild(Integer.toString(year))
            .getUri();
    }


    public URI cratePublisherUri(){
        return nvaProxyUri()
            .addChild(NSD_PROXY_PATH_PUBLISHER)
            .addChild(Integer.toString(nsdCode))
            .addChild(Integer.toString(year))
            .getUri();
    }

    private UriWrapper nvaProxyUri() {
        return new UriWrapper(NVA_API_DOMAIN).addChild(NSD_PROXY_PATH);
    }
}
