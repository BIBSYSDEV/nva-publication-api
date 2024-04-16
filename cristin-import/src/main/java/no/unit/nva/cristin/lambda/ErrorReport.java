package no.unit.nva.cristin.lambda;

import static nva.commons.core.attempt.Try.attempt;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.Environment;
import nva.commons.core.paths.UnixPath;
import software.amazon.awssdk.services.s3.S3Client;

public final class ErrorReport {

    public static final String CRISTIN_IMPORT_BUCKET = new Environment().readEnv("CRISTIN_IMPORT_BUCKET");
    public static final String ERROR_REPORT = "ERROR_REPORT";
    private String exception;
    private String body;
    private String cristinId;



    public static ErrorReport exceptionName(String exceptionName) {
        return builder().withException(exceptionName).build();
    }

    public ErrorReport withCristinId(Integer cristinId) {
        return copy().withCristinId(cristinId.toString()).build();
    }

    public ErrorReport withBody(String body) {
        return copy().withBody(body).build();
    }

    public void persist(S3Client s3Client) {
        var driver = new S3Driver(s3Client, CRISTIN_IMPORT_BUCKET);
        var location = createLocation();
        attempt(() -> driver.insertFile(location, body)).orElseThrow();
    }

    private UnixPath createLocation() {
        return UnixPath.fromString(ERROR_REPORT)
                   .addChild(exception)
                   .addChild(cristinId);
    }

    private static Builder builder() {
        return new Builder();
    }

    private Builder copy() {
        return builder().withBody(this.body).withException(this.exception);
    }

    public static final class Builder {

        private String exceptionName;
        private String body;
        private String cristinId;

        private Builder() {
        }

        public Builder withException(String exception) {
            this.exceptionName = exception;
            return this;
        }

        public Builder withCristinId(String cristinId) {
            this.cristinId = cristinId;
            return this;
        }

        public Builder withBody(String body) {
            this.body = body;
            return this;
        }

        public ErrorReport build() {
            ErrorReport errorReport = new ErrorReport();
            errorReport.exception = this.exceptionName;
            errorReport.body = this.body;
            errorReport.cristinId = this.cristinId;
            return errorReport;
        }
    }
}
