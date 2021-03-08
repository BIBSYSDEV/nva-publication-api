package no.unit.nva.publication.migration;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Objects;
import java.util.Optional;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.storage.model.DoiRequest;
import nva.commons.core.JacocoGenerated;
import org.javers.core.Javers;
import org.javers.core.JaversBuilder;
import org.javers.core.diff.Diff;

public final class ResourceUpdate {

    public static final String NOT_COMPARED_VERSIONS_ERROR = "You should call compareVersions first";
    private static final Javers JAVERS = JaversBuilder.javers().registerIgnoredClass(DoiRequest.class).build();
    private static final boolean SUCCESS = true;
    private static final boolean FAILURE = false;

    @JsonProperty("resourceType")
    private final String resourceType;
    @JsonProperty("oldVersion")
    private final Publication oldVersion;
    @JsonProperty("newVersion")
    private final Publication newVersion;
    @JsonIgnore
    private final Exception exception;
    @JsonProperty("success")
    private final boolean success;
    @JsonIgnore
    private final Diff diff;

    private ResourceUpdate(
        String resourceType,
        Publication oldVersion,
        Publication newVersion,
        Exception exception,
        boolean success,
        Diff diff) {
        this.resourceType = resourceType;
        this.oldVersion = oldVersion;
        this.newVersion = newVersion;
        this.exception = exception;
        this.success = success;
        this.diff = diff;
    }

    public static ResourceUpdate createSuccessfulUpdate(String resourceType, Publication oldVersion,
                                                        Publication newVersion) {
        return new ResourceUpdate(resourceType, oldVersion, newVersion, null, SUCCESS, null);
    }

    public static ResourceUpdate createFailedUpdate(String resourceType, Publication oldVersion, Exception exception) {
        return new ResourceUpdate(resourceType, oldVersion, null, exception, FAILURE, null);
    }

    public ResourceUpdate compareVersions() {
        return new ResourceUpdate(resourceType, oldVersion, newVersion, exception, success, difference());
    }

    @JacocoGenerated
    @JsonProperty("difference")
    public String getDifference() {
        return Optional.ofNullable(diff).map(Diff::prettyPrint).orElse(null);
    }

    @JacocoGenerated
    @JsonProperty("exception")
    public String getExceptionString() {
        StringWriter writer = new StringWriter();
        PrintWriter printer = new PrintWriter(writer);
        if (nonNull(exception)) {
            exception.printStackTrace(printer);
        }

        return writer.toString();
    }

    @JacocoGenerated
    @JsonIgnore
    public Exception getException() {
        return exception;
    }

    @JacocoGenerated
    public boolean isSuccess() {
        return success;
    }

    @JacocoGenerated
    public boolean isFailure() {
        return !isSuccess();
    }

    @JacocoGenerated
    public Publication getOldVersion() {
        return oldVersion;
    }

    @JacocoGenerated
    public Publication getNewVersion() {
        return newVersion;
    }

    @JacocoGenerated
    @JsonIgnore
    public boolean isDoiRequestUpdate() {
        return nonNull(getNewVersion().getDoiRequest());
    }

    public boolean versionsAreEquivalent() {
        return versionsAreEqual() || versionsAreSemanticallyEquivalent();
    }

    private boolean versionsAreEqual() {
        return Objects.equals(newVersion, oldVersion);
    }

    private boolean versionsAreSemanticallyEquivalent() {
        if (comparisonHasNotBeenExecuted()) {
            throw new IllegalStateException(NOT_COMPARED_VERSIONS_ERROR);
        }
        return resourceIsPublication() && !diff.hasChanges();
    }

    private boolean comparisonHasNotBeenExecuted() {
        return resourceIsPublication() && isNull(diff);
    }

    private Diff difference() {
        if (resourceIsPublication()) {
            return JAVERS.compare(oldVersion, newVersion);
        }
        return null;
    }

    private boolean resourceIsPublication() {
        return resourceType.equals(PublicationImporter.RESOURCE_TYPE);
    }
}
