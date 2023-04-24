package no.unit.nva.publication.ticket.model.identityservice;

import com.fasterxml.jackson.annotation.JsonProperty;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.publication.model.business.PublishingWorkflow;

public class CustomerPublishingWorkflowResponse implements JsonSerializable {

    private static final String PUBLISHING_WORKFLOW_FIELD = "publicationWorkflow";
    @JsonProperty(PUBLISHING_WORKFLOW_FIELD)
    private final String publishingWorkflow;


    public CustomerPublishingWorkflowResponse(@JsonProperty(PUBLISHING_WORKFLOW_FIELD) String publishingWorkflow) {
        this.publishingWorkflow = publishingWorkflow;
    }

    public PublishingWorkflow convertToPublishingWorkflow() {
        return PublishingWorkflow.lookUp(publishingWorkflow);
    }
}
