package no.sikt.nva.brage.migration.testutils;

import static java.util.Objects.nonNull;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.print.attribute.standard.Media;
import no.sikt.nva.brage.migration.NvaType;
import no.sikt.nva.brage.migration.mapper.ChannelType;
import no.sikt.nva.brage.migration.mapper.PublicationContextMapper;
import no.sikt.nva.brage.migration.testutils.NvaBrageMigrationDataGenerator.Builder;
import no.unit.nva.model.Reference;
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
import no.unit.nva.model.contexttypes.Report;
import no.unit.nva.model.contexttypes.ResearchData;
import no.unit.nva.model.contexttypes.Series;
import no.unit.nva.model.contexttypes.UnconfirmedJournal;
import no.unit.nva.model.contexttypes.UnconfirmedSeries;
import no.unit.nva.model.contexttypes.media.MediaFormat;
import no.unit.nva.model.contexttypes.media.MediaSubType;
import no.unit.nva.model.contexttypes.media.MediaSubTypeEnum;
import no.unit.nva.model.exceptions.InvalidIsbnException;
import no.unit.nva.model.exceptions.InvalidIssnException;
import no.unit.nva.model.exceptions.InvalidUnconfirmedSeriesException;
import no.unit.nva.model.instancetypes.Map;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.artistic.architecture.Architecture;
import no.unit.nva.model.instancetypes.artistic.architecture.ArchitectureSubtype;
import no.unit.nva.model.instancetypes.artistic.design.ArtisticDesign;
import no.unit.nva.model.instancetypes.artistic.design.ArtisticDesignSubtype;
import no.unit.nva.model.instancetypes.artistic.music.MusicPerformance;
import no.unit.nva.model.instancetypes.artistic.performingarts.PerformingArts;
import no.unit.nva.model.instancetypes.artistic.performingarts.PerformingArtsSubtype;
import no.unit.nva.model.instancetypes.artistic.visualarts.VisualArts;
import no.unit.nva.model.instancetypes.artistic.visualarts.VisualArtsSubtype;
import no.unit.nva.model.instancetypes.artistic.visualarts.VisualArtsSubtypeEnum;
import no.unit.nva.model.instancetypes.book.AcademicMonograph;
import no.unit.nva.model.instancetypes.book.BookAnthology;
import no.unit.nva.model.instancetypes.book.BookMonograph;
import no.unit.nva.model.instancetypes.book.NonFictionMonograph;
import no.unit.nva.model.instancetypes.chapter.AcademicChapter;
import no.unit.nva.model.instancetypes.chapter.NonFictionChapter;
import no.unit.nva.model.instancetypes.degree.DegreeBachelor;
import no.unit.nva.model.instancetypes.degree.DegreeMaster;
import no.unit.nva.model.instancetypes.degree.DegreePhd;
import no.unit.nva.model.instancetypes.degree.OtherStudentWork;
import no.unit.nva.model.instancetypes.event.ConferencePoster;
import no.unit.nva.model.instancetypes.event.Lecture;
import no.unit.nva.model.instancetypes.event.OtherPresentation;
import no.unit.nva.model.instancetypes.journal.AcademicArticle;
import no.unit.nva.model.instancetypes.journal.FeatureArticle;
import no.unit.nva.model.instancetypes.journal.JournalArticle;
import no.unit.nva.model.instancetypes.journal.ProfessionalArticle;
import no.unit.nva.model.instancetypes.media.MediaInterview;
import no.unit.nva.model.instancetypes.media.MediaReaderOpinion;
import no.unit.nva.model.instancetypes.report.ReportBasic;
import no.unit.nva.model.instancetypes.report.ReportResearch;
import no.unit.nva.model.instancetypes.report.ReportWorkingPaper;
import no.unit.nva.model.instancetypes.researchdata.DataSet;
import no.unit.nva.model.instancetypes.researchdata.GeographicalDescription;
import no.unit.nva.model.pages.MonographPages;
import no.unit.nva.model.pages.Pages;
import no.unit.nva.model.pages.Range;
import nva.commons.core.paths.UriWrapper;
import org.jetbrains.annotations.NotNull;
import org.joda.time.DateTime;

public final class ReferenceGenerator {

    public static final String CURRENT_YEAR = String.valueOf(DateTime.now().getYear());

    public static Reference generateReference(Builder builder) {
        return Optional.ofNullable(builder)
                   .map(ReferenceGenerator::buildReference)
                   .orElse(new Reference());
    }

