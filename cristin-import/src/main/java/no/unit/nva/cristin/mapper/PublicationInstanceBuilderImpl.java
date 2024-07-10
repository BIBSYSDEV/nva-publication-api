package no.unit.nva.cristin.mapper;

import static no.unit.nva.cristin.mapper.CristinMainCategory.isArt;
import static no.unit.nva.cristin.mapper.CristinMainCategory.isBook;
import static no.unit.nva.cristin.mapper.CristinMainCategory.isChapter;
import static no.unit.nva.cristin.mapper.CristinMainCategory.isEvent;
import static no.unit.nva.cristin.mapper.CristinMainCategory.isExhibition;
import static no.unit.nva.cristin.mapper.CristinMainCategory.isJournal;
import static no.unit.nva.cristin.mapper.CristinMainCategory.isMediaContribution;
import static no.unit.nva.cristin.mapper.CristinMainCategory.isReport;
import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.isBriefs;
import static no.unit.nva.cristin.mapper.nva.ReferenceBuilder.NIFU_CUSTOMER_NAME;
import java.util.Objects;
import no.unit.nva.cristin.mapper.nva.exceptions.UnsupportedMainCategoryException;
import no.unit.nva.cristin.mapper.nva.exceptions.UnsupportedSecondaryCategoryException;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.report.ReportPolicy;
import no.unit.nva.model.pages.MonographPages;
import no.unit.nva.model.pages.Pages;
import software.amazon.awssdk.services.s3.S3Client;

public class PublicationInstanceBuilderImpl {

    public static final String ERROR_CRISTIN_OBJECT_IS_NULL = "CristinObject can not be null";

    public static final String ERROR_PARSING_MAIN_OR_SECONDARY_CATEGORIES = "Error parsing main or secondary "
                                                                            + "categories";

    private final CristinObject cristinObject;
    private final S3Client s3Client;

    public PublicationInstanceBuilderImpl(CristinObject cristinObject, S3Client s3Client) {
        Objects.requireNonNull(cristinObject, ERROR_CRISTIN_OBJECT_IS_NULL);
        this.cristinObject = cristinObject;
        this.s3Client = s3Client;
    }

    public PublicationInstance<? extends Pages> build() {
        if (isBook(cristinObject)) {
            return new BookBuilder(cristinObject).build();
        } else if (isReport(cristinObject)) {
            return new ReportBuilder(cristinObject).build();
        } else if (isJournal(cristinObject)) {
            return new JournalBuilder(cristinObject, s3Client).build();
        } else if (isChapter(cristinObject)) {
            return new ChapterArticleBuilder(cristinObject).build();
        } else if (isEvent(cristinObject)) {
            return new EventBuilder(cristinObject).build();
        } else if (isMediaContribution(cristinObject)) {
            return new MediaContributionBuilder(cristinObject).build();
        } else if (isArt(cristinObject)) {
            return new ArtBuilder(cristinObject, s3Client).build();
        } else if (isExhibition(cristinObject)) {
            return new ExhibitionProductionBuilder(cristinObject).build();
        } else if (isBriefsThatShouldBeMapped(cristinObject)) {
            return new ReportPolicy(new MonographPages());
        } else if (cristinObject.getMainCategory().isUnknownCategory()) {
            throw new UnsupportedMainCategoryException();
        } else if (cristinObject.getSecondaryCategory().isUnknownCategory()) {
            throw new UnsupportedSecondaryCategoryException();
        }
        throw new RuntimeException(ERROR_PARSING_MAIN_OR_SECONDARY_CATEGORIES);
    }

    private boolean isBriefsThatShouldBeMapped(CristinObject cristinObject) {
        return isBriefs(cristinObject) && NIFU_CUSTOMER_NAME.equals(cristinObject.getOwnerCodeCreated());
    }
}
