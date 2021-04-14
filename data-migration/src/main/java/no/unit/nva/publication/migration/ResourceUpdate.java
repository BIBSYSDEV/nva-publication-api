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

    @JsonIgnore
    private Exception exceptionObject;
    @JsonIgnore
    private Diff diffObject;

    @JsonProperty("resourceType")
    private String resourceType;
    @JsonProperty("oldVersion")
    private Publication oldVersion;
    @JsonProperty("newVersion")
    private Publication newVersion;
    @JsonProperty("success")
    private boolean success;
    @JsonProperty("exception")
    private String exception;
    @JsonProperty("difference")
    private String difference;

    public ResourceUpdate() {

    }

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
        this.exceptionObject = exception;
        this.exception = constructExceptionString(exception);
        this.success = success;
        this.diffObject = diff;
        this.difference = constructDifferenceString(diff);
    }

    public static ResourceUpdate createSuccessfulUpdate(String resourceType, Publication oldVersion,
                                                        Publication newVersion) {
        return new ResourceUpdate(resourceType, oldVersion, newVersion, null, SUCCESS, null);
    }

    public static ResourceUpdate createFailedUpdate(String resourceType, Publication oldVersion, Exception exception) {
        return new ResourceUpdate(resourceType, oldVersion, null, exception, FAILURE, null);
    }

    @JacocoGenerated
    public String getResourceType() {
        return resourceType;
    }

    @JacocoGenerated
    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public ResourceUpdate compareVersions() {
        return new ResourceUpdate(resourceType, oldVersion, newVersion, exceptionObject, success,
                                  calculateDifference());
    }

    @JacocoGenerated
    public String getDifference() {
        return difference;
    }

    @JacocoGenerated
    public void setDifference(String difference) {
        this.difference = difference;
    }

    @JacocoGenerated
    @JsonProperty("exception")
    public String getException() {
        return exception;
    }

    @JacocoGenerated
    public void setException(String exception) {
        this.exception = exception;
    }

    @JacocoGenerated
    public boolean isSuccess() {
        return success;
    }

    @JacocoGenerated
    public void setSuccess(boolean success) {
        this.success = success;
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
    public void setOldVersion(Publication oldVersion) {
        this.oldVersion = oldVersion;
    }

    @JacocoGenerated
    public Publication getNewVersion() {
        return newVersion;
    }

    @JacocoGenerated
    public void setNewVersion(Publication newVersion) {
        this.newVersion = newVersion;
    }

    @JacocoGenerated
    @JsonIgnore
    public boolean isDoiRequestUpdate() {
        return nonNull(getNewVersion().getDoiRequest());
    }

    public boolean versionsAreEquivalent() {
        return versionsAreEqual() || versionsAreSemanticallyEquivalent();
    }

    private String constructDifferenceString(Diff diff) {
        return Optional.ofNullable(diff).map(Diff::prettyPrint).orElse(null);
    }

    private String constructExceptionString(Exception exception) {
        StringWriter writer = new StringWriter();
        PrintWriter printer = new PrintWriter(writer);
        if (nonNull(exception)) {
            exception.printStackTrace(printer);
        }

        return writer.toString();
    }

    private boolean versionsAreEqual() {
        return Objects.equals(newVersion, oldVersion);
    }

    private boolean versionsAreSemanticallyEquivalent() {
        if (comparisonHasNotBeenExecuted()) {
            throw new IllegalStateException(NOT_COMPARED_VERSIONS_ERROR);
        }
        return resourceIsPublication() && !diffObject.hasChanges();
    }

    private boolean comparisonHasNotBeenExecuted() {
        return resourceIsPublication() && isNull(difference);
    }

    private Diff calculateDifference() {
        if (resourceIsPublication()) {
            return JAVERS.compare(oldVersion, newVersion);
        }
        return null;
    }

    private boolean resourceIsPublication() {
        return resourceType.equals(PublicationImporter.RESOURCE_TYPE);
    }
}
