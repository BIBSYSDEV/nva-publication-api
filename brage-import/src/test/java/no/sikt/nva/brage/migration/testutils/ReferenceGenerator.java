package no.sikt.nva.brage.migration.testutils;

import static java.util.Objects.nonNull;
import java.util.Collections;
import java.util.Optional;
import no.sikt.nva.brage.migration.mapper.ChannelType;
import no.sikt.nva.brage.migration.mapper.PublicationContextMapper;
import no.sikt.nva.brage.migration.testutils.NvaBrageMigrationDataGenerator.Builder;
import no.sikt.nva.brage.migration.testutils.type.NvaType;
import no.unit.nva.model.Reference;
import no.unit.nva.model.contexttypes.Book;
import no.unit.nva.model.contexttypes.Chapter;
import no.unit.nva.model.contexttypes.Degree;
import no.unit.nva.model.contexttypes.Event;
import no.unit.nva.model.contexttypes.GeographicalContent;
import no.unit.nva.model.contexttypes.Journal;
import no.unit.nva.model.contexttypes.PublicationContext;
import no.unit.nva.model.contexttypes.Publisher;
import no.unit.nva.model.contexttypes.Report;
import no.unit.nva.model.contexttypes.ResearchData;
import no.unit.nva.model.contexttypes.Series;
import no.unit.nva.model.contexttypes.UnconfirmedJournal;
import no.unit.nva.model.exceptions.InvalidIsbnException;
import no.unit.nva.model.exceptions.InvalidIssnException;
import no.unit.nva.model.exceptions.InvalidUnconfirmedSeriesException;
import no.unit.nva.model.instancetypes.Map;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.book.BookMonograph;
import no.unit.nva.model.instancetypes.book.BookMonographContentType;
import no.unit.nva.model.instancetypes.chapter.ChapterInReport;
import no.unit.nva.model.instancetypes.degree.DegreeBachelor;
import no.unit.nva.model.instancetypes.degree.DegreeMaster;
import no.unit.nva.model.instancetypes.degree.DegreePhd;
import no.unit.nva.model.instancetypes.degree.OtherStudentWork;
import no.unit.nva.model.instancetypes.event.Lecture;
import no.unit.nva.model.instancetypes.journal.FeatureArticle;
import no.unit.nva.model.instancetypes.journal.JournalArticle;
import no.unit.nva.model.instancetypes.journal.JournalArticleContentType;
import no.unit.nva.model.instancetypes.report.ReportBasic;
import no.unit.nva.model.instancetypes.report.ReportResearch;
import no.unit.nva.model.instancetypes.report.ReportWorkingPaper;
import no.unit.nva.model.instancetypes.researchdata.DataSet;
import no.unit.nva.model.instancetypes.researchdata.GeographicalDescription;
import no.unit.nva.model.pages.MonographPages;
import no.unit.nva.model.pages.Pages;
import no.unit.nva.model.pages.Range;
import nva.commons.core.paths.UriWrapper;
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
                           .build();
            }
            if (NvaType.MAP.getValue().equals(builder.getType().getNva())) {
                return new Reference.Builder().withPublicationInstance(
                        new Map(builder.getDescriptionsForPublication(), builder.getMonographPages()))
                           .withPublishingContext(new GeographicalContent(generatePublisher(builder)))
                           .build();
            }
            if (NvaType.DATASET.getValue().equals(builder.getType().getNva())) {
                return new Reference.Builder()
                           .withPublicationInstance(getPublicationInstanceForDataset(builder))
                           .withPublishingContext(new ResearchData(generatePublisher(builder)))
                           .build();
            }
            if (NvaType.REPORT.getValue().equals(builder.getType().getNva())) {
                return new Reference.Builder()
                           .withPublicationInstance(generatePublicationInstanceForReport(builder))
                           .withPublishingContext(generatePublicationContextForReport(builder))
                           .build();
            }
            if (NvaType.RESEARCH_REPORT.getValue().equals(builder.getType().getNva())) {
                return new Reference.Builder()
                           .withPublicationInstance(generatePublicationInstanceForResearchReport(builder))
                           .withPublishingContext(generatePublicationContextForReport(builder))
                           .build();
            }
            if (NvaType.WORKING_PAPER.getValue().equals(builder.getType().getNva())) {
                return new Reference.Builder()
                           .withPublicationInstance(generatePublicationInstanceForReportWorkingPaper(builder))
                           .withPublishingContext(generatePublicationContextForReport(builder))
                           .build();
            }
            if (NvaType.BACHELOR_THESIS.getValue().equals(builder.getType().getNva())) {
                return new Reference.Builder()
                           .withPublishingContext(generatePublicationContextForDegree(builder))
                           .withPublicationInstance(generatePublicationInstanceForBachelorDegree(builder))
                           .build();
            }
            if (NvaType.MASTER_THESIS.getValue().equals(builder.getType().getNva())) {
                return new Reference.Builder()
                           .withPublishingContext(generatePublicationContextForDegree(builder))
                           .withPublicationInstance(generatePublicationInstanceForMasterDegree(builder))
                           .build();
            }
            if (NvaType.DOCTORAL_THESIS.getValue().equals(builder.getType().getNva())) {
                return new Reference.Builder()
                           .withPublishingContext(generatePublicationContextForDegree(builder))
                           .withPublicationInstance(generatePublicationInstanceForPhd(builder))
                           .build();
            }
            if (NvaType.JOURNAL_ARTICLE.getValue().equals(builder.getType().getNva()) && hasJournalId(builder)) {
                return new Reference.Builder()
                           .withPublishingContext(generateJournal(builder))
                           .withPublicationInstance(generatePublicationInstanceForJournalArticle(builder))
                           .build();
            }
            if (NvaType.CHRONICLE.getValue().equals(builder.getType().getNva()) && hasJournalId(builder)) {
                return new Reference.Builder()
                           .withPublishingContext(generateJournal(builder))
                           .withPublicationInstance(generatePublicationInstanceForFeatureArticle(builder))
                           .build();
            }
            if (NvaType.JOURNAL_ARTICLE.getValue().equals(builder.getType().getNva()) && !hasJournalId(builder)) {
                return new Reference.Builder()
                           .withPublishingContext(generateUnconfirmedJournal(builder))
                           .withPublicationInstance(generatePublicationInstanceForJournalArticle(builder))
                           .build();
            }
            if (NvaType.SCIENTIFIC_ARTICLE.getValue().equals(builder.getType().getNva()) && hasJournalId(builder)) {
                return new Reference.Builder()
                           .withPublishingContext(generateJournal(builder))
                           .withPublicationInstance(generatePublicationInstanceForScientificArticle(builder))
                           .build();
            }
            if (NvaType.STUDENT_PAPER_OTHERS.getValue().equals(builder.getType().getNva())
                || NvaType.STUDENT_PAPER.getValue().equals(builder.getType().getNva())) {
                return new Reference.Builder()
                           .withPublishingContext(generatePublicationContextForOtherStudentWork(builder))
                           .withPublicationInstance(
                               new OtherStudentWork.Builder().withPages(builder.getMonographPages())
                                   .withSubmittedDate(builder.getPublicationDateForPublication())
                                   .build()).build();
            }
            if (NvaType.SCIENTIFIC_MONOGRAPH.getValue().equals(builder.getType().getNva())) {
                return new Reference.Builder()
                           .withPublishingContext(generatePublicationContextForBook(builder))
                           .withPublicationInstance(generatePublicationInstanceForScientificMonograph(builder))
                           .build();
            }
            if (NvaType.LECTURE.getValue().equals(builder.getType().getNva())) {
                return new Reference.Builder()
                           .withPublishingContext(new Event.Builder().build())
                           .withPublicationInstance(new Lecture())
                           .build();
            }
            if (NvaType.CHAPTER.getValue().equals(builder.getType().getNva())) {
                return new Reference.Builder()
                           .withPublishingContext(new Chapter())
                           .withPublicationInstance(new ChapterInReport.Builder()
                                                        .withPages(generateRange(builder))
                                                        .build())
                           .build();
            }
            return new Reference.Builder().build();
        } catch (Exception e) {
            return new Reference.Builder().build();
        }
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
        return new ReportWorkingPaper.Builder().withPages(builder.getMonographPages()).build();
    }

    private static PublicationInstance<? extends Pages> generatePublicationInstanceForScientificArticle(
        Builder builder) {
        return new JournalArticle.Builder().withContent(JournalArticleContentType.ACADEMIC_ARTICLE)
                   .withPeerReviewed(true)
                   .withPages(generateRange(builder)).build();
    }

    private static PublicationContext generateUnconfirmedJournal(Builder builder) throws InvalidIssnException {
        return new UnconfirmedJournal(builder.getPublication().getJournal(),
                                      builder.getPublication().getIssn(),
                                      builder.getPublication().getIssn());
    }

    private static boolean hasJournalId(Builder builder) {
        return nonNull(builder.getPublication().getPublicationContext().getJournal().getId());
    }

    private static PublicationContext generatePublicationContextForReport(Builder builder)
        throws InvalidIssnException, InvalidIsbnException, InvalidUnconfirmedSeriesException {
        return new Report.Builder().withIsbnList(Collections.singletonList(builder.getIsbn()))
                   .withSeries(generateSeries(builder))
                   .build();
    }

    private static BookMonograph generatePublicationInstanceForScientificMonograph(Builder builder) {
        return new BookMonograph.Builder().withContentType(BookMonographContentType.ACADEMIC_MONOGRAPH)
                   .withPeerReviewed(true)
                   .withOriginalResearch(false)
                   .withPages(builder.getMonographPages())
                   .build();
    }

    private static PublicationContext generatePublicationContextForOtherStudentWork(Builder builder)
        throws InvalidIsbnException {
        return new Book.BookBuilder().withIsbnList(Collections.singletonList(builder.getIsbn())).build();
    }

    private static JournalArticle generatePublicationInstanceForJournalArticle(Builder builder) {
        return new JournalArticle.Builder().withContent(JournalArticleContentType.PROFESSIONAL_ARTICLE)
                   .withPages(generateRange(builder)).build();
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

    private static Series generateSeries(Builder builder) {
        if (nonNull(builder.getSeriesId())) {
            return new Series(UriWrapper.fromUri(PublicationContextMapper.CHANNEL_REGISTRY)
                                  .addChild(ChannelType.SERIES.getType())
                                  .addChild(builder.getSeriesId())
                                  .addChild(nonNull(getYear(builder)) ? getYear(builder) : CURRENT_YEAR)
                                  .getUri());
        }
        return null;
    }

    private static Journal generateJournal(Builder builder) {
        return new Journal(UriWrapper.fromUri(PublicationContextMapper.CHANNEL_REGISTRY)
                               .addChild(ChannelType.JOURNAL.getType())
                               .addChild(builder.getJournalId())
                               .addChild(nonNull(getYear(builder)) ? getYear(builder) : CURRENT_YEAR)
                               .getUri());
    }

    private static DataSet getPublicationInstanceForDataset(Builder builder) {
        return new DataSet(false, new GeographicalDescription(String.join(", ", builder.getSpatialCoverage())),
                           null, null, null);
    }

    private static DegreeBachelor generatePublicationInstanceForBachelorDegree(Builder builder) {
        return new DegreeBachelor.Builder().withSubmittedDate(
                builder.getPublicationDateForPublication())
                   .withPages(builder.getMonographPages()).build();
    }

    private static DegreeMaster generatePublicationInstanceForMasterDegree(Builder builder) {
        return new DegreeMaster.Builder().withSubmittedDate(
                builder.getPublicationDateForPublication())
                   .withPages(builder.getMonographPages()).build();
    }

    private static DegreePhd generatePublicationInstanceForPhd(Builder builder) {
        return new DegreePhd.Builder().withSubmittedDate(
                builder.getPublicationDateForPublication())
                   .withPages(builder.getMonographPages()).build();
    }

    private static ReportBasic generatePublicationInstanceForReport(Builder builder) {
        return new ReportBasic.Builder()
                   .withPages(
                       new MonographPages.Builder()
                           .withPages(builder.getPages().getPages())
                           .withIllustrated(false).build())
                   .build();
    }

    private static ReportResearch generatePublicationInstanceForResearchReport(Builder builder) {
        return new ReportResearch.Builder()
                   .withPages(new MonographPages.Builder()
                                  .withPages(builder.getPages().getPages())
                                  .withIllustrated(false).build())
                   .build();
    }

    private static Book generatePublicationContextForBook(Builder builder) throws InvalidIsbnException {
        return new Book.BookBuilder()
                   .withSeriesNumber(builder.getSeriesNumberPublication())
                   .withIsbnList(Collections.singletonList(builder.getIsbn()))
                   .build();
    }

    private static BookMonograph generatePublicationInstanceForBook(Builder builder) {
        return new BookMonograph.Builder()
                   .withPeerReviewed(false)
                   .withOriginalResearch(false)
                   .withContentType(BookMonographContentType.NON_FICTION_MONOGRAPH)
                   .withPages(new MonographPages.Builder().withIllustrated(false)
                                  .withPages(builder.getPages().getPages())
                                  .build())
                   .build();
    }

    private static Degree generatePublicationContextForDegree(Builder builder)
        throws InvalidIsbnException, InvalidUnconfirmedSeriesException {
        return new Degree.Builder().withIsbnList(Collections.singletonList(builder.getIsbn())).build();
    }
}
