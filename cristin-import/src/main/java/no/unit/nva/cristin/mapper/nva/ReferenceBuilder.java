package no.unit.nva.cristin.mapper.nva;

import static java.util.Objects.nonNull;
import static no.unit.nva.cristin.mapper.CristinMainCategory.isArt;
import static no.unit.nva.cristin.mapper.CristinMainCategory.isBook;
import static no.unit.nva.cristin.mapper.CristinMainCategory.isChapter;
import static no.unit.nva.cristin.mapper.CristinMainCategory.isEvent;
import static no.unit.nva.cristin.mapper.CristinMainCategory.isExhibition;
import static no.unit.nva.cristin.mapper.CristinMainCategory.isJournal;
import static no.unit.nva.cristin.mapper.CristinMainCategory.isMediaContribution;
import static no.unit.nva.cristin.mapper.CristinMainCategory.isReport;
import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.isDegreeLicentiate;
import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.isDegreeMaster;
import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.isDegreePhd;
import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.isMediaFeatureArticle;
import static no.unit.nva.cristin.mapper.nva.exceptions.ExceptionHandling.castToCorrectRuntimeException;
import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.stream.Stream;
import no.unit.nva.cristin.mapper.CristinBookOrReportMetadata;
import no.unit.nva.cristin.mapper.CristinBookOrReportPartMetadata;
import no.unit.nva.cristin.mapper.CristinJournalPublication;
import no.unit.nva.cristin.mapper.CristinLectureOrPosterMetaData;
import no.unit.nva.cristin.mapper.CristinMediaContribution;
import no.unit.nva.cristin.mapper.CristinObject;
import no.unit.nva.cristin.mapper.CristinSecondaryCategory;
import no.unit.nva.cristin.mapper.MediaPeriodicalBuilder;
import no.unit.nva.cristin.mapper.PeriodicalBuilder;
import no.unit.nva.cristin.mapper.PresentationEvent;
import no.unit.nva.cristin.mapper.PublicationInstanceBuilderImpl;
import no.unit.nva.cristin.mapper.channelregistry.ChannelRegistryMapper;
import no.unit.nva.model.Agent;
import no.unit.nva.model.Reference;
import no.unit.nva.model.UnconfirmedOrganization;
import no.unit.nva.model.contexttypes.Anthology;
import no.unit.nva.model.contexttypes.Artistic;
import no.unit.nva.model.contexttypes.Event;
import no.unit.nva.model.contexttypes.ExhibitionContent;
import no.unit.nva.model.contexttypes.MediaContribution;
import no.unit.nva.model.contexttypes.PublicationContext;
import no.unit.nva.model.contexttypes.media.MediaFormat;
import no.unit.nva.model.contexttypes.media.MediaSubType;
import no.unit.nva.model.contexttypes.media.MediaSubTypeEnum;
import no.unit.nva.model.contexttypes.place.Place;
import no.unit.nva.model.contexttypes.place.UnconfirmedPlace;
import no.unit.nva.model.exceptions.InvalidIsbnException;
import no.unit.nva.model.exceptions.InvalidIssnException;
import no.unit.nva.model.exceptions.InvalidUnconfirmedSeriesException;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.pages.Pages;
import no.unit.nva.model.time.Period;
import no.unit.nva.model.time.Time;
import nva.commons.core.SingletonCollector;
import nva.commons.doi.DoiConverter;
import nva.commons.doi.DoiValidator;

public class ReferenceBuilder extends CristinMappingModule {

    private final DoiConverter doiConverter;

    public ReferenceBuilder(CristinObject cristinObject, ChannelRegistryMapper channelRegistryMapper) {
        super(cristinObject, channelRegistryMapper);
        doiConverter = new DoiConverter(DoiValidator::validateOffline);
    }

    public Reference buildReference() {
        PublicationInstanceBuilderImpl publicationInstanceBuilderImpl = new PublicationInstanceBuilderImpl(
            cristinObject);
        PublicationInstance<? extends Pages> publicationInstance = publicationInstanceBuilderImpl.build();
        PublicationContext publicationContext = attempt(this::buildPublicationContext).orElseThrow(
            failure -> castToCorrectRuntimeException(failure.getException()));
        return new Reference.Builder().withPublicationInstance(publicationInstance)
                   .withPublishingContext(publicationContext)
                   .withDoi(extractDoi())
                   .build();
    }

    @SuppressWarnings({"PMD.NPathComplexity"})
    private PublicationContext buildPublicationContext()
        throws InvalidIsbnException, InvalidIssnException, InvalidUnconfirmedSeriesException {
        if (isBook(cristinObject)) {
            return new NvaBookBuilder(cristinObject, channelRegistryMapper).buildBookForPublicationContext();
        }
        if (isJournal(cristinObject)) {
            return new PeriodicalBuilder(cristinObject, channelRegistryMapper).buildPeriodicalForPublicationContext();
        }
        if (isMediaFeatureArticle(cristinObject)) {
            return new MediaPeriodicalBuilder(cristinObject,
                                              channelRegistryMapper)
                       .buildMediaPeriodicalForPublicationContext();
        }
        if (isReport(cristinObject)) {
            return buildPublicationContextWhenMainCategoryIsReport();
        }
        if (isChapter(cristinObject)) {
            return buildChapterForPublicationContext();
        }
        if (isEvent(cristinObject)) {
            return buildEventForPublicationContext();
        }
        if (isMediaContribution(cristinObject)) {
            return buildMediaContributionForPublicationContext();
        }
        if (isExhibition(cristinObject)) {
            return new ExhibitionContent();
        }
        if (isArt(cristinObject)) {
            return new Artistic();
        }
        return null;
    }