    private static Reference buildReference(Builder builder) {
        try {
            if (NvaType.BOOK.getValue().equals(builder.getType().getNva())) {
                return new Reference.Builder()
                           .withPublishingContext(generatePublicationContextForBook(builder))
                           .withPublicationInstance(generatePublicationInstanceForBook(builder))
                           .withDoi(builder.getDoi())
                           .build();
            }
            if (NvaType.ANTHOLOGY.getValue().equals(builder.getType().getNva())) {
                return new Reference.Builder()
                           .withPublishingContext(generatePublicationContextForBook(builder))
                           .withPublicationInstance(generatePublicationInstanceForAnthology(builder))
                           .withDoi(builder.getDoi())
                           .build();
            }
            if (NvaType.MAP.getValue().equals(builder.getType().getNva())) {
                return new Reference.Builder().withPublicationInstance(
                        new Map(builder.getDescriptionsForPublication(), builder.getMonographPages()))
                           .withPublishingContext(new GeographicalContent(generatePublisher(builder)))
                           .withDoi(builder.getDoi())
                           .build();
            }
            if (NvaType.DATASET.getValue().equals(builder.getType().getNva())) {
                return new Reference.Builder()
                           .withPublicationInstance(getPublicationInstanceForDataset(builder))
                           .withPublishingContext(new ResearchData(generatePublisher(builder)))
                           .withDoi(builder.getDoi())
                           .build();
            }
            if (NvaType.REPORT.getValue().equals(builder.getType().getNva())) {
                return new Reference.Builder()
                           .withPublicationInstance(generatePublicationInstanceForReport(builder))
                           .withPublishingContext(generatePublicationContextForReport(builder))
                           .withDoi(builder.getDoi())
                           .build();
            }
            if (NvaType.RESEARCH_REPORT.getValue().equals(builder.getType().getNva())) {
                return new Reference.Builder()
                           .withPublicationInstance(generatePublicationInstanceForResearchReport(builder))
                           .withPublishingContext(generatePublicationContextForReport(builder))
                           .withDoi(builder.getDoi())
                           .build();
            }
            if (NvaType.WORKING_PAPER.getValue().equals(builder.getType().getNva())) {
                return new Reference.Builder()
                           .withPublicationInstance(generatePublicationInstanceForReportWorkingPaper(builder))
                           .withPublishingContext(generatePublicationContextForReport(builder))
                           .withDoi(builder.getDoi())
                           .build();
            }
            if (NvaType.BACHELOR_THESIS.getValue().equals(builder.getType().getNva())) {
                return new Reference.Builder()
                           .withPublishingContext(generatePublicationContextForDegree(builder))
                           .withPublicationInstance(generatePublicationInstanceForBachelorDegree(builder))
                           .withDoi(builder.getDoi())
                           .build();
            }
            if (NvaType.MASTER_THESIS.getValue().equals(builder.getType().getNva())) {
                return new Reference.Builder()
                           .withPublishingContext(generatePublicationContextForDegree(builder))
                           .withPublicationInstance(generatePublicationInstanceForMasterDegree(builder))
                           .withDoi(builder.getDoi())
                           .build();
            }
            if (NvaType.DOCTORAL_THESIS.getValue().equals(builder.getType().getNva())) {
                return new Reference.Builder()
                           .withPublishingContext(generatePublicationContextForDegree(builder))
                           .withPublicationInstance(generatePublicationInstanceForPhd(builder))
                           .withDoi(builder.getDoi())
                           .build();
            }
            if (NvaType.JOURNAL_ARTICLE.getValue().equals(builder.getType().getNva()) && hasJournalId(builder)) {
                return new Reference.Builder()
                           .withPublishingContext(generateJournal(builder))
                           .withPublicationInstance(generatePublicationInstanceForJournalArticle(builder))
                           .withDoi(builder.getDoi())
                           .build();
            }
            if (NvaType.CHRONICLE.getValue().equals(builder.getType().getNva()) && hasJournalId(builder)) {
                return new Reference.Builder()
                           .withPublishingContext(generateJournal(builder))
                           .withPublicationInstance(generatePublicationInstanceForFeatureArticle(builder))
                           .withDoi(builder.getDoi())
                           .build();
            }
            if (NvaType.JOURNAL_ARTICLE.getValue().equals(builder.getType().getNva()) && !hasJournalId(builder)) {
                return new Reference.Builder()
                           .withPublishingContext(generateUnconfirmedJournal(builder))
                           .withPublicationInstance(generatePublicationInstanceForJournalArticle(builder))
                           .withDoi(builder.getDoi())
                           .build();
            }
            if (NvaType.SCIENTIFIC_ARTICLE.getValue().equals(builder.getType().getNva()) && hasJournalId(builder)) {
                return new Reference.Builder()
                           .withPublishingContext(generateJournal(builder))
                           .withPublicationInstance(generatePublicationInstanceForScientificArticle(builder))
                           .withDoi(builder.getDoi())
                           .build();
            }
            if (NvaType.STUDENT_PAPER_OTHERS.getValue().equals(builder.getType().getNva())
                || NvaType.STUDENT_PAPER.getValue().equals(builder.getType().getNva())) {
                return new Reference.Builder()
                           .withPublishingContext(generatePublicationContextForOtherStudentWork(builder))
                           .withPublicationInstance(generatePublicationInstanceForStudentPaper(builder))
                           .withDoi(builder.getDoi())
                           .build();
            }
            if (NvaType.SCIENTIFIC_MONOGRAPH.getValue().equals(builder.getType().getNva())) {
                return new Reference.Builder()
                           .withPublishingContext(generatePublicationContextForBook(builder))
                           .withPublicationInstance(generatePublicationInstanceForScientificMonograph(builder))
                           .withDoi(builder.getDoi())
                           .build();
            }
            if (NvaType.LECTURE.getValue().equals(builder.getType().getNva())) {
                return new Reference.Builder()
                           .withPublishingContext(new Event.Builder().build())
                           .withPublicationInstance(new Lecture())
                           .withDoi(builder.getDoi())
                           .build();
            }
            if (NvaType.CHAPTER.getValue().equals(builder.getType().getNva())) {
                return new Reference.Builder()
                           .withPublishingContext(new Anthology())
                           .withPublicationInstance(generatePublicationInstanceForChapter(builder))
                           .withDoi(builder.getDoi())
                           .build();
            }
            if (NvaType.SCIENTIFIC_CHAPTER.getValue().equals(builder.getType().getNva())) {
                return new Reference.Builder()
                           .withPublishingContext(new Anthology())
                           .withPublicationInstance(generatePublicationInstanceForScientificChapter(builder))
                           .withDoi(builder.getDoi())
                           .build();
            }
            if (NvaType.SCIENTIFIC_ARTICLE.getValue().equals(builder.getType().getNva())) {
                return new Reference.Builder()
                           .withPublishingContext(new Anthology())
                           .withPublicationInstance(generatePublicationInstanceForScientificArticle(builder))
                           .withDoi(builder.getDoi())
                           .build();
            }
            if (NvaType.DESIGN_PRODUCT.getValue().equals(builder.getType().getNva())) {
                return new Reference.Builder()
                           .withPublishingContext(new Artistic())
                           .withPublicationInstance(generatePublicationInstanceForDesignProduct())
                           .withDoi(builder.getDoi())
                           .build();
            }
            if (NvaType.RECORDING_MUSICAL.getValue().equals(builder.getType().getNva())) {
                return new Reference.Builder()
                           .withPublishingContext(new Artistic())
                           .withPublicationInstance(new MusicPerformance(Collections.emptyList()))
                           .withDoi(builder.getDoi())
                           .build();
            }
            if (NvaType.PLAN_OR_BLUEPRINT.getValue().equals(builder.getType().getNva())) {
                return new Reference.Builder()
                           .withPublishingContext(new Artistic())
                           .withPublicationInstance(generatePublicationInstanceForArchitecture())
                           .withDoi(builder.getDoi())
                           .build();
            }
            if (NvaType.CONFERENCE_POSTER.getValue().equals(builder.getType().getNva())) {
                return new Reference.Builder()
                           .withPublishingContext(new Event.Builder().build())
                           .withPublicationInstance(generatePublicationInstanceForConferencePoster())
                           .withDoi(builder.getDoi())
                           .build();
            }
            if (NvaType.INTERVIEW.getValue().equals(builder.getType().getNva())) {
                return new Reference.Builder()
                           .withPublishingContext(generateMediaContribution())
                           .withPublicationInstance(new MediaInterview())
                           .withDoi(builder.getDoi())
                           .build();
            }
            if (NvaType.PRESENTATION_OTHER.getValue().equals(builder.getType().getNva())) {
                return new Reference.Builder()
                           .withPublishingContext(new Event.Builder().build())
                           .withPublicationInstance(new OtherPresentation())
                           .withDoi(builder.getDoi())
                           .build();
            }
            if (NvaType.PERFORMING_ARTS.getValue().equals(builder.getType().getNva())) {
                return new Reference.Builder()
                           .withPublishingContext(new Artistic())
                           .withPublicationInstance(generatePerformingArts())
                           .withDoi(builder.getDoi())
                           .build();
            }
            if (NvaType.PROFESSIONAL_ARTICLE.getValue().equals(builder.getType().getNva())) {
                return new Reference.Builder()
                           .withPublishingContext(generateJournal(builder))
                           .withPublicationInstance(new ProfessionalArticle(generateRange(builder), null, null, null))
                           .withDoi(builder.getDoi())
                           .build();
            }
            if (NvaType.READER_OPINION.getValue().equals(builder.getType().getNva())) {
                return new Reference.Builder()
                           .withPublishingContext(generateMediaContribution())
                           .withPublicationInstance(new MediaReaderOpinion(null, null, null, null))
                           .withDoi(builder.getDoi())
                           .build();
            }
            if (NvaType.VISUAL_ARTS.getValue().equals(builder.getType().getNva())) {
                return new Reference.Builder()
                           .withPublishingContext(new Artistic())
                           .withPublicationInstance(new VisualArts(VisualArtsSubtype.createOther(null),null, null))
                           .withDoi(builder.getDoi())
                           .build();
            }
            return new Reference.Builder().build();
        } catch (Exception e) {
            return new Reference.Builder().build();
        }
    }

