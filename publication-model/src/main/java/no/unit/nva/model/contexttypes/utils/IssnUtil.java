package no.unit.nva.model.contexttypes.utils;

import static java.util.Objects.isNull;
import no.unit.nva.model.exceptions.InvalidIssnException;
import org.apache.commons.validator.routines.ISSNValidator;

public final class IssnUtil {
    /**
     * Returns a valid ISSN or null.
     *
     * @param issn a valid ISSN
     * @return String, validated representation of the ISSN
     * @throws InvalidIssnException Thrown if the ISSN is invalid
     */
    @SuppressWarnings("PMD.NullAssignment")
    public static String checkIssn(String issn) throws InvalidIssnException {
        if (isNull(issn) || issn.isEmpty()) {
            return null;
        }
        if (new ISSNValidator().isValid(issn)) {
            return issn;
        } else {
            throw new InvalidIssnException(issn);
        }
    }
}
