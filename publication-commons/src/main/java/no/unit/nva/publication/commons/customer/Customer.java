package no.unit.nva.publication.commons.customer;

import static java.util.Collections.emptySet;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Set;

public class Customer {
    private static final String FIELD_ALLOW_FILE_UPLOAD_FOR_TYPES = "allowFileUploadForTypes";
    private static final String FIELD_PUBLICATION_WORKFLOW = "publicationWorkflow";
    private static final String FIELD_RIGHTS_RETENTION_STRATEGY = "rightsRetentionStrategy";

    @JsonProperty(FIELD_ALLOW_FILE_UPLOAD_FOR_TYPES)
    private final Set<String> allowFileUploadForTypes;
    @JsonProperty(FIELD_PUBLICATION_WORKFLOW)
    private final String publicationWorkflow;

    @JsonProperty(FIELD_RIGHTS_RETENTION_STRATEGY)
    private final CustomerApiRightsRetention rightsRetentionStrategy;

    @JsonCreator
    public Customer(@JsonProperty(FIELD_ALLOW_FILE_UPLOAD_FOR_TYPES) Set<String> allowFileUploadForTypes,
                    @JsonProperty(FIELD_PUBLICATION_WORKFLOW) String publicationWorkflow,
                    @JsonProperty(FIELD_RIGHTS_RETENTION_STRATEGY) CustomerApiRightsRetention rightsRetentionStrategy) {
        this.allowFileUploadForTypes = allowFileUploadForTypes;
        this.publicationWorkflow = publicationWorkflow;
        this.rightsRetentionStrategy = rightsRetentionStrategy;
    }

    public Set<String> getAllowFileUploadForTypes() {
        return allowFileUploadForTypes == null ? emptySet() : allowFileUploadForTypes;
    }

    public String getPublicationWorkflow() {
        return publicationWorkflow;
    }

    public CustomerApiRightsRetention getRightsRetentionStrategy() {
        return rightsRetentionStrategy;
    }
}
