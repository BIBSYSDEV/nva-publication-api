package no.unit.nva.cristin.patcher;

import static no.unit.nva.cristin.lambda.constants.MappingConstants.NVA_API_DOMAIN;
import static no.unit.nva.publication.PublicationServiceConfig.PUBLICATION_PATH;
import java.net.URI;
import no.unit.nva.cristin.patcher.exception.PublicationInstanceMismatchException;
import no.unit.nva.cristin.patcher.model.ParentAndChild;
import no.unit.nva.model.Publication;
import no.unit.nva.model.contexttypes.Anthology;
import nva.commons.core.paths.UriWrapper;

public final class CristinPatcher {

    private static final String CHILD_PARENT_REFERENCE_MISMATCH =
        "Invalid child / parent reference mismatch, child publicationContext: %s, parent publicationInstance: %s";

    private CristinPatcher() {

    }

    public static ParentAndChild updateChildPublication(ParentAndChild parentAndChild) {
        var child = parentAndChild.getChildPublication();
        var parent = parentAndChild.getParentPublication();
        if (ChildParentInstanceComparator.isValidCombination(child, parent)) {
            var anthology = (Anthology) child.getEntityDescription().getReference().getPublicationContext();
            anthology.setId(createPartOfUri(parentAndChild.getParentPublication()));
        } else {
            throw new PublicationInstanceMismatchException(
                String.format(CHILD_PARENT_REFERENCE_MISMATCH,
                              ChildParentInstanceComparator.getPublicationsInstanceName(child),
                              ChildParentInstanceComparator.getPublicationsInstanceName(parent)));
        }
        return parentAndChild;
    }

    private static URI createPartOfUri(Publication parentPublication) {
        return UriWrapper.fromHost(NVA_API_DOMAIN)
                   .addChild(PUBLICATION_PATH)
                   .addChild(parentPublication.getIdentifier().toString())
                   .getUri();
    }
}
