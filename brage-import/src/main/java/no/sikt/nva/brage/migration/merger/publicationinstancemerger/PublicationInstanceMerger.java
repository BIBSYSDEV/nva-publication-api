package no.sikt.nva.brage.migration.merger.publicationinstancemerger;

import static java.util.Objects.nonNull;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Supplier;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.instancetypes.degree.RelatedDocument;
import no.unit.nva.model.pages.MonographPages;
import no.unit.nva.model.pages.Range;
import nva.commons.core.JacocoGenerated;

public class PublicationInstanceMerger {

    @JacocoGenerated
    PublicationInstanceMerger() {
    }

    public static MonographPages getPages(MonographPages pages, MonographPages bragePages) {
        return nonNull(pages) ? pages : bragePages;
    }

    public static PublicationDate getDate(PublicationDate submittedDate, PublicationDate brageDate) {
        return nonNull(submittedDate) ? submittedDate : brageDate;
    }

    public static Range getRange(Range oldRange, Range newRange) {
        return nonNull(oldRange) ? oldRange : newRange;
    }

    public static String getNonNullValue(String oldValue, String newValue) {
        return nonNull(oldValue) ? oldValue : newValue;
    }

    public static <T, C extends Collection<T>> C mergeCollections(C oldCollection, C newCollection, Supplier<C> collectionFactory) {
        var result = collectionFactory.get();
        if (nonNull(oldCollection)) {
            result.addAll(oldCollection);
        }
        if (nonNull(newCollection)) {
            result.addAll(newCollection);
        }
        return result;
    }
}
