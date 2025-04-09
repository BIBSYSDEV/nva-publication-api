package no.unit.nva.cristin.patcher;

import static no.unit.nva.cristin.lambda.constants.MappingConstants.NVA_API_DOMAIN;
import static no.unit.nva.cristin.patcher.ChildParentInstanceComparator.getPublicationsInstanceName;
import static no.unit.nva.publication.PublicationServiceConfig.PUBLICATION_PATH;
import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import no.unit.nva.cristin.patcher.exception.ChildNotAnthologyException;
import no.unit.nva.cristin.patcher.exception.ChildPatchPublicationInstanceMismatchException;
import no.unit.nva.cristin.patcher.exception.ParentPatchPublicationInstanceMismatchException;
import no.unit.nva.cristin.patcher.model.ParentAndChild;
import no.unit.nva.model.Publication;
import no.unit.nva.model.additionalidentifiers.CristinIdentifier;
import no.unit.nva.model.additionalidentifiers.SourceName;
import no.unit.nva.model.contexttypes.Anthology;
import no.unit.nva.model.instancetypes.degree.ConfirmedDocument;
import no.unit.nva.model.instancetypes.degree.DegreePhd;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.paths.UnixPath;
import nva.commons.core.paths.UriWrapper;

public final class CristinPatcher {

    private static final String PARENT_CHILD_REFERENCE_MISMATCH =
        "Invalid parent / child reference mismatch, child publicationContext: %s, parent publicationInstance: %s";
    private static final String PATCH_ERROR_REPORT = "PATCH_ERROR_REPORT";
    private final S3Driver s3Driver;

    public CristinPatcher(S3Driver s3Driver) {
        this.s3Driver = s3Driver;
    }

    public ParentAndChild updateChildPublication(ParentAndChild parentAndChild) {
        var child = parentAndChild.getChildPublication();
        var parent = parentAndChild.getParentPublication();
        if (ChildParentInstanceComparator.isValidCombination(child, parent)) {
            var anthology = (Anthology) child.getEntityDescription().getReference().getPublicationContext();
            anthology.setId(createPartOfUri(parentAndChild.getParentPublication()));
        } else if (child.getEntityDescription().getReference().getPublicationContext() instanceof Anthology anthology) {
            anthology.setId(createPartOfUri(parentAndChild.getParentPublication()));
            persistErrorReport(child, parent, ChildPatchPublicationInstanceMismatchException.getExceptionName());
            anthology.setId(createPartOfUri(parentAndChild.getParentPublication()));
        } else {
            persistErrorReport(child, parent, ChildNotAnthologyException.getExceptionName());
        }
        return parentAndChild;
    }

    private void persistErrorReport(Publication child, Publication parent, String exceptionName) {
        var body = createErrorReportBody(child, parent);
        var unixPath = UnixPath.of(PATCH_ERROR_REPORT,
                                   exceptionName,
                                   getCristinIdentifier(child));
        attempt(() -> s3Driver.insertFile(unixPath, body)).orElseThrow();
    }

    private static String createErrorReportBody(Publication child, Publication parent) {
        return "Child:%s:Parent:%s".formatted(
            getPublicationsInstanceName(child),
            getPublicationsInstanceName(parent));
    }

    private static String getCristinIdentifier(Publication child) {
        return child.getAdditionalIdentifiers().stream()
                   .filter(CristinIdentifier.class::isInstance)
                   .map(CristinIdentifier.class::cast)
                   .filter(cristinIdentifier -> SourceName.CRISTIN_SYSTEM.equals(
                       cristinIdentifier.source().system()))
                   .map(CristinIdentifier::value)
                   .findFirst().orElse(child.getIdentifier().toString());
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
            degreePhd.getRelated().add(ConfirmedDocument.fromUri(createPartOfUri(child)));
            return parentAndChild;
        } else {
            throw new ParentPatchPublicationInstanceMismatchException(
                String.format(PARENT_CHILD_REFERENCE_MISMATCH,
                              getPublicationsInstanceName(parent),
                              getPublicationsInstanceName(child)));
        }
    }

    private static URI createPartOfUri(Publication parentPublication) {
        return UriWrapper.fromHost(NVA_API_DOMAIN)
                   .addChild(PUBLICATION_PATH)
                   .addChild(parentPublication.getIdentifier().toString())
                   .getUri();
    }
}
