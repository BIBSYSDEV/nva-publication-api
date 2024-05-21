package no.sikt.nva.brage.migration.mapper;

import static java.util.Objects.nonNull;
import static no.sikt.nva.brage.migration.mapper.PublicationInstanceMapper.isAnthology;
import static no.sikt.nva.brage.migration.mapper.PublicationInstanceMapper.isConferenceReport;
import static no.sikt.nva.brage.migration.mapper.PublicationInstanceMapper.isEditorial;
import static no.sikt.nva.brage.migration.mapper.PublicationInstanceMapper.isExhibitionCatalog;
import static no.sikt.nva.brage.migration.mapper.PublicationInstanceMapper.isLiteraryArts;
import static no.sikt.nva.brage.migration.mapper.PublicationInstanceMapper.isPopularScienceMonograph;
import static no.sikt.nva.brage.migration.mapper.PublicationInstanceMapper.isProfessionalArticle;
import static no.sikt.nva.brage.migration.mapper.PublicationInstanceMapper.isReaderOpinion;
import static no.sikt.nva.brage.migration.mapper.PublicationInstanceMapper.isVisualArts;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import no.sikt.nva.brage.migration.NvaType;
import no.sikt.nva.brage.migration.lambda.PublicationContextException;
import no.sikt.nva.brage.migration.record.EntityDescription;
import no.sikt.nva.brage.migration.record.Publication;
import no.sikt.nva.brage.migration.record.PublicationDate;
import no.sikt.nva.brage.migration.record.PublicationDateNva;
import no.sikt.nva.brage.migration.record.Record;
import no.unit.nva.model.Course;
import no.unit.nva.model.UnconfirmedCourse;
import no.unit.nva.model.contexttypes.Anthology;
import no.unit.nva.model.contexttypes.Artistic;
import no.unit.nva.model.contexttypes.Book;
import no.unit.nva.model.contexttypes.BookSeries;
import no.unit.nva.model.contexttypes.Degree;
import no.unit.nva.model.contexttypes.Event;
import no.unit.nva.model.contexttypes.GeographicalContent;
import no.unit.nva.model.contexttypes.Journal;
import no.unit.nva.model.contexttypes.MediaContribution;
import no.unit.nva.model.contexttypes.PublicationContext;
import no.unit.nva.model.contexttypes.Publisher;
import no.unit.nva.model.contexttypes.PublishingHouse;
import no.unit.nva.model.contexttypes.Report;
import no.unit.nva.model.contexttypes.ResearchData;
import no.unit.nva.model.contexttypes.Series;
import no.unit.nva.model.contexttypes.UnconfirmedJournal;
import no.unit.nva.model.contexttypes.UnconfirmedMediaContributionPeriodical;
import no.unit.nva.model.contexttypes.UnconfirmedPublisher;
import no.unit.nva.model.contexttypes.UnconfirmedSeries;
import no.unit.nva.model.contexttypes.media.MediaFormat;
import no.unit.nva.model.contexttypes.media.MediaSubType;
import no.unit.nva.model.contexttypes.media.MediaSubTypeEnum;
import no.unit.nva.model.exceptions.InvalidIsbnException;
import no.unit.nva.model.exceptions.InvalidIssnException;
import no.unit.nva.model.exceptions.InvalidUnconfirmedSeriesException;
import nva.commons.core.Environment;
import nva.commons.core.StringUtils;
import nva.commons.core.paths.UriWrapper;
import org.joda.time.DateTime;

@SuppressWarnings("PMD.GodClass")
public final class PublicationContextMapper {

    public static final String PUBLICATION_CHANNELS_V_2 = "publication-channels-v2";
    public static final String DOMAIN_NAME_ENVIRONMENT_VARIABLE_NAME = "DOMAIN_NAME";
    public static final String HTTPS_PREFIX = "https://";
    public static final URI CHANNEL_REGISTRY_V_2 = readChannelRegistryPathFromEnvironment();
    public static final String NOT_SUPPORTED_TYPE = "Not supported type for creating publication context: ";
    public static final int HAS_BOTH_SERIES_TITLE_AND_SERIES_NUMBER = 2;
    public static final String CURRENT_YEAR = getCurrentYear();
    public static final int SIZE_ONE = 1;

    private PublicationContextMapper() {
    }

