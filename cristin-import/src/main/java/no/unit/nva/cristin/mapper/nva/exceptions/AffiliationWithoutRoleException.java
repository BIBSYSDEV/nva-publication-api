package no.unit.nva.cristin.mapper.nva.exceptions;

public class AffiliationWithoutRoleException extends RuntimeException {

    public static final String ERROR_MESSAGE =
            "The affiliation has no role. All affiliations must have at least one role.";

    public AffiliationWithoutRoleException() {
        super(ERROR_MESSAGE);
    }
}
