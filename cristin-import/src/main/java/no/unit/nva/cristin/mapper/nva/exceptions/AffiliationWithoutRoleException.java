package no.unit.nva.cristin.mapper.nva.exceptions;

public final class AffiliationWithoutRoleException extends RuntimeException {

    private AffiliationWithoutRoleException() {
        super();
    }

    public static String name() {
        return AffiliationWithoutRoleException.class.getSimpleName();
    }
}