    @SuppressWarnings({"PMD.NPathComplexity", "PMD.CognitiveComplexity"})
    public static PublicationContext buildPublicationContext(Record brageRecord)
        throws InvalidIsbnException, InvalidUnconfirmedSeriesException, InvalidIssnException {
        if (shouldBeMappedToBook(brageRecord)) {
            return buildPublicationContextWhenBook(brageRecord);
        }
        if (isSupportedReportType(brageRecord)) {
            return buildPublicationContextWhenReport(brageRecord);
        }
        if (isUnconfirmedJournal(brageRecord) || isUnconfirmedScientificArticle(brageRecord)) {
            return buildPublicationContextForUnconfirmedJournal(brageRecord);
        }
        if (isArticle(brageRecord)) {
            return buildPublicationContextWhenJournalArticle(brageRecord);
        }
        if (isDegree(brageRecord)) {
            return buildPublicationContextWhenDegree(brageRecord);
        }
        if (isMap(brageRecord)) {
            return buildPublicationContextWhenMap(brageRecord);
        }
        if (isChapter(brageRecord) || isScientificChapter(brageRecord)) {
            return new Anthology();
        }
        if (isEvent(brageRecord)) {
            return buildPublicationContextWhenEvent();
        }
        if (isArtistic(brageRecord)) {
            return new Artistic();
        }
        if (isDataset(brageRecord)) {
            return buildPublicationContextWhenDataSet(brageRecord);
        }
        if (isInterview(brageRecord)) {
            return buildPublicationContextWhenMediaContribution();
        }
        if (isReaderOpinion(brageRecord)) {
            return buildPublicationContextWhenReaderOpinion();
        }
        if (isCristinRecord(brageRecord)) {
            return null;
        } else {
            throw new PublicationContextException(NOT_SUPPORTED_TYPE + brageRecord.getType().getNva());
        }
    }

    private static boolean isEvent(Record brageRecord) {
        return isLecture(brageRecord)
               || isConferencePoster(brageRecord)
               || isOtherPresentation(brageRecord)
               || isConferenceLecture(brageRecord);
    }

    public static boolean isConferenceLecture(Record record) {
        return NvaType.CONFERENCE_LECTURE.getValue().equals(record.getType().getNva());
    }

    public static boolean isCristinRecord(Record record) {
        return NvaType.CRISTIN_RECORD.getValue().equals(record.getType().getNva());
    }

    public static boolean isFilm(Record brageRecord) {
        return NvaType.FILM.getValue().equals(brageRecord.getType().getNva());
    }

    public static boolean isPerformingArts(Record brageRecord) {
        return NvaType.PERFORMING_ARTS.getValue().equals(brageRecord.getType().getNva());
    }

    public static boolean isTextbook(Record brageRecord) {
        return NvaType.TEXTBOOK.getValue().equals(brageRecord.getType().getNva());
    }

    public static boolean isOtherPresentation(Record brageRecord) {
        return NvaType.PRESENTATION_OTHER.getValue().equals(brageRecord.getType().getNva());
    }

    public static boolean isSupportedReportType(Record brageRecord) {
        return isReport(brageRecord)
               || isResearchReport(brageRecord)
               || isReportWorkingPaper(brageRecord)
               || isConferenceReport(brageRecord);
    }

    public static boolean isMusic(Record brageRecord) {
        return NvaType.RECORDING_MUSICAL.getValue().equals(brageRecord.getType().getNva());
    }

    public static boolean isStudentPaper(Record brageRecord) {
        return NvaType.STUDENT_PAPER.getValue().equals(brageRecord.getType().getNva());
    }

    public static boolean isChapter(Record brageRecord) {
        return NvaType.CHAPTER.getValue().equals(brageRecord.getType().getNva());
    }

    public static boolean isMediaFeatureArticle(Record brageRecord) {
        return NvaType.MEDIA_FEATURE_ARTICLE.getValue().equals(brageRecord.getType().getNva());
    }

    public static boolean isScientificMonograph(Record brageRecord) {
        return NvaType.SCIENTIFIC_MONOGRAPH.getValue().equals(brageRecord.getType().getNva());
    }

    public static boolean isOtherStudentWork(Record brageRecord) {
        return NvaType.STUDENT_PAPER_OTHERS.getValue().equals(brageRecord.getType().getNva());
    }

    public static boolean isDataset(Record brageRecord) {
        return NvaType.DATASET.getValue().equals(brageRecord.getType().getNva());
    }

    public static boolean isResearchReport(Record brageRecord) {
        return NvaType.RESEARCH_REPORT.getValue().equals(brageRecord.getType().getNva());
    }

    public static boolean isBook(Record brageRecord) {
        return NvaType.BOOK.getValue().equals(brageRecord.getType().getNva());
    }

    public static boolean isScientificArticle(Record brageRecord) {
        return NvaType.SCIENTIFIC_ARTICLE.getValue().equals(brageRecord.getType().getNva());
    }

