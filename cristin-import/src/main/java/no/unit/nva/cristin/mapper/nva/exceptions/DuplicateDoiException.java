package no.unit.nva.cristin.mapper.nva.exceptions;

import java.net.URI;

public class DuplicateDoiException extends RuntimeException {
    public DuplicateDoiException(URI doi) {
        super(String.format("Doi %s already exists", doi));
    }
}
