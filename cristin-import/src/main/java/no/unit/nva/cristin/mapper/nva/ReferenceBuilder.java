package no.unit.nva.cristin.mapper.nva;

import static java.util.Objects.nonNull;
import static no.unit.nva.cristin.mapper.CristinMainCategory.isArt;
import static no.unit.nva.cristin.mapper.CristinMainCategory.isBook;
import static no.unit.nva.cristin.mapper.CristinMainCategory.isChapter;
import static no.unit.nva.cristin.mapper.CristinMainCategory.isEvent;
import static no.unit.nva.cristin.mapper.CristinMainCategory.isExhibition;
import static no.unit.nva.cristin.mapper.CristinMainCategory.isInformationMaterial;
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
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import no.unit.nva.cristin.lambda.ErrorReport;
import no.unit.nva.cristin.mapper.CristinBookOrReportMetadata;
import no.unit.nva.cristin.mapper.CristinBookOrReportPartMetadata;
import no.unit.nva.cristin.mapper.CristinJournalPublication;
import no.unit.nva.cristin.mapper.CristinJournalPublicationJournal;
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
import nva.commons.doi.DoiConverter;
import nva.commons.doi.DoiValidator;
import nva.commons.doi.InvalidDoiException;
import software.amazon.awssdk.services.s3.S3Client;

public class ReferenceBuilder extends CristinMappingModule {

    public static final String NIFU_CUSTOMER_NAME = "NIFU";
    private final DoiConverter doiConverter;

    public ReferenceBuilder(CristinObject cristinObject,
                            ChannelRegistryMapper channelRegistryMapper,
                            S3Client s3Client) {
        super(cristinObject, channelRegistryMapper, s3Client);
        doiConverter = new DoiConverter(DoiValidator::validateOffline);

    }

    public Reference buildReference() {
        PublicationInstanceBuilderImpl publicationInstanceBuilderImpl = new PublicationInstanceBuilderImpl(
            cristinObject, s3Client);
        PublicationInstance<? extends Pages> publicationInstance = publicationInstanceBuilderImpl.build();
        PublicationContext publicationContext = attempt(this::buildPublicationContext).orElseThrow(
            failure -> castToCorrectRuntimeException(failure.getException()));
        var doi = extractDoi();
        return new Reference.Builder().withPublicationInstance(publicationInstance)
                   .withPublishingContext(publicationContext)
                   .withDoi(doi)
                   .build();
    }

    @SuppressWarnings({"PMD.NPathComplexity"})
    private PublicationContext buildPublicationContext()
        throws InvalidIsbnException, InvalidIssnException, InvalidUnconfirmedSeriesException {
        if (isBook(cristinObject)) {
            return new NvaBookBuilder(cristinObject, channelRegistryMapper, s3Client).buildBookForPublicationContext();
        }
        if (isJournal(cristinObject)) {
            return new PeriodicalBuilder(cristinObject, channelRegistryMapper, s3Client)
                       .buildPeriodicalForPublicationContext();
        }
        if (isMediaFeatureArticle(cristinObject)) {
            return new MediaPeriodicalBuilder(cristinObject,
                                              channelRegistryMapper, s3Client)
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
        if (isInformationalMaterialThatShouldBeMapped()) {
            return InformationMaterialBuilder.buildPublicationContext(cristinObject);
        }
        return null;
    }

    private boolean isInformationalMaterialThatShouldBeMapped() {
        return isInformationMaterial(cristinObject) && NIFU_CUSTOMER_NAME.equals(cristinObject.getOwnerCodeCreated());
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
        if (hasJournalTitle()) {
            return Optional.ofNullable(cristinObject.getJournalPublication())
                       .map(CristinJournalPublication::getJournal)
                       .map(CristinJournalPublicationJournal::getJournalTitle);
        } else {
            return Optional.ofNullable(cristinObject.getMediaContribution())
                       .map(CristinMediaContribution::getMediaPlaceName);
        }
    }

    private boolean hasJournalTitle() {
        return Optional.ofNullable(cristinObject.getJournalPublication())
                   .map(CristinJournalPublication::getJournal)
                   .map(CristinJournalPublicationJournal::getJournalTitle)
                   .isPresent();
    }

    private boolean isWrittenInterview(CristinObject cristinObject) {
        return CristinSecondaryCategory.WRITTEN_INTERVIEW.equals(cristinObject.getSecondaryCategory());
    }

    private PublicationContext buildPublicationContextWhenMainCategoryIsReport()
        throws InvalidIsbnException, InvalidIssnException, InvalidUnconfirmedSeriesException {
        if (isDegreePhd(cristinObject) || isDegreeMaster(cristinObject) || isDegreeLicentiate(cristinObject)) {
            return new NvaDegreeBuilder(cristinObject, channelRegistryMapper, s3Client).buildDegree();
        }
        return new NvaReportBuilder(cristinObject, channelRegistryMapper, s3Client).buildNvaReport();
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
        return nonNull(from) || nonNull(to) ? new Period(from, to) : null;
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
        var doi = Stream.of(getBookOrReportPart(), getBookOrReportDoi(), getJournalDOi())
                      .filter(Objects::nonNull)
                      .distinct()
                      .findFirst()
                      .orElse(null);
        try {
            return doiConverter.toUri(doi);
        } catch (Exception e) {
            ErrorReport.exceptionName(InvalidDoiException.class.getSimpleName())
                .withCristinId(cristinObject.getId())
                .withBody(doi)
                .persist(s3Client);
            return null;
        }
    }

    private String getBookOrReportPart() {
        return Optional.ofNullable(extractCristinBookReportPart())
                   .map(CristinBookOrReportPartMetadata::getDoi)
                   .orElse(null);
    }

    private String getBookOrReportDoi() {
        return Optional.ofNullable(extractCristinBookReport())
                   .map(CristinBookOrReportMetadata::getDoi)
                   .orElse(null);
    }

    private String getJournalDOi() {
        return Optional.ofNullable(extractCristinJournalPublication())
                   .map(CristinJournalPublication::getDoi)
                   .orElse(null);
    }
}