    @NotNull
    private static MediaContribution generateMediaContribution() {
        return new MediaContribution.Builder().withFormat(MediaFormat.TEXT)
                   .withMedium(MediaSubType.create(MediaSubTypeEnum.OTHER)).build();
    }

    private static PerformingArts generatePerformingArts() {
        return new PerformingArts(PerformingArtsSubtype.createOther(null), null, List.of());    }

    private static PublicationInstance<? extends Pages> generatePublicationInstanceForAnthology(Builder builder) {
        return new BookAnthology(constructMonographPages(builder));
    }

    private static ConferencePoster generatePublicationInstanceForConferencePoster() {
        return new ConferencePoster();
    }

    private static OtherStudentWork generatePublicationInstanceForStudentPaper(Builder builder) {
        return new OtherStudentWork(builder.getMonographPages(), builder.getPublicationDateForPublication());
    }

    private static PublicationInstance<? extends Pages> generatePublicationInstanceForScientificChapter(
        Builder builder) {
        return new AcademicChapter(generateRange(builder));
    }

    private static PublicationInstance<? extends Pages> generatePublicationInstanceForChapter(
        Builder builder) {
        return new NonFictionChapter(generateRange(builder));
    }

    private static ArtisticDesign generatePublicationInstanceForDesignProduct() {
        return new ArtisticDesign(ArtisticDesignSubtype.createOther(
            null), null, Collections.emptyList());
    }

