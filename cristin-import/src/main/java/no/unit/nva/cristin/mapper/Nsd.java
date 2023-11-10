package no.unit.nva.cristin.mapper;

import static no.unit.nva.cristin.lambda.constants.MappingConstants.NVA_CHANNEL_REGISTRY_V2;
import static no.unit.nva.cristin.lambda.constants.MappingConstants.NSD_PROXY_PATH_JOURNAL;
import static no.unit.nva.cristin.lambda.constants.MappingConstants.NSD_PROXY_PATH_PUBLISHER;
import static no.unit.nva.cristin.lambda.constants.MappingConstants.NVA_API_DOMAIN;
import java.net.URI;
import java.util.Optional;
import no.unit.nva.cristin.mapper.channelregistry.ChannelRegistryMapper;
import no.unit.nva.cristin.mapper.nva.exceptions.ChannelRegistryException;
import nva.commons.core.paths.UriWrapper;

public class Nsd {

    private final int nsdCode;
    private final int year;
    private final ChannelRegistryMapper channelRegistryMapper;

    public Nsd(int nsdCode, int year, ChannelRegistryMapper channelRegistryMapper) {
        this.nsdCode = nsdCode;
        this.year = year;
        this.channelRegistryMapper = channelRegistryMapper;
    }

    public URI createJournalOrSeriesUri() {
        return lookupNsdJournalOrSeriesProxyUri()
                   .orElseGet(() -> lookupNsdPublisherProxyUri()
                                        .orElseThrow(() -> new ChannelRegistryException(nsdCode)));
    }

    public URI getPublisherUri() {
        return lookupNsdPublisherProxyUri()
                   .orElseGet(() -> lookupNsdJournalOrSeriesProxyUri()
                                        .orElseThrow(() -> new ChannelRegistryException(nsdCode)));
    }

    private Optional<URI> lookupNsdPublisherProxyUri() {
        return channelRegistryMapper.convertNsdPublisherCodeToPid(nsdCode)
                   .map(pid -> getNsdProxyUri(NSD_PROXY_PATH_PUBLISHER, pid));
    }

    private Optional<URI> lookupNsdJournalOrSeriesProxyUri() {
        return channelRegistryMapper.convertNsdJournalCodeToPid(nsdCode)
                   .map(pid -> getNsdProxyUri(NSD_PROXY_PATH_JOURNAL, pid));
    }

    private URI getNsdProxyUri(String nsdProxyPath, String pid) {
        return nvaProxyUri()
                   .addChild(nsdProxyPath)
                   .addChild(pid)
                   .addChild(Integer.toString(year))
                   .getUri();
    }

    private UriWrapper nvaProxyUri() {
        return UriWrapper.fromUri(NVA_API_DOMAIN).addChild(NVA_CHANNEL_REGISTRY_V2);
    }
}
