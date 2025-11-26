package no.sikt.nva.scopus.conversion.model;

import java.net.URI;
import no.unit.nva.publication.external.services.cristin.CristinPerson;

public record CristinPersonContainer(AuthorIdentifiers authorIdentifiers, URI cristinId, CristinPerson cristinPerson) {

}