    public static boolean isReportWorkingPaper(Record brageRecord) {
        return NvaType.WORKING_PAPER.getValue().equals(brageRecord.getType().getNva());
    }

    public static boolean isLecture(Record brageRecord) {
        return NvaType.LECTURE.getValue().equals(brageRecord.getType().getNva());
    }

    public static boolean isDesignProduct(Record brageRecord) {
        return NvaType.DESIGN_PRODUCT.getValue().equals(brageRecord.getType().getNva());
    }

    public static boolean isScientificChapter(Record brageRecord) {
        return NvaType.SCIENTIFIC_CHAPTER.getValue().equals(brageRecord.getType().getNva());
    }

    public static boolean isPlanOrBlueprint(Record brageRecord) {
        return NvaType.PLAN_OR_BLUEPRINT.getValue().equals(brageRecord.getType().getNva());
    }

    public static boolean isConferencePoster(Record brageRecord) {
        return NvaType.CONFERENCE_POSTER.getValue().equals(brageRecord.getType().getNva());
    }

    public static boolean isInterview(Record brageRecord) {
        return NvaType.INTERVIEW.getValue().equals(brageRecord.getType().getNva());
    }

    private static boolean shouldBeMappedToBook(Record record) {
        return isBook(record)
               || isScientificMonograph(record)
               || isOtherStudentWork(record)
               || isStudentPaper(record)
               || isAnthology(record)
               || isTextbook(record)
               || isExhibitionCatalog(record)
               || isPopularScienceMonograph(record);
    }

    private static PublicationContext buildPublicationContextWhenReaderOpinion() throws InvalidIssnException {
        return new UnconfirmedMediaContributionPeriodical(null, null, null);
    }

    private static boolean isArtistic(Record brageRecord) {
        return isDesignProduct(brageRecord)
               || isMusic(brageRecord)
               || isPlanOrBlueprint(brageRecord)
               || isPerformingArts(brageRecord)
               || isVisualArts(brageRecord)
               || isFilm(brageRecord)
               || isLiteraryArts(brageRecord);
    }

    private static boolean isArticle(Record brageRecord) {
        return isJournalArticle(brageRecord)
               || isScientificArticle(brageRecord)
               || isMediaFeatureArticle(brageRecord)
               || isProfessionalArticle(brageRecord)
               || isEditorial(brageRecord);
    }

    private static PublicationContext buildPublicationContextWhenMediaContribution() {
        return new MediaContribution.Builder().withFormat(MediaFormat.TEXT)
                   .withMedium(MediaSubType.create(MediaSubTypeEnum.OTHER))
                   .build();
    }

    private static boolean isReport(Record brageRecord) {
        return NvaType.REPORT.getValue().equals(brageRecord.getType().getNva());
    }

    private static PublicationContext buildPublicationContextWhenEvent() {
        return new Event.Builder().build();
    }

    private static boolean isUnconfirmedScientificArticle(Record brageRecord) {
        return NvaType.SCIENTIFIC_ARTICLE.getValue().equals(brageRecord.getType().getNva()) && !hasJournalId(
            brageRecord);
    }

    private static PublicationContext buildPublicationContextForUnconfirmedJournal(Record brageRecord)
        throws InvalidIssnException {
        var issnList = extractIssnList(brageRecord);
        if (issnList.size() > SIZE_ONE) {
            return new UnconfirmedJournal(extractJournalTitle(brageRecord), issnList.get(0), issnList.get(1));
        } else {
            var issn = !issnList.isEmpty() ? issnList.get(0) : null;
            return new UnconfirmedJournal(extractJournalTitle(brageRecord), issn, null);
        }
    }

    private static List<String> extractIssnList(Record brageRecord) {
        return Optional.ofNullable(brageRecord.getPublication())
                   .map(Publication::getIssnList)
                   .orElse(Collections.emptyList());
    }

    private static String extractJournalTitle(Record brageRecord) {
        return Optional.ofNullable(brageRecord.getPublication()).map(Publication::getJournal).orElse(null);
    }

    private static boolean isUnconfirmedJournal(Record brageRecord) {
        return NvaType.JOURNAL_ARTICLE.getValue().equals(brageRecord.getType().getNva()) && !hasJournalId(brageRecord);
    }

    private static URI readChannelRegistryPathFromEnvironment() {
        var basePath = new Environment().readEnv(DOMAIN_NAME_ENVIRONMENT_VARIABLE_NAME);
        return UriWrapper.fromUri(HTTPS_PREFIX + basePath).addChild(PUBLICATION_CHANNELS_V_2).getUri();
    }

    private static PublicationContext buildPublicationContextWhenJournalArticle(Record brageRecord)
        throws InvalidIssnException {
        return extractJournal(brageRecord);
    }

