package no.unit.nva.publication.model.business;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.SingletonCollector;

public enum PublishingWorkflow {
    REGISTRATOR_PUBLISHES_METADATA_ONLY("RegistratorPublishesMetadataOnly"),
    REGISTRATOR_PUBLISHES_METADATA_AND_FILES("RegistratorPublishesMetadataAndFiles"),
    REGISTRATOR_REQUIRES_APPROVAL_FOR_METADATA_AND_FILES("RegistratorRequiresApprovalForMetadataAndFiles");

    public static final String ERROR_MESSAGE_TEMPLATE = "%s not a valid PublicationWorkflow, expected one of: %s";
    public static final String DELIMITER = ", ";

    private final String value;

    PublishingWorkflow(String value) {
        this.value = value;
    }

    @JsonCreator
    public static PublishingWorkflow lookUp(String value) {
        return stream(values())
                   .filter(nameType -> nameType.getValue().equalsIgnoreCase(value))
                   .collect(SingletonCollector.tryCollect())
                   .orElseThrow(failure -> throwException(value));
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JacocoGenerated
    private static RuntimeException throwException(String value) {
        var validValues = stream(PublishingWorkflow.values())
                              .map(PublishingWorkflow::toString)
                              .collect(joining(DELIMITER));
        return new IllegalArgumentException(format(ERROR_MESSAGE_TEMPLATE, value, validValues));
    }

}
