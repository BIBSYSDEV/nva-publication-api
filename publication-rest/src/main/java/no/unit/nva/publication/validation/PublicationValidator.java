package no.unit.nva.publication.validation;

import no.unit.nva.model.Publication;
import no.unit.nva.publication.commons.customer.Customer;

public interface PublicationValidator {
    void validate(Publication publication, Customer customer) throws PublicationValidationException;
}
