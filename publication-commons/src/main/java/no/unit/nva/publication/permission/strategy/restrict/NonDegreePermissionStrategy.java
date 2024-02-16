package no.unit.nva.publication.permission.strategy.restrict;

import static java.util.Objects.nonNull;
import static no.unit.nva.model.role.Role.CREATOR;
import static nva.commons.apigateway.AccessRight.MANAGE_DEGREE;
import static nva.commons.core.attempt.Try.attempt;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.model.role.RoleType;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permission.strategy.grant.GrantPermissionStrategy;
import nva.commons.apigateway.AccessRight;

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