    private PublicationContext buildMediaContributionForPublicationContext() {
        return isWrittenInterview(cristinObject)
                   ? new MediaContribution.Builder()
                         .withFormat(MediaFormat.TEXT)
                         .withMedium(MediaSubType.create(MediaSubTypeEnum.JOURNAL))
                         .withDisseminationChannel(extractDisseminationChannel().orElse(null))
                         .build()
                   : new MediaContribution.Builder()
                         .withFormat(new MediaFormatBuilder(cristinObject).build())
                         .withMedium(new MediaSubTypeBuilder(cristinObject).build())
                         .withDisseminationChannel(extractDisseminationChannel().orElse(null))
                         .build();
    }

    private Optional<String> extractDisseminationChannel() {
        return Optional.ofNullable(cristinObject.getMediaContribution())
                   .map(CristinMediaContribution::getMediaPlaceName);
    }

    private boolean isWrittenInterview(CristinObject cristinObject) {
        return CristinSecondaryCategory.WRITTEN_INTERVIEW.equals(cristinObject.getSecondaryCategory());
    }

    private PublicationContext buildPublicationContextWhenMainCategoryIsReport()
        throws InvalidIsbnException, InvalidIssnException, InvalidUnconfirmedSeriesException {
        if (isDegreePhd(cristinObject) || isDegreeMaster(cristinObject) || isDegreeLicentiate(cristinObject)) {
            return new NvaDegreeBuilder(cristinObject, channelRegistryMapper).buildDegree();
        }
        return new NvaReportBuilder(cristinObject, channelRegistryMapper).buildNvaReport();
    }

    private Anthology buildChapterForPublicationContext() {
        return new Anthology.Builder().build();
    }

    private PublicationContext buildEventForPublicationContext() {
        return new Event.Builder()
                   .withLabel(extractLabel())
                   .withAgent(extractAgent())
                   .withPlace(extractPlace())
                   .withTime(extractTime())
                   .build();
    }

    private Time extractTime() {
        var from = extractCristinEventFromDate();
        var to = extractCristinEventToDate();
        return nonNull(from) && nonNull(to) ? new Period(from, to) : null;
    }

    private Instant extractCristinEventFromDate() {
        return Optional.ofNullable(cristinObject)
                   .map(CristinObject::getLectureOrPosterMetaData)
                   .map(CristinLectureOrPosterMetaData::getEvent)
                   .map(PresentationEvent::getFrom)
                   .map(this::toInstant)
                   .orElse(null);
    }

    private Instant extractCristinEventToDate() {
        return Optional.ofNullable(cristinObject)
                   .map(CristinObject::getLectureOrPosterMetaData)
                   .map(CristinLectureOrPosterMetaData::getEvent)
                   .map(PresentationEvent::getTo)
                   .map(this::toInstant)
                   .orElse(null);
    }

    private Instant toInstant(String date) {
        var formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        return LocalDateTime.parse(date, formatter).toInstant(ZoneOffset.UTC);
    }

    private Place extractPlace() {
        return Optional.ofNullable(cristinObject)
                   .map(CristinObject::getLectureOrPosterMetaData)
                   .map(CristinLectureOrPosterMetaData::getEvent)
                   .map(this::toUnconfirmedPlace)
                   .orElse(null);
    }

    private Place toUnconfirmedPlace(PresentationEvent event) {
        return new UnconfirmedPlace(
            Optional.ofNullable(event).map(PresentationEvent::getPlace).orElse(null),
            Optional.ofNullable(event).map(PresentationEvent::getCountryCode).orElse(null)
            );
    }

    private Agent extractAgent() {
        return Optional.ofNullable(cristinObject)
                   .map(CristinObject::getLectureOrPosterMetaData)
                   .map(CristinLectureOrPosterMetaData::getEvent)
                   .map(PresentationEvent::getAgent)
                   .map(UnconfirmedOrganization::new)
                   .orElse(null);
    }

    private String extractLabel() {
        return Optional.ofNullable(cristinObject)
                   .map(CristinObject::getLectureOrPosterMetaData)
                   .map(CristinLectureOrPosterMetaData::getEvent)
                   .map(PresentationEvent::getTitle)
                   .orElse(null);
    }

    private URI extractDoi() {
        return Stream.of(getBookOrReportPart(), getBookOrReportDoi(), getJournalDOi())
                   .flatMap(Optional::stream)
                   .distinct()
                   .collect(SingletonCollector.collectOrElse(null));
    }

    private Optional<URI> getBookOrReportPart() {
        return Optional.ofNullable(extractCristinBookReportPart())
                   .map(CristinBookOrReportPartMetadata::getDoi)
                   .map(doiConverter::toUri);
    }

    private Optional<URI> getBookOrReportDoi() {
        return Optional.ofNullable(extractCristinBookReport())
                   .map(CristinBookOrReportMetadata::getDoi)
                   .map(doiConverter::toUri);
    }

    private Optional<URI> getJournalDOi() {
        return Optional.ofNullable(extractCristinJournalPublication())
                   .map(CristinJournalPublication::getDoi)
                   .map(doiConverter::toUri);
    }
}
