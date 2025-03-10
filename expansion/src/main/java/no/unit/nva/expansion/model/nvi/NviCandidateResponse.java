package no.unit.nva.expansion.model.nvi;

public record NviCandidateResponse(Status reportStatus, String period) {

    public ScientificIndex toNviStatus() {
        return new ScientificIndex(period, reportStatus.status());
    }

    private record Status(String status) {

    }
}
