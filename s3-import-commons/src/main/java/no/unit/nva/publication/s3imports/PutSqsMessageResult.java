package no.unit.nva.publication.s3imports;

import java.util.List;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.events.models.EventReference;

public class PutSqsMessageResult implements JsonSerializable {

    private List<PutSqsMessageResultFailureEntry> failures;
    private List<EventReference> successes;

    public PutSqsMessageResult() {
        failures = List.of();
        successes = List.of();
    }

    public List<PutSqsMessageResultFailureEntry> getFailures() {
        return failures;
    }

    public void setFailures(List<PutSqsMessageResultFailureEntry> failures) {
        this.failures = failures;
    }

    public List<EventReference> getSuccesses() {
        return successes;
    }

    public void setSuccesses(List<EventReference> successes) {
        this.successes = successes;
    }

    public PutSqsMessageResult combine(PutSqsMessageResult putSqsMessageResult) {
        this.failures.addAll(putSqsMessageResult.getFailures());
        this.successes.addAll(putSqsMessageResult.getSuccesses());
        return this;
    }
}
