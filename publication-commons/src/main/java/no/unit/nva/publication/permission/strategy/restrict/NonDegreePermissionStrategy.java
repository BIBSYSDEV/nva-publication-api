package no.unit.nva.publication.permission.strategy.restrict;

import static nva.commons.apigateway.AccessRight.MANAGE_DEGREE;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.publication.model.business.UserInstance;

public class NonDegreePermissionStrategy extends RestrictPermissionStrategy {

    public NonDegreePermissionStrategy(Publication publication, UserInstance userInstance) {
        super(publication, userInstance);
    }

    @Override
    public boolean deniesAction(PublicationOperation permission) {
        return switch (permission) {
            default -> canManage();
        };
    }

    private boolean canManage() {
        return isDegree() && !hasAccessRight(MANAGE_DEGREE) && !isUsersDraft();
    }

    private boolean isUsersDraft() {
        return isDraft() && isOwner();
    }
}
