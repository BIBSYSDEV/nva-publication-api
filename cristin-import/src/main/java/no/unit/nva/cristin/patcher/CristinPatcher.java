package no.unit.nva.cristin.patcher;

import static no.unit.nva.cristin.lambda.constants.MappingConstants.NVA_API_DOMAIN;
import static no.unit.nva.publication.PublicationServiceConfig.PUBLICATION_PATH;
import java.net.URI;
import no.unit.nva.cristin.patcher.exception.PublicationInstanceMismatchException;
import no.unit.nva.cristin.patcher.model.ParentAndChild;
import no.unit.nva.model.Publication;
import no.unit.nva.model.contexttypes.Anthology;
import no.unit.nva.model.contexttypes.PublicationContext;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.book.BookAnthology;
import nva.commons.core.paths.UriWrapper;

public final class CristinPatcher {

    private static final String CHILD_PARENT_REFERENCE_MISMATCH =
        "Invalid child / parent reference mismatch, child publicationContext: %s, parent publicationInstance: %s";

    private CristinPatcher() {

    }

    public static ParentAndChild updateChildPublication(ParentAndChild parentAndChild) {
        var childContext =
            parentAndChild.getChildPublication().getEntityDescription().getReference().getPublicationContext();
        var parentPublicationInstance =
            parentAndChild.getParentPublication().getEntityDescription().getReference().getPublicationInstance();
        if (parentChildPublicationContextMatch(childContext, parentPublicationInstance)) {
            var anthology = (Anthology) childContext;
            anthology.setId(createPartOfUri(parentAndChild.getParentPublication()));
        } else {
            throw new PublicationInstanceMismatchException(
                String.format(CHILD_PARENT_REFERENCE_MISMATCH,
                              childContext.getClass().getSimpleName(),
                              parentPublicationInstance.getClass().getSimpleName()));
        }
        return parentAndChild;
    }

    private static boolean parentChildPublicationContextMatch(PublicationContext childContext,
                                                              PublicationInstance parentPublicationInstance) {
        return childContext instanceof Anthology && parentPublicationInstance instanceof BookAnthology;
    }

    private static URI createPartOfUri(Publication parentPublication) {
        return UriWrapper.fromUri(NVA_API_DOMAIN + PUBLICATION_PATH + "/" + parentPublication.getIdentifier()).getUri();
    }
}
