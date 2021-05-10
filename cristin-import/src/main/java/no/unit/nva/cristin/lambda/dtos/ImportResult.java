package no.unit.nva.cristin.lambda.dtos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.JsonSerializable;
import nva.commons.core.exceptions.ExceptionUtils;

public class ImportResult<I> implements JsonSerializable {

    @JsonIgnore
    public static final String EVENT_FIELD = "event";
    @JsonIgnore
    public static final String EXCEPTION_FIELD = "exception";
    @JsonIgnore
    public static final String STATUS_FIELD = "status";
    @JsonIgnore
    private static final String FAILURE = "FAILURE";
    @JsonProperty(EVENT_FIELD)
    private final I event;
    @JsonProperty(EXCEPTION_FIELD)
    private final String exception;
    @JsonProperty(STATUS_FIELD)
    private final String status;

    @JsonCreator
    @JacocoGenerated
    public ImportResult(@JsonProperty(EVENT_FIELD) I event,
                        @JsonProperty(EXCEPTION_FIELD) String exception,
                        @JsonProperty(STATUS_FIELD) String status) {
        this.event = event;
        this.exception = exception;
        this.status = status;
    }

    private ImportResult(I event, Exception exception, String status) {
        this.event = event;
        this.exception = ExceptionUtils.stackTraceInSingleLine(exception);
        this.status = status;
    }

    public static <I> ImportResult<I> reportFailure(I event, Exception exception) {
        return new ImportResult<>(event, exception, FAILURE);
    }

    @JacocoGenerated
    public String getStatus() {
        return status;
    }

    @JacocoGenerated
    public I getEvent() {
        return event;
    }

    @JacocoGenerated
    public String getException() {
        return exception;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getEvent(), getException(), getStatus());
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ImportResult)) {
            return false;
        }
        ImportResult<?> that = (ImportResult<?>) o;
        return Objects.equals(getEvent(), that.getEvent())
               && Objects.equals(getException(), that.getException())
               && Objects.equals(getStatus(), that.getStatus());
    }
}
