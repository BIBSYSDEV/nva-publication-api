package no.unit.nva.publication.model.business;

import com.fasterxml.jackson.annotation.JsonTypeName;
import nva.commons.core.JacocoGenerated;

import java.beans.ConstructorProperties;

@JsonTypeName("Customer")
@JacocoGenerated
public final class WorkFlowDto {
    private final PublicationWorkflow publicationWorkflow;

    @ConstructorProperties({"publicationWorkflow"})
    public WorkFlowDto(PublicationWorkflow publicationWorkflow) {
        this.publicationWorkflow = publicationWorkflow;
    }

    public PublicationWorkflow getPublication() {
        return publicationWorkflow;
    }


}