    private static boolean isJournalArticle(Record brageRecord) {
        return NvaType.JOURNAL_ARTICLE.getValue().equals(brageRecord.getType().getNva()) && hasJournalId(brageRecord);
    }

    private static boolean hasJournalId(Record brageRecord) {
        return nonNull(extractJournalId(brageRecord));
    }

    private static String extractJournalId(Record brageRecord) {
        return Optional.ofNullable(brageRecord.getPublication())
                   .map(Publication::getPublicationContext)
                   .map(no.sikt.nva.brage.migration.record.PublicationContext::getJournal)
                   .map(no.sikt.nva.brage.migration.record.Journal::getPid)
                   .map(String::toString)
                   .orElse(null);
    }

    private static PublicationContext buildPublicationContextWhenDataSet(Record brageRecord) {
        return new ResearchData(extractPublisher(brageRecord));
    }

    private static boolean isMap(Record brageRecord) {
        return NvaType.MAP.getValue().equals(brageRecord.getType().getNva());
    }

    private static PublicationContext buildPublicationContextWhenMap(Record brageRecord) {
        return new GeographicalContent(extractPublisher(brageRecord));
    }

    private static boolean isDegree(Record brageRecord) {
        return NvaType.BACHELOR_THESIS.getValue().equals(brageRecord.getType().getNva())
               || NvaType.MASTER_THESIS.getValue().equals(brageRecord.getType().getNva())
               || NvaType.DOCTORAL_THESIS.getValue().equals(brageRecord.getType().getNva());
    }

    private static PublicationContext buildPublicationContextWhenDegree(Record brageRecord)
        throws InvalidIsbnException, InvalidUnconfirmedSeriesException, InvalidIssnException {
        return new Degree.Builder().withIsbnList(extractIsbnList(brageRecord))
                   .withSeries(extractSeries(brageRecord))
                   .withPublisher(extractPublisher(brageRecord))
                   .withSeriesNumber(extractSeriesNumber(brageRecord))
                   .withCourse(extractCourse(brageRecord))
                   .build();
    }

    private static Course extractCourse(Record brageRecord) {
        return nonNull(brageRecord.getSubjectCode()) ? new UnconfirmedCourse(brageRecord.getSubjectCode()) : null;
    }

    private static List<String> extractIsbnList(Record brageRecord) {
        return Optional.ofNullable(brageRecord.getPublication())
                   .map(Publication::getIsbnList)
                   .orElse(Collections.emptyList())
                   .stream()
                   .filter(StringUtils::isNotBlank)
                   .collect(Collectors.toList());
    }

    private static String extractYear(Record brageRecord) {
        return Optional.ofNullable(brageRecord.getEntityDescription())
                   .map(EntityDescription::getPublicationDate)
                   .map(PublicationDate::getNva)
                   .map(PublicationDateNva::getYear)
                   .orElse(null);
    }

    private static String extractPotentialSeriesNumberValue(String potentialSeriesNumber) {
        if (potentialSeriesNumber.contains(":")) {
            var seriesNumberAndYear = Arrays.asList(potentialSeriesNumber.split(":"));
            return Collections.max(seriesNumberAndYear);
        }

        if (potentialSeriesNumber.contains("/")) {
            var seriesNumberAndYear = Arrays.asList(potentialSeriesNumber.split("/"));
            return Collections.max(seriesNumberAndYear);
        }
        return potentialSeriesNumber;
    }

    private static PublicationContext buildPublicationContextWhenBook(Record brageRecord)
        throws InvalidIssnException {
        return new Book.BookBuilder().withPublisher(extractPublisher(brageRecord))
                   .withSeries(extractSeries(brageRecord))
                   .withIsbnList(extractIsbnList(brageRecord))
                   .withSeriesNumber(extractSeriesNumber(brageRecord))
                   .build();
    }

    private static PublicationContext buildPublicationContextWhenReport(Record brageRecord)
        throws InvalidUnconfirmedSeriesException, InvalidIssnException {
        return new Report.Builder().withPublisher(extractPublisher(brageRecord))
                   .withSeries(extractSeries(brageRecord))
                   .withIsbnList(extractIsbnList(brageRecord))
                   .withSeriesNumber(extractSeriesNumber(brageRecord))
                   .withSeriesNumber(brageRecord.getEntityDescription().getPublicationInstance().getIssue())
                   .build();
    }

    private static String extractSeriesNumber(Record brageRecord) {
        return Optional.ofNullable(brageRecord.getPublication())
                   .map(Publication::getPartOfSeries)
                   .map(PublicationContextMapper::extractPartOfSeriesValue)
                   .orElse(null);
    }

