package no.unit.nva.expansion.model.nvi;

public record NviCandidateResponse(StatusWithDescription reportStatus, String period) {

    public ScientificIndex toNviStatus() {
        return new ScientificIndex(period, reportStatus.status());
    }

    private record StatusWithDescription(String status) {

    }
}
