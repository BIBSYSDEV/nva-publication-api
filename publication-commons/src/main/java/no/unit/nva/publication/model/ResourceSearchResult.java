package no.unit.nva.publication.model;

import java.util.List;
import no.unit.nva.publication.model.business.Resource;

public record ResourceSearchResult(int totalHits, int hitsReturned, List<Resource> resources) {}
