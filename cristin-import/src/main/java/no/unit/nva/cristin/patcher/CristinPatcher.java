package no.unit.nva.cristin.patcher;

import static no.unit.nva.cristin.lambda.constants.MappingConstants.NVA_API_DOMAIN;
import static no.unit.nva.publication.PublicationServiceConfig.PUBLICATION_PATH;
import java.net.URI;
import no.unit.nva.cristin.patcher.exception.ChildPatchPublicationInstanceMismatchException;
import no.unit.nva.cristin.patcher.exception.ParentPatchPublicationInstanceMismatchException;
import no.unit.nva.cristin.patcher.model.ParentAndChild;
import no.unit.nva.model.Publication;
import no.unit.nva.model.contexttypes.Anthology;
import no.unit.nva.model.instancetypes.degree.ConfirmedDocument;
import no.unit.nva.model.instancetypes.degree.DegreePhd;
import nva.commons.core.paths.UriWrapper;

public final class CristinPatcher {

    private static final String CHILD_PARENT_REFERENCE_MISMATCH =
        "Invalid child / parent reference mismatch, child publicationContext: %s, parent publicationInstance: %s";

    private static final String PARENT_CHILD_REFERENCE_MISMATCH =
        "Invalid parent / child reference mismatch, child publicationContext: %s, parent publicationInstance: %s";

    private CristinPatcher() {

    }

    public static ParentAndChild updateChildPublication(ParentAndChild parentAndChild) {
        var child = parentAndChild.getChildPublication();
        var parent = parentAndChild.getParentPublication();
        if (ChildParentInstanceComparator.isValidCombination(child, parent)) {
            var anthology = (Anthology) child.getEntityDescription().getReference().getPublicationContext();
            anthology.setId(createPublicationId(parentAndChild.getParentPublication()));
        } else {
            throw new ChildPatchPublicationInstanceMismatchException(
                String.format(CHILD_PARENT_REFERENCE_MISMATCH,
                              ChildParentInstanceComparator.getPublicationsInstanceName(child),
                              ChildParentInstanceComparator.getPublicationsInstanceName(parent)));
        }
        return parentAndChild;
    }

    private static boolean isDegreePhd(Publication publication) {
        return publication.getEntityDescription()
                   .getReference()
                   .getPublicationInstance() instanceof DegreePhd;
    }

    public static ParentAndChild updateParentPublication(ParentAndChild parentAndChild) {
        return isDegreePhd(parentAndChild.getParentPublication())
                   ? attemptToUpdateParentPublication(parentAndChild)
                   : parentAndChild;
    }

    private static ParentAndChild attemptToUpdateParentPublication(ParentAndChild parentAndChild) {
        var child = parentAndChild.getChildPublication();
        var parent = parentAndChild.getParentPublication();
        if (ParentChildInstanceComparator.isValidCombination(child, parent)) {
            var degreePhd = (DegreePhd) parent.getEntityDescription().getReference().getPublicationInstance();
            degreePhd.getRelated().add(new ConfirmedDocument(createPublicationId(child)));
            return parentAndChild;
        } else {
            throw new ParentPatchPublicationInstanceMismatchException(
                String.format(PARENT_CHILD_REFERENCE_MISMATCH,
                              ChildParentInstanceComparator.getPublicationsInstanceName(parent),
                              ChildParentInstanceComparator.getPublicationsInstanceName(child)));
        }
    }

    private static URI createPublicationId(Publication publication) {
        return UriWrapper.fromUri(NVA_API_DOMAIN + PUBLICATION_PATH + "/" + publication.getIdentifier()).getUri();
    }
}
