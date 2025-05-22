package no.unit.nva.cristin.mapper.nva.exceptions;

public final class ContributorWithoutAffiliationException extends RuntimeException {

    private ContributorWithoutAffiliationException() {
        super();
    }

    public static String name() {
        return ContributorWithoutAffiliationException.class.getSimpleName();
    }
}
