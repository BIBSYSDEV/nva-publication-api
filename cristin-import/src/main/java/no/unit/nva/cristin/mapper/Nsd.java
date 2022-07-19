package no.unit.nva.cristin.mapper;

import static no.unit.nva.cristin.lambda.constants.MappingConstants.NSD_PROXY_PATH;
import static no.unit.nva.cristin.lambda.constants.MappingConstants.NSD_PROXY_PATH_JOURNAL;
import static no.unit.nva.cristin.lambda.constants.MappingConstants.NSD_PROXY_PATH_PUBLISHER;
import static no.unit.nva.cristin.lambda.constants.MappingConstants.NVA_API_DOMAIN;
import java.net.URI;
import nva.commons.core.paths.UriWrapper;

public class Nsd {
    
    private final int nsdCode;
    private final int year;
    
    public Nsd(int nsdCode, int year) {
        this.nsdCode = nsdCode;
        this.year = year;
    }
    
    public URI createJournalOrSeriesUri() {
        return getNsdProxyUri(NSD_PROXY_PATH_JOURNAL);
    }
    
    public URI getPublisherUri() {
        return getNsdProxyUri(NSD_PROXY_PATH_PUBLISHER);
    }
    
    private URI getNsdProxyUri(String nsdProxyPathPublisher) {
        return nvaProxyUri()
            .addChild(nsdProxyPathPublisher)
            .addChild(Integer.toString(nsdCode))
            .addChild(Integer.toString(year))
            .getUri();
    }
    
    private UriWrapper nvaProxyUri() {
        return UriWrapper.fromUri(NVA_API_DOMAIN).addChild(NSD_PROXY_PATH);
    }
}
