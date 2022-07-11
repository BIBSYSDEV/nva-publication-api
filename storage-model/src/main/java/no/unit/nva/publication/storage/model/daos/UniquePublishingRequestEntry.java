package no.unit.nva.publication.storage.model.daos;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import no.unit.nva.publication.storage.model.PublishingRequestCase;

@JsonTypeName("PublishingRequestEntry")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class UniquePublishingRequestEntry extends UniquenessEntry {

    public static final String TYPE = "PublicationRequestEntry";

    private UniquePublishingRequestEntry() {
        super();
    }

    public UniquePublishingRequestEntry(String identifier) {
        super(identifier);
    }

    public static UniquePublishingRequestEntry create(PublishingRequestCase publishingRequest) {
        return new UniquePublishingRequestEntry(publishingRequest.getResourceIdentifier().toString());
    }

    @Override
    protected String getType() {
        return TYPE;
    }
}
