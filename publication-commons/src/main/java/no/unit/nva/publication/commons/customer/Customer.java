package no.unit.nva.publication.commons.customer;

import static java.util.Collections.emptySet;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Set;

public class Customer {
    private static final String FIELD_ALLOW_FILE_UPLOAD_FOR_TYPES = "allowFileUploadForTypes";
    private static final String FIELD_PUBLICATION_WORKFLOW = "publicationWorkflow";

    @JsonProperty(FIELD_ALLOW_FILE_UPLOAD_FOR_TYPES)
    private final Set<String> allowFileUploadForTypes;
    @JsonProperty(FIELD_PUBLICATION_WORKFLOW)
    private final String publicationWorkflow;

    @JsonCreator
    public Customer(@JsonProperty(FIELD_ALLOW_FILE_UPLOAD_FOR_TYPES) Set<String> allowFileUploadForTypes,
                    @JsonProperty(FIELD_PUBLICATION_WORKFLOW) String publicationWorkflow) {
        this.allowFileUploadForTypes = allowFileUploadForTypes;
        this.publicationWorkflow = publicationWorkflow;
    }

    public Set<String> getAllowFileUploadForTypes() {
        return allowFileUploadForTypes == null ? emptySet(): allowFileUploadForTypes;
    }

    public String getPublicationWorkflow() {
        return publicationWorkflow;
    }
}