    private static Architecture generatePublicationInstanceForArchitecture() {
        return new Architecture(ArchitectureSubtype.createOther(null), null,
                                Collections.emptyList());
    }

    private static PublicationInstance<? extends Pages> generatePublicationInstanceForFeatureArticle(Builder builder) {
        return new FeatureArticle.Builder()
                   .withPages(generateRange(builder))
                   .build();
    }

    private static Range generateRange(Builder builder) {
        return new Range(builder.getPages().getRange().getBegin(), builder.getPages().getRange().getEnd());
    }

    private static PublicationInstance<? extends Pages> generatePublicationInstanceForReportWorkingPaper(
        Builder builder) {
        return new ReportWorkingPaper(builder.getMonographPages());
    }

    private static PublicationInstance<? extends Pages> generatePublicationInstanceForScientificArticle(
        Builder builder) {
        return new AcademicArticle(generateRange(builder), null, null, null);
    }

    private static PublicationContext generateUnconfirmedJournal(Builder builder) throws InvalidIssnException {
        if (builder.getIssnList().size() > 1) {
            return new UnconfirmedJournal(builder.getPublication().getJournal(),
                                          builder.getIssnList().get(0),
                                          builder.getIssnList().get(1));
        }
        if (builder.getIssnList().size() == 1) {
            return new UnconfirmedJournal(builder.getPublication().getJournal(),
                                          builder.getIssnList().get(0), null);
        } else {
            return new UnconfirmedJournal(builder.getPublication().getJournal(),
                                          null, null);
        }
    }

    private static boolean hasJournalId(Builder builder) {
        return nonNull(builder.getPublication().getPublicationContext().getJournal().getId());
    }

