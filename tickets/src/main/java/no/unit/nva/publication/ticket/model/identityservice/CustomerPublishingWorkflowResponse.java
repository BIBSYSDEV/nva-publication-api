package no.unit.nva.publication.ticket.model.identityservice;

import com.fasterxml.jackson.annotation.JsonProperty;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.publication.model.business.PublishingWorkflow;
import nva.commons.apigateway.exceptions.BadGatewayException;

public class CustomerPublishingWorkflowResponse implements JsonSerializable {

    private static final String PUBLISHING_WORKFLOW_FIELD = "publicationWorkflow";
    private static final String REGISTRATOR_PUBLISHES_METADATA_AND_FILES = "RegistratorPublishesMetadataAndFiles";
    private static final String REGISTRATOR_PUBLISHES_METADATA_ONLY = "RegistratorPublishesMetadataOnly";
    private static final String REGISTRATOR_REQUIRES_APPROVAL_FOR_METADATA_AND_FILES = "RegistratorRequiresApprovalForMetadataAndFiles";
    @JsonProperty(PUBLISHING_WORKFLOW_FIELD)
    private final String publishingWorkflow;


    public CustomerPublishingWorkflowResponse(@JsonProperty(PUBLISHING_WORKFLOW_FIELD) String publishingWorkflow) {
        this.publishingWorkflow = publishingWorkflow;
    }

    public PublishingWorkflow convertToPublishingWorkflow() throws BadGatewayException {
        switch (publishingWorkflow) {
            case REGISTRATOR_PUBLISHES_METADATA_AND_FILES:
                return PublishingWorkflow.REGISTRATOR_PUBLISHES_METADATA_AND_FILES;
            case REGISTRATOR_PUBLISHES_METADATA_ONLY:
                return PublishingWorkflow.REGISTRATOR_PUBLISHES_METADATA_ONLY;
            case REGISTRATOR_REQUIRES_APPROVAL_FOR_METADATA_AND_FILES:
                return PublishingWorkflow.REGISTRATOR_REQUIRES_APPROVAL_FOR_METADATA_AND_FILES;
            default:
                throw new BadGatewayException("Unable to resolve customer publishing workflow");
        }
    }
}
