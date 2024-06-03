package no.unit.nva.cristin.mapper;

import static no.unit.nva.cristin.lambda.constants.MappingConstants.NVA_CHANNEL_REGISTRY_V2;
import static no.unit.nva.cristin.lambda.constants.MappingConstants.NSD_PROXY_PATH_PUBLISHER;
import static no.unit.nva.cristin.lambda.constants.MappingConstants.NVA_API_DOMAIN;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import no.unit.nva.cristin.lambda.ErrorReport;
import no.unit.nva.cristin.mapper.channelregistry.ChannelRegistryEntry;
import no.unit.nva.cristin.mapper.channelregistry.ChannelRegistryEntry.ChannelType;
import no.unit.nva.cristin.mapper.channelregistry.ChannelRegistryMapper;
import no.unit.nva.cristin.mapper.nva.exceptions.ChannelRegistryException;
import no.unit.nva.cristin.mapper.nva.exceptions.WrongChannelTypeException;
import nva.commons.core.paths.UriWrapper;
import software.amazon.awssdk.services.s3.S3Client;

public class PublishingChannelEntryResolver {

    private final Integer nsdCode;
    private final int year;
    private final List<String> channelNames;
    private final ChannelRegistryMapper channelRegistryMapper;
    private final S3Client s3Client;
    private final Integer cristinId;

    public PublishingChannelEntryResolver(Integer nsdCode,
                                          int year,
                                          List<String> channelNames,
                                          ChannelRegistryMapper channelRegistryMapper,
                                          S3Client s3Client,
                                          Integer cristinId) {
        this.nsdCode = nsdCode;
        this.year = year;
        this.channelNames = channelNames;
        this.channelRegistryMapper = channelRegistryMapper;
        this.s3Client = s3Client;
        this.cristinId = cristinId;
    }

    public URI getPublisherUri() {
        return lookupNsdPublisherProxyUri()
                   .or(this::lookupCristinPublisherByhName)
                   .orElseGet(() -> lookupNsdJournalOrSeriesProxyUri()
                                        .orElse(persistChannelRegistryExceptionReport("Publisher")));
    }

    private Optional<? extends URI> lookupCristinPublisherByhName() {
        return channelNames.stream().map(channelRegistryMapper::convertPublisherNameToPid)
                   .flatMap(Optional::stream)
                   .findFirst()
                   .map(pid -> getNsdProxyUri(NSD_PROXY_PATH_PUBLISHER, pid));
    }

    public URI createJournal() {
        return lookUpNsdJournal().orElse(persistChannelRegistryExceptionReport("Journal"));
    }

    private URI persistChannelRegistryExceptionReport(String channelType) {
        ErrorReport.exceptionName(ChannelRegistryException.name())
            .withCristinId(cristinId)
            .withBody(String.join(":",channelType, String.valueOf(nsdCode)))
            .persist(s3Client);
        return null;
    }

    public URI createSeries() {
        return lookUpNsdSeries().orElse(persistChannelRegistryExceptionReport("Series"));
    }

    private Optional<URI> lookUpNsdSeries() {
        return Optional.ofNullable(nsdCode)
                   .flatMap(identifier -> channelRegistryMapper.convertNsdJournalCodeToPid(identifier)
                                              .map(this::toSeriesUri));
    }

    private URI toSeriesUri(ChannelRegistryEntry channelRegistryEntry) {
        var uri = getNsdProxyUri(channelRegistryEntry.getEntryPath(), channelRegistryEntry.id());
        if (!ChannelType.SERIES.equals(channelRegistryEntry.type())) {
            ErrorReport.exceptionName(WrongChannelTypeException.name())
                .withBody(uri.toString())
                .withCristinId(cristinId)
                .persist(s3Client);
        }
        return uri;
    }

    private Optional<URI> lookUpNsdJournal() {
        return Optional.ofNullable(nsdCode)
                   .flatMap(integer -> channelRegistryMapper.convertNsdJournalCodeToPid(integer)
                                           .map(this::toJournalUri));
    }

    private URI toJournalUri(ChannelRegistryEntry channelRegistryEntry) {
        var uri = getNsdProxyUri(channelRegistryEntry.getEntryPath(), channelRegistryEntry.id());
        if (!ChannelType.JOURNAL.equals(channelRegistryEntry.type())) {
            ErrorReport.exceptionName(WrongChannelTypeException.class.getSimpleName())
                .withBody(uri.toString())
                .withCristinId(cristinId)
                .persist(s3Client);
        }
        return uri;
    }

    private Optional<URI> lookupNsdPublisherProxyUri() {
        return Optional.ofNullable(nsdCode)
                   .flatMap(integer -> channelRegistryMapper.convertNsdPublisherCodeToPid(integer)
                                              .map(pid -> getNsdProxyUri(NSD_PROXY_PATH_PUBLISHER, pid)));
    }

    private Optional<URI> lookupNsdJournalOrSeriesProxyUri() {
        return Optional.ofNullable(nsdCode)
                   .flatMap(integer -> channelRegistryMapper.convertNsdJournalCodeToPid(integer)
                                           .map(channelRegistryEntry -> getNsdProxyUri(channelRegistryEntry.getEntryPath(),
                                                                                       channelRegistryEntry.id())));
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
