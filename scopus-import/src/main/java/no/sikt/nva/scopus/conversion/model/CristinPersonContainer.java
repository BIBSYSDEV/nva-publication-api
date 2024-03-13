package no.sikt.nva.scopus.conversion.model;

import java.net.URI;
import no.sikt.nva.scopus.conversion.model.cristin.CristinPerson;

public record CristinPersonContainer(AuthorIdentifiers authorIdentifiers, URI cristinId, CristinPerson cristinPerson) {

}
