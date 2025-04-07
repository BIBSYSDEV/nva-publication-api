package no.unit.nva.cristin.patcher;

import static java.util.Objects.isNull;
import java.util.List;
import java.util.Map;
import no.unit.nva.model.Publication;
import no.unit.nva.model.instancetypes.book.AcademicMonograph;
import no.unit.nva.model.instancetypes.book.BookAnthology;
import no.unit.nva.model.instancetypes.book.Encyclopedia;
import no.unit.nva.model.instancetypes.book.ExhibitionCatalog;
import no.unit.nva.model.instancetypes.book.NonFictionMonograph;
import no.unit.nva.model.instancetypes.book.PopularScienceMonograph;
import no.unit.nva.model.instancetypes.book.Textbook;
import no.unit.nva.model.instancetypes.chapter.AcademicChapter;
import no.unit.nva.model.instancetypes.chapter.ChapterInReport;
import no.unit.nva.model.instancetypes.chapter.EncyclopediaChapter;
import no.unit.nva.model.instancetypes.chapter.Introduction;
import no.unit.nva.model.instancetypes.chapter.NonFictionChapter;
import no.unit.nva.model.instancetypes.chapter.PopularScienceChapter;
import no.unit.nva.model.instancetypes.chapter.TextbookChapter;
import no.unit.nva.model.instancetypes.report.ConferenceReport;
import no.unit.nva.model.instancetypes.report.ReportPolicy;
import no.unit.nva.model.instancetypes.report.ReportResearch;
import nva.commons.core.JacocoGenerated;

public final class ChildParentInstanceComparator {

    private static final Map<String, List<String>> VALID_PUBLICATION_INSTANCE = Map.of(
        NonFictionChapter.class.getSimpleName(),
        List.of(BookAnthology.class.getSimpleName(), Textbook.class.getSimpleName(),
                NonFictionMonograph.class.getSimpleName(), PopularScienceMonograph.class.getSimpleName(),
                Encyclopedia.class.getSimpleName(), ExhibitionCatalog.class.getSimpleName(),
                AcademicMonograph.class.getSimpleName()),

        AcademicChapter.class.getSimpleName(),
        List.of(BookAnthology.class.getSimpleName(), NonFictionMonograph.class.getSimpleName(),
                Textbook.class.getSimpleName(), PopularScienceMonograph.class.getSimpleName(),
                AcademicMonograph.class.getSimpleName(), Encyclopedia.class.getSimpleName(),
                ExhibitionCatalog.class.getSimpleName(),ReportResearch.class.getSimpleName()),

        Introduction.class.getSimpleName(),
        List.of(BookAnthology.class.getSimpleName(), NonFictionMonograph.class.getSimpleName(),
                Introduction.class.getSimpleName(), Textbook.class.getSimpleName(),
                PopularScienceMonograph.class.getSimpleName(), AcademicMonograph.class.getSimpleName(),
                ExhibitionCatalog.class.getSimpleName(), Encyclopedia.class.getSimpleName()),

        PopularScienceChapter.class.getSimpleName(),
        List.of(BookAnthology.class.getSimpleName(), PopularScienceMonograph.class.getSimpleName(),
                NonFictionMonograph.class.getSimpleName(), Textbook.class.getSimpleName(),
                ExhibitionCatalog.class.getSimpleName(), Encyclopedia.class.getSimpleName(),
                AcademicMonograph.class.getSimpleName()),

        TextbookChapter.class.getSimpleName(), List.of(Textbook.class.getSimpleName()),

        EncyclopediaChapter.class.getSimpleName(),
        List.of(Encyclopedia.class.getSimpleName(), BookAnthology.class.getSimpleName(),
                NonFictionMonograph.class.getSimpleName(), AcademicMonograph.class.getSimpleName(),
                Textbook.class.getSimpleName(), PopularScienceMonograph.class.getSimpleName(),
                ExhibitionCatalog.class.getSimpleName()),

        ChapterInReport.class.getSimpleName(),
        List.of(ReportResearch.class.getSimpleName(), ReportPolicy.class.getSimpleName(),
                ConferenceReport.class.getSimpleName(), ConferenceReport.class.getSimpleName()));

    @JacocoGenerated
    private ChildParentInstanceComparator() {

    }

    public static boolean isValidCombination(Publication child, Publication parent) {
        var validParents = VALID_PUBLICATION_INSTANCE.get(getPublicationsInstanceName(child));
        if (isNull(validParents)) {
            return false;
        }
        return validParents.contains(getPublicationsInstanceName(parent));
    }

    public static String getPublicationsInstanceName(Publication publication) {
        return publication.getEntityDescription().getReference().getPublicationInstance().getClass().getSimpleName();
    }
}
