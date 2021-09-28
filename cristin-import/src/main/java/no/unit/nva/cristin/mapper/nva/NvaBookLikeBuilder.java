package no.unit.nva.cristin.mapper.nva;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.cristin.mapper.CristinBookOrReportMetadata;
import no.unit.nva.cristin.mapper.CristinObject;
import no.unit.nva.cristin.mapper.CristinPublisher;
import no.unit.nva.cristin.mapper.Nsd;
import no.unit.nva.cristin.mapper.nva.exceptions.NoPublisherException;
import no.unit.nva.model.contexttypes.BookSeries;
import no.unit.nva.model.contexttypes.Publisher;
import no.unit.nva.model.contexttypes.PublishingHouse;
import no.unit.nva.model.contexttypes.UnconfirmedPublisher;
import nva.commons.core.StringUtils;

public class NvaBookLikeBuilder extends CristinMappingModule {

    public static final String CUSTOM_VOLUME_SERIES_DELIMITER = ";";
    private static final String EMPTY_STRING = null;

    public NvaBookLikeBuilder(CristinObject cristinObject) {
        super(cristinObject);
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
        return new NvaBookSeriesBuilder(cristinObject).createBookSeries();
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
        return extractPublishersNsdCode()
            .map(nsdCode -> new Nsd(nsdCode, extractYearReportedInNvi()))
            .map(Nsd::getPublisherUri)
            .map(this::createConfirmedPublisher);
    }

    private Optional<Integer> extractPublishersNsdCode() {
        return extractBookOrReportMetadata()
            .map(CristinBookOrReportMetadata::getCristinPublisher)
            .map(CristinPublisher::getNsdCode);
    }

    private PublishingHouse createUnconfirmedPublisher() {
        return new UnconfirmedPublisher(extractUnconfirmedPublisherName());
    }

    private PublishingHouse createConfirmedPublisher(URI uri) {
        return new Publisher(uri);
    }

    private String extractUnconfirmedPublisherName() {
        String publisherName = extractPublisherNameFromPrimaryField()
            .orElseGet(this::lookForPublisherNameInAlternativeField);
        return Optional.ofNullable(publisherName)
            .orElseThrow(this::publicationWithoutPublisherIsNotAllowed);
    }

    private NoPublisherException publicationWithoutPublisherIsNotAllowed() {
        return new NoPublisherException(cristinObject.getId());
    }

    private Optional<String> extractPublisherNameFromPrimaryField() {
        return extractBookOrReportMetadata()
            .map(CristinBookOrReportMetadata::getCristinPublisher)
            .map(CristinPublisher::getPublisherName);
    }

    private String lookForPublisherNameInAlternativeField() {
        return extractBookOrReportMetadata()
            .map(CristinBookOrReportMetadata::getPublisherName)
            .orElse(null);
    }
}
