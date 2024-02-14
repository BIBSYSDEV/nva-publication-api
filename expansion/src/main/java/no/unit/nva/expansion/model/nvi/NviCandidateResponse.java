package no.unit.nva.expansion.model.nvi;

import java.net.URI;

public record NviCandidateResponse(String status, Period period) {

    public ScientificIndex toNviStatus() {
        return new ScientificIndex(period.id(), period.year(), status);
    }
    public record Period(URI id, String year) {

    }
}
