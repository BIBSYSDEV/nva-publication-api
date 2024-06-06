package no.unit.nva.cristin.mapper.nva;

import static java.util.Objects.nonNull;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.cristin.lambda.ErrorReport;
import no.unit.nva.cristin.mapper.CristinBookOrReportMetadata;
import no.unit.nva.cristin.mapper.CristinObject;
import no.unit.nva.cristin.mapper.CristinPublisher;
import no.unit.nva.cristin.mapper.PublishingChannelEntryResolver;
import no.unit.nva.cristin.mapper.channelregistry.ChannelRegistryMapper;
import no.unit.nva.cristin.mapper.nva.exceptions.NoPublisherException;
import no.unit.nva.model.Revision;
import no.unit.nva.model.contexttypes.BookSeries;
import no.unit.nva.model.contexttypes.NullPublisher;
import no.unit.nva.model.contexttypes.Publisher;
import no.unit.nva.model.contexttypes.PublishingHouse;
import no.unit.nva.model.contexttypes.UnconfirmedPublisher;
import nva.commons.core.StringUtils;
import software.amazon.awssdk.services.s3.S3Client;

public class NvaBookLikeBuilder extends CristinMappingModule {

    public static final String CUSTOM_VOLUME_SERIES_DELIMITER = ";";
    private static final String EMPTY_STRING = null;
    public static final String MISSING_PUBLISHER = "Missing publisher";

    public NvaBookLikeBuilder(CristinObject cristinObject, ChannelRegistryMapper channelRegistryMapper,
                              S3Client s3Client) {
        super(cristinObject, channelRegistryMapper, s3Client);
    }

    protected String constructSeriesNumber() {
        String volume = extractBookOrReportMetadata()
                            .map(CristinBookOrReportMetadata::getVolume)
                            .filter(StringUtils::isNotBlank)
                            .map(volumeNumber -> String.format("Volume:%s", volumeNumber))
                            .orElse(EMPTY_STRING);
        String issue = extractBookOrReportMetadata()
                           .map(CristinBookOrReportMetadata::getIssue)
                           .filter(StringUtils::isNotBlank)
                           .map(issueNumber -> String.format("Issue:%s", issueNumber))
                           .orElse(null);

        return Stream.of(volume, issue).filter(Objects::nonNull)
                   .collect(Collectors.joining(CUSTOM_VOLUME_SERIES_DELIMITER));
    }

    protected BookSeries buildSeries() {
        return new NvaBookSeriesBuilder(cristinObject, channelRegistryMapper, s3Client)
                   .createBookSeries();
    }

    protected List<String> createIsbnList() {
        return extractIsbn().stream().collect(Collectors.toList());
    }

    protected PublishingHouse buildPublisher() {
        return createConfirmedPublisherIfPublisherReferenceHasNsdCode()
                   .orElseGet(this::createUnconfirmedPublisher);
    }

    private Optional<CristinBookOrReportMetadata> extractBookOrReportMetadata() {
        return Optional.of(cristinObject)
                   .map(CristinObject::getBookOrReportMetadata);
    }

    private Optional<PublishingHouse> createConfirmedPublisherIfPublisherReferenceHasNsdCode() {
        var nsdCode = extractPublishersNsdCode().orElse(null);
        var nsd = new PublishingChannelEntryResolver(nsdCode, extractYearReportedInNvi(), extractPublisherNames(),
                                                     List.of(),
                                                     channelRegistryMapper,
                                                     s3Client,
                                                     cristinObject.getId());
        var publisherUri = nsd.getPublisherUri();
        return nonNull(publisherUri) ? Optional.of(new Publisher(publisherUri)) : Optional.empty();
    }

    private List<String> extractPublisherNames() {
        return Stream.of(extractPublisherNameFromAlternativeField(),
                         extractPublisherNameFromPrimaryField().orElse(null))
                   .filter(Objects::nonNull)
                   .toList();
    }

    private Optional<Integer> extractPublishersNsdCode() {
        return extractBookOrReportMetadata()
                   .map(CristinBookOrReportMetadata::getCristinPublisher)
                   .map(CristinPublisher::getNsdCode);
    }

    private PublishingHouse createUnconfirmedPublisher() {
        var publisherName = extractUnconfirmedPublisherName();
        return nonNull(publisherName) ? new UnconfirmedPublisher(publisherName) : new NullPublisher();
    }

    private String extractUnconfirmedPublisherName() {
        var publisherName = extractPublisherNameFromPrimaryField()
                                   .orElseGet(this::extractPublisherNameFromAlternativeField);
        return Optional.ofNullable(publisherName)
                   .orElse(persistReportForNoPublisherException());
    }

    private String persistReportForNoPublisherException() {
        ErrorReport.exceptionName(NoPublisherException.name())
            .withBody(MISSING_PUBLISHER)
            .withCristinId(cristinObject.getId())
            .persist(s3Client);
        return null;
    }

    private Optional<String> extractPublisherNameFromPrimaryField() {
        return extractBookOrReportMetadata()
                   .map(CristinBookOrReportMetadata::getCristinPublisher)
                   .map(CristinPublisher::getPublisherName);
    }

    private String extractPublisherNameFromAlternativeField() {
        return extractBookOrReportMetadata()
                   .map(CristinBookOrReportMetadata::getPublisherName)
                   .orElse(null);
    }

    protected Revision lookupRevision() {
        return Optional.ofNullable(cristinObject.getBookOrReportMetadata())
                   .map(CristinBookOrReportMetadata::convertRevisionStatusToNvaRevision)
                   .orElse(Revision.UNREVISED);
    }
}
