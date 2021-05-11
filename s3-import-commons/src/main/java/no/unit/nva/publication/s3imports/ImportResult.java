package no.unit.nva.publication.s3imports;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.JsonSerializable;
import nva.commons.core.exceptions.ExceptionUtils;

public class ImportResult<I> implements JsonSerializable {

    @JsonIgnore
    public static final String INPUT_FIELD = "input";
    @JsonIgnore
    public static final String EXCEPTION_FIELD = "exception";
    @JsonIgnore
    public static final String STATUS_FIELD = "status";
    @JsonIgnore
    public static final String FAILURE = "FAILURE";
    @JsonProperty(INPUT_FIELD)
    private final I input;
    @JsonProperty(EXCEPTION_FIELD)
    private final String exception;
    @JsonProperty(STATUS_FIELD)
    private final String status;

    @JsonCreator
    @JacocoGenerated
    public ImportResult(@JsonProperty(INPUT_FIELD) I input,
                        @JsonProperty(EXCEPTION_FIELD) String exception,
                        @JsonProperty(STATUS_FIELD) String status) {
        this.input = input;
        this.exception = exception;
        this.status = status;
    }

    private ImportResult(I input, Exception exception, String status) {
        this.input = input;
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
    public I getInput() {
        return input;
    }

    @JacocoGenerated
    public String getException() {
        return exception;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getInput(), getException(), getStatus());
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
        return Objects.equals(getInput(), that.getInput())
               && Objects.equals(getException(), that.getException())
               && Objects.equals(getStatus(), that.getStatus());
    }
}
