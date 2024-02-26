package no.unit.nva.cristin.patcher;

import static java.util.Objects.nonNull;
import java.util.List;
import java.util.Map;
import no.unit.nva.model.Publication;
import no.unit.nva.model.instancetypes.book.NonFictionMonograph;
import no.unit.nva.model.instancetypes.chapter.Introduction;
import no.unit.nva.model.instancetypes.degree.DegreePhd;
import nva.commons.core.JacocoGenerated;

public final class ParentChildInstanceComparator {

    private static final Map<String, List<String>> VALID_PUBLICATION_INSTANCE =
        Map.of(NonFictionMonograph.class.getSimpleName(), List.of(DegreePhd.class.getSimpleName()),
               Introduction.class.getSimpleName(), List.of(DegreePhd.class.getSimpleName()));


    @JacocoGenerated
    private ParentChildInstanceComparator() {

    }

    public static boolean isValidCombination(Publication child, Publication parent) {
        var validChildren = VALID_PUBLICATION_INSTANCE.get(getPublicationsInstanceName(child));
        return nonNull(validChildren) && validChildren.contains(getPublicationsInstanceName(parent));
    }

    public static String getPublicationsInstanceName(Publication publication) {
        return publication
                   .getEntityDescription()
                   .getReference()
                   .getPublicationInstance()
                   .getClass()
                   .getSimpleName();
    }

}
