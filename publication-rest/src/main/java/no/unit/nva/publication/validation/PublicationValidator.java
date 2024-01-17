package no.unit.nva.publication.validation;

import java.net.URI;
import no.unit.nva.model.Publication;

public interface PublicationValidator {
    void validate(Publication publication, URI customerUri) throws PublicationValidationException;
}
