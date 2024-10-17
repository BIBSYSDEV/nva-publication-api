package no.unit.nva.model;

import java.net.URI;
import java.util.List;

public record CuratingInstitution(URI id, List<URI> curatedContributors) {

}
