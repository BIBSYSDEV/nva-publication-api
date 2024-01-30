package no.unit.nva.cristin.mapper;

import static no.unit.nva.cristin.lambda.constants.MappingConstants.NVA_CHANNEL_REGISTRY_V2;
import static no.unit.nva.cristin.lambda.constants.MappingConstants.NSD_PROXY_PATH_PUBLISHER;
import static no.unit.nva.cristin.lambda.constants.MappingConstants.NVA_API_DOMAIN;
import java.net.URI;
import java.util.Optional;
import no.unit.nva.cristin.mapper.channelregistry.ChannelRegistryEntry;
import no.unit.nva.cristin.mapper.channelregistry.ChannelRegistryEntry.ChannelType;
import no.unit.nva.cristin.mapper.channelregistry.ChannelRegistryMapper;
import no.unit.nva.cristin.mapper.nva.exceptions.ChannelRegistryException;
import no.unit.nva.cristin.mapper.nva.exceptions.WrongChannelTypeException;
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

    public URI getPublisherUri() {
        return lookupNsdPublisherProxyUri()
                   .orElseGet(() -> lookupNsdJournalOrSeriesProxyUri()
                                        .orElseThrow(() -> new ChannelRegistryException(nsdCode)));
    }

    public URI createJournal() {
        return lookUpNsdJournal().orElseThrow(() -> new ChannelRegistryException(nsdCode));
    }

    public URI createSeries() {
        return lookUpNsdSeries().orElseThrow(() -> new ChannelRegistryException(nsdCode));
    }

    private Optional<URI> lookUpNsdSeries() {
        return channelRegistryMapper.convertNsdJournalCodeToPid(nsdCode)
            .map(this::toSeriesUri);
    }

    private URI toSeriesUri(ChannelRegistryEntry channelRegistryEntry) {
        if (ChannelType.SERIES.equals(channelRegistryEntry.type())) {
            return getNsdProxyUri(channelRegistryEntry.getEntryPath(), channelRegistryEntry.id());
        } else {
            throw new WrongChannelTypeException(channelRegistryEntry);
        }
    }

    private Optional<URI> lookUpNsdJournal() {
        return channelRegistryMapper.convertNsdJournalCodeToPid(nsdCode)
                   .map(this::toJournalUri);
    }

    private URI toJournalUri(ChannelRegistryEntry channelRegistryEntry) {
        if (ChannelType.JOURNAL.equals(channelRegistryEntry.type())) {
            return getNsdProxyUri(channelRegistryEntry.getEntryPath(), channelRegistryEntry.id());
        } else {
            throw new WrongChannelTypeException(channelRegistryEntry);
        }
    }

    private Optional<URI> lookupNsdPublisherProxyUri() {
        return channelRegistryMapper.convertNsdPublisherCodeToPid(nsdCode)
                   .map(pid -> getNsdProxyUri(NSD_PROXY_PATH_PUBLISHER, pid));
    }

    private Optional<URI> lookupNsdJournalOrSeriesProxyUri() {
        return channelRegistryMapper.convertNsdJournalCodeToPid(nsdCode)
                   .map(channelRegistryEntry -> getNsdProxyUri(channelRegistryEntry.getEntryPath(),
                                                               channelRegistryEntry.id()));
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
