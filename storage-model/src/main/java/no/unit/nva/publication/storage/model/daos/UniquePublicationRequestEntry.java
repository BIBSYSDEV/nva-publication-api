package no.unit.nva.publication.storage.model.daos;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import nva.commons.core.JacocoGenerated;

@JacocoGenerated
@JsonTypeName("PublicationRequestEntry")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class UniquePublicationRequestEntry extends UniquenessEntry {

    private static final String TYPE = "PublicationRequestEntry";

    public UniquePublicationRequestEntry() {
        super();
    }

    public UniquePublicationRequestEntry(String identifier) {
        super(identifier);
    }

    public static UniquePublicationRequestEntry create(PublishingRequestDao publicationRequestDao) {
        return new UniquePublicationRequestEntry(publicationRequestDao.getResourceIdentifier().toString());
    }

    @Override
    protected String getType() {
        return TYPE;
    }
}