    private static PublicationContext generatePublicationContextForReport(Builder builder)
        throws InvalidIssnException, InvalidIsbnException, InvalidUnconfirmedSeriesException {
        return new Report.Builder().withIsbnList(Collections.singletonList(builder.getIsbn()))
                   .withSeries(generateSeries(builder))
                   .withSeriesNumber(builder.getSeriesNumberPublication())
                   .build();
    }

    private static BookMonograph generatePublicationInstanceForScientificMonograph(Builder builder) {
        return new AcademicMonograph(builder.getMonographPages());
    }

    private static PublicationContext generatePublicationContextForOtherStudentWork(Builder builder)
        throws InvalidIsbnException {
        return new Book.BookBuilder().withIsbnList(Collections.singletonList(builder.getIsbn())).build();
    }

    private static JournalArticle generatePublicationInstanceForJournalArticle(Builder builder) {
        return new ProfessionalArticle(generateRange(builder), null, null, null);
    }

    private static Publisher generatePublisher(Builder builder) {
        return new Publisher(UriWrapper.fromUri(PublicationContextMapper.CHANNEL_REGISTRY)
                                 .addChild(ChannelType.PUBLISHER.getType())
                                 .addChild(builder.getPublisherId())
                                 .addChild(nonNull(getYear(builder)) ? getYear(builder) : CURRENT_YEAR)
                                 .getUri());
    }

    private static String getYear(Builder builder) {
        return builder.getPublicationDate().getNva().getYear();
    }

    private static BookSeries generateSeries(Builder builder) throws InvalidIssnException {
        if (nonNull(builder.getSeriesId())) {
            return new Series(UriWrapper.fromUri(PublicationContextMapper.CHANNEL_REGISTRY)
                                  .addChild(ChannelType.JOURNAL.getType())
                                  .addChild(builder.getSeriesId())
                                  .addChild(nonNull(getYear(builder)) ? getYear(builder) : CURRENT_YEAR)
                                  .getUri());
        } else {
            return new UnconfirmedSeries(builder.getSeriesTitle(), builder.getIssnList().get(0),
                                         builder.getIssnList().get(1));
        }
    }

    private static Journal generateJournal(Builder builder) {
        return new Journal(UriWrapper.fromUri(PublicationContextMapper.CHANNEL_REGISTRY)
                               .addChild(ChannelType.JOURNAL.getType())
                               .addChild(builder.getJournalId())
                               .addChild(nonNull(getYear(builder)) ? getYear(builder) : CURRENT_YEAR)
                               .getUri());
    }

    private static DataSet getPublicationInstanceForDataset(Builder builder) {
        return new DataSet(false,
                           new GeographicalDescription(String.join(", ", builder.getSpatialCoverage())),
                           null, null, null);
    }

    private static DegreeBachelor generatePublicationInstanceForBachelorDegree(Builder builder) {
        return new DegreeBachelor(builder.getMonographPages(), builder.getPublicationDateForPublication());
    }

    private static DegreeMaster generatePublicationInstanceForMasterDegree(Builder builder) {
        return new DegreeMaster(builder.getMonographPages(), builder.getPublicationDateForPublication());
    }

    private static DegreePhd generatePublicationInstanceForPhd(Builder builder) {
        return new DegreePhd(builder.getMonographPages(), builder.getPublicationDateForPublication());
    }

    private static ReportBasic generatePublicationInstanceForReport(Builder builder) {
        return new ReportBasic(
            new MonographPages.Builder()
                .withPages(builder.getPages().getPages())
                .withIllustrated(false).build());
    }

    private static ReportResearch generatePublicationInstanceForResearchReport(Builder builder) {
        return new ReportResearch(new MonographPages.Builder()
                                      .withPages(builder.getPages().getPages())
                                      .withIllustrated(false).build());
    }

    private static Book generatePublicationContextForBook(Builder builder) throws InvalidIsbnException {
        return new Book.BookBuilder()
                   .withSeriesNumber(builder.getSeriesNumberPublication())
                   .withIsbnList(Collections.singletonList(builder.getIsbn()))
                   .build();
    }

    private static BookMonograph generatePublicationInstanceForBook(Builder builder) {
        var monographPages = constructMonographPages(builder);
        return new NonFictionMonograph(monographPages);
    }

    private static MonographPages constructMonographPages(Builder builder) {
        return new MonographPages.Builder().withIllustrated(false)
                   .withPages(builder.getPages().getPages())
                   .build();
    }

    private static Degree generatePublicationContextForDegree(Builder builder)
        throws InvalidIsbnException, InvalidUnconfirmedSeriesException {
        return new Degree.Builder().withIsbnList(Collections.singletonList(builder.getIsbn())).build();
    }
}