    private static String extractPartOfSeriesValue(String partOfSeriesValue) {
        return Optional.ofNullable(partOfSeriesValue)
                   .map(value -> hasNumber(value) ? extractPotentialSeriesNumberValue(getNumber(value)) : null)
                   .orElse(null);
    }

    private static String getNumber(String value) {
        return value.split(";")[1];
    }

    private static boolean hasNumber(String value) {
        return value.split(";").length == HAS_BOTH_SERIES_TITLE_AND_SERIES_NUMBER;
    }

    @SuppressWarnings("PMD.NullAssignment")
    private static BookSeries extractSeries(Record brageRecord) throws InvalidIssnException {
        return Optional.ofNullable(brageRecord.getPublication().getPublicationContext())
                   .map(no.sikt.nva.brage.migration.record.PublicationContext::getSeries)
                   .map(no.sikt.nva.brage.migration.record.Series::getPid)
                   .map(pid -> generateSeries(pid, extractYear(brageRecord)))
                   .orElse(isSupportedReportType(brageRecord) ? generateUnconfirmedSeries(brageRecord) : null);
    }

    private static BookSeries generateUnconfirmedSeries(Record brageRecord) throws InvalidIssnException {
        var issnList = extractIssnList(brageRecord);
        if (issnList.size() > SIZE_ONE) {
            return new UnconfirmedSeries(generateUnconfirmedSeriesTitle(brageRecord), issnList.get(0), issnList.get(1));
        } else {
            var issn = !issnList.isEmpty() ? issnList.get(0) : null;
            return new UnconfirmedSeries(generateUnconfirmedSeriesTitle(brageRecord), issn, null);
        }
    }

    private static String generateUnconfirmedSeriesTitle(Record brageRecord) {
        return Optional.ofNullable(brageRecord.getPublication())
                   .map(Publication::getPartOfSeries)
                   .map(partOfSeriesValue -> partOfSeriesValue.split(";")[0])
                   .orElse(null);
    }

    private static PublishingHouse extractPublisher(Record brageRecord) {
        return Optional.ofNullable(brageRecord.getPublication().getPublicationContext())
                   .map(no.sikt.nva.brage.migration.record.PublicationContext::getPublisher)
                   .map(no.sikt.nva.brage.migration.record.Publisher::getPid)
                   .map(pid -> generatePublisher(pid, extractYear(brageRecord)))
                   .orElse(generateUnconfirmedPublisher(brageRecord));
    }

    private static UnconfirmedPublisher generateUnconfirmedPublisher(Record brageRecord) {
        return Optional.ofNullable(brageRecord.getPublication().getPublicationContext())
                   .map(no.sikt.nva.brage.migration.record.PublicationContext::getBragePublisher)
                   .map(UnconfirmedPublisher::new)
                   .orElse(null);
    }

    private static PublicationContext extractJournal(Record brageRecord) throws InvalidIssnException {
        return Optional.ofNullable(brageRecord.getPublication().getPublicationContext())
                   .map(no.sikt.nva.brage.migration.record.PublicationContext::getJournal)
                   .map(no.sikt.nva.brage.migration.record.Journal::getPid)
                   .map(pid -> generateJournal(pid, extractYear(brageRecord)))
                   .orElse(buildPublicationContextForUnconfirmedJournal(brageRecord));
    }

    private static PublishingHouse generatePublisher(String publisherPid, String year) {
        return new Publisher(UriWrapper.fromUri(PublicationContextMapper.CHANNEL_REGISTRY_V_2)
                                 .addChild(ChannelType.PUBLISHER.getType())
                                 .addChild(publisherPid)
                                 .addChild(nonNull(year) ? year : CURRENT_YEAR)
                                 .getUri());
    }

    private static String getCurrentYear() {
        return String.valueOf(DateTime.now().getYear());
    }

    private static PublicationContext generateJournal(String journalPid, String year) {
        return new Journal(UriWrapper.fromUri(PublicationContextMapper.CHANNEL_REGISTRY_V_2)
                               .addChild(ChannelType.JOURNAL.getType())
                               .addChild(journalPid)
                               .addChild(nonNull(year) ? year : CURRENT_YEAR)
                               .getUri());
    }

    private static BookSeries generateSeries(String seriesPid, String year) {
        return new Series(UriWrapper.fromUri(PublicationContextMapper.CHANNEL_REGISTRY_V_2)
                              .addChild(ChannelType.SERIES.getType())
                              .addChild(seriesPid)
                              .addChild(nonNull(year) ? year : CURRENT_YEAR)
                              .getUri());
    }
}
