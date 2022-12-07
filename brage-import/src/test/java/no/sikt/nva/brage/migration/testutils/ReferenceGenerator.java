package no.sikt.nva.brage.migration.testutils;

import java.util.Optional;
import no.sikt.nva.brage.migration.testutils.NvaBrageMigrationDataGenerator.Builder;
import no.sikt.nva.brage.migration.testutils.type.NvaType;
import no.unit.nva.model.Reference;
import no.unit.nva.model.contexttypes.Book;
import no.unit.nva.model.contexttypes.Chapter;
import no.unit.nva.model.contexttypes.Degree;
import no.unit.nva.model.contexttypes.Report;
import no.unit.nva.model.exceptions.InvalidIsbnException;
import no.unit.nva.model.instancetypes.Map;
import no.unit.nva.model.instancetypes.book.BookMonograph;
import no.unit.nva.model.instancetypes.book.BookMonographContentType;
import no.unit.nva.model.instancetypes.degree.DegreeBachelor;
import no.unit.nva.model.instancetypes.report.ReportBasic;
import no.unit.nva.model.instancetypes.report.ReportResearch;
import no.unit.nva.model.instancetypes.researchdata.DataSet;
import no.unit.nva.model.instancetypes.researchdata.GeographicalDescription;
import no.unit.nva.model.pages.MonographPages;
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
                    new Map(builder.getDescriptionsForPublication(), builder.getMonographPages())).build();
            }
            if (NvaType.DATASET.getValue().equals(builder.getType().getNva())) {
                return new Reference.Builder().withPublicationInstance(
                    new DataSet(false, new GeographicalDescription(String.join(", ", builder.getSpatialCoverage())),
                                null, null, null)).build();
            }
            if (NvaType.REPORT.getValue().equals(builder.getType().getNva())) {
                return new Reference.Builder()
                           .withPublicationInstance(generatePublicationInstanceForReport(builder))
                           .withPublishingContext(new Report.Builder().build())
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
                           .withPublicationInstance(generatePulicationInstanceForBachelorDegree(builder))
                           .build();
            }
            return new Reference.Builder().build();
        } catch (Exception e) {
            return new Reference.Builder().build();
        }
    }

    @NotNull
    private static DegreeBachelor generatePulicationInstanceForBachelorDegree(Builder builder) {
        return new DegreeBachelor.Builder().withSubmittedDate(
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
        return new Book.BookBuilder().withSeriesNumber(builder.getSeriesNumberPublication()).build();
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
