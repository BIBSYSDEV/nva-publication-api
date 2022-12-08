package no.sikt.nva.brage.migration.testutils;

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
import no.unit.nva.model.contexttypes.GeographicalContent;
import no.unit.nva.model.contexttypes.Journal;
import no.unit.nva.model.contexttypes.Publisher;
import no.unit.nva.model.contexttypes.Report;
import no.unit.nva.model.contexttypes.ResearchData;
import no.unit.nva.model.contexttypes.Series;
import no.unit.nva.model.exceptions.InvalidIsbnException;
import no.unit.nva.model.instancetypes.Map;
import no.unit.nva.model.instancetypes.book.BookMonograph;
import no.unit.nva.model.instancetypes.book.BookMonographContentType;
import no.unit.nva.model.instancetypes.degree.DegreeBachelor;
import no.unit.nva.model.instancetypes.degree.DegreeMaster;
import no.unit.nva.model.instancetypes.degree.DegreePhd;
import no.unit.nva.model.instancetypes.journal.JournalArticle;
import no.unit.nva.model.instancetypes.report.ReportBasic;
import no.unit.nva.model.instancetypes.report.ReportResearch;
import no.unit.nva.model.instancetypes.researchdata.DataSet;
import no.unit.nva.model.instancetypes.researchdata.GeographicalDescription;
import no.unit.nva.model.pages.MonographPages;
import no.unit.nva.model.pages.Range;
import nva.commons.core.paths.UriWrapper;
import org.jetbrains.annotations.NotNull;

public final class ReferenceGenerator {

    public static Reference generateReference(Builder builder) {
        return Optional.ofNullable(builder)
                   .map(ReferenceGenerator::buildReference)
                   .orElse(new Reference());
    }

    private static Reference buildReference(Builder builder) {
        try {
            if (NvaType.CHAPTER.getValue().equals(builder.getType().getNva())) {
                return new Reference.Builder()
                           .withPublishingContext(new Chapter.Builder().build())
                           .build();
            }
            if (NvaType.BOOK.getValue().equals(builder.getType().getNva())) {
                return new Reference.Builder()
                           .withPublishingContext(generatePublicationContextForBook(builder))
                           .withPublicationInstance(generatePublicationInstanceForBook())
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
                           .withPublishingContext(new Report.Builder().withSeries(generateSeries(builder)).build())
                           .build();
            }
            if (NvaType.RESEARCH_REPORT.getValue().equals(builder.getType().getNva())) {
                return new Reference.Builder()
                           .withPublicationInstance(generatePublicationInstanceForResearchReport(builder))
                           .withPublishingContext(new Report.Builder().build())
                           .build();
            }
            if (NvaType.BACHELOR_THESIS.getValue().equals(builder.getType().getNva())) {
                return new Reference.Builder()
                           .withPublishingContext(new Degree.Builder().build())
                           .withPublicationInstance(generatePublicationInstanceForBachelorDegree(builder))
                           .build();
            }
            if (NvaType.MASTER_THESIS.getValue().equals(builder.getType().getNva())) {
                return new Reference.Builder()
                           .withPublishingContext(new Degree.Builder().build())
                           .withPublicationInstance(generatePublicationInstanceForMasterDegree(builder))
                           .build();
            }
            if (NvaType.DOCTORAL_THESIS.getValue().equals(builder.getType().getNva())) {
                return new Reference.Builder()
                           .withPublishingContext(new Degree.Builder().build())
                           .withPublicationInstance(generatePublicationInstanceForPhd(builder))
                           .build();
            }
            if (NvaType.JOURNAL_ARTICLE.getValue().equals(builder.getType().getNva())) {
                return new Reference.Builder()
                           .withPublishingContext(generatePublishingContextForJournal(builder))
                           .withPublicationInstance(generatePublicationInstanceForJournalArticle())
                           .build();
            }
            return new Reference.Builder().build();
        } catch (Exception e) {
            return new Reference.Builder().build();
        }
    }

    @NotNull
    private static JournalArticle generatePublicationInstanceForJournalArticle() {
        return new JournalArticle.Builder().withPages(new Range.Builder().build()).build();
    }

    private static Publisher generatePublisher(Builder builder) {
        return new Publisher(UriWrapper.fromUri(PublicationContextMapper.BASE_URL)
                                 .addChild(ChannelType.PUBLISHER.getType())
                                 .addChild(builder.getPublisherId())
                                 .getUri());
    }

    private static Series generateSeries(Builder builder) {
        return new Series(UriWrapper.fromUri(PublicationContextMapper.BASE_URL)
                              .addChild(ChannelType.SERIES.getType())
                              .addChild(builder.getSeriesId())
                              .getUri());
    }

    private static Journal generatePublishingContextForJournal(Builder builder) {
        return new Journal(UriWrapper.fromUri(PublicationContextMapper.BASE_URL)
                               .addChild(ChannelType.JOURNAL.getType())
                               .addChild(builder.getJournalId())
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

    private static BookMonograph generatePublicationInstanceForBook() {
        return new BookMonograph.Builder()
                   .withPeerReviewed(false)
                   .withOriginalResearch(false)
                   .withContentType(BookMonographContentType.NON_FICTION_MONOGRAPH)
                   .withPages(new MonographPages.Builder().withIllustrated(false).build())
                   .build();
    }
}
