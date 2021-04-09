package no.unit.nva.dataimport;

import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.parallel.ParallelExecutionException;

public class ImportResult {

    @JsonProperty("errorMessage")
    private final String errorMessage;
    @JsonProperty("filename")
    private final String filename;

    @JsonCreator
    public ImportResult(@JsonProperty("filename") String filename,
                        @JsonProperty("errorMessage") String errorMessage) {
        this.errorMessage = errorMessage;
        this.filename = filename;
    }

    public static ImportResult fromParallelExecutionException(ParallelExecutionException failure) {
        String filename = (String) failure.getInput();
        String errorMessage = attempt(failure::getCause)
                                  .map(Throwable::getMessage)
                                  .toOptional()
                                  .orElse(failure.getMessage());
        return new ImportResult(filename, errorMessage);
    }

    @JacocoGenerated
    public String getErrorMessage() {
        return errorMessage;
    }

    @JacocoGenerated
    public String getFilename() {
        return filename;
    }
}
