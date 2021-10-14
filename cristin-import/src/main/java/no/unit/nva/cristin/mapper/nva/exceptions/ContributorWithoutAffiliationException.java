package no.unit.nva.cristin.mapper.nva.exceptions;

public class ContributorWithoutAffiliationException extends RuntimeException {

    public static final String ERROR_MESSAGE =
            "The contributor has no affiliation. All contributors must have affiliations.";

    public ContributorWithoutAffiliationException() {
        super(ERROR_MESSAGE);
    }
}
