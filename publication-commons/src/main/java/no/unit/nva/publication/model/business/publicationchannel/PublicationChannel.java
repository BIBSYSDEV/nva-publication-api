package no.unit.nva.publication.model.business.publicationchannel;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.net.URI;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.business.Entity;
import no.unit.nva.publication.model.storage.PublicationChannelDao;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({@JsonSubTypes.Type(name = ClaimedPublicationChannel.TYPE, value = ClaimedPublicationChannel.class),
    @JsonSubTypes.Type(name = NonClaimedPublicationChannel.TYPE, value = NonClaimedPublicationChannel.class)})
public sealed interface PublicationChannel extends Entity
    permits ClaimedPublicationChannel, NonClaimedPublicationChannel {

    String RESOURCE_IDENTIFIER_FIELD = "resourceIdentifier";
    String CREATED_DATE_FIELD = "createdDate";
    String MODIFIED_DATE_FIELD = "modifiedDate";
    String ORGANIZATION_ID_FIELD = "organizationId";
    String CONSTRAINT_FIELD = "constraint";
    String CHANNEL_TYPE_FIELD = "channelType";
    String IDENTIFIER_FIELD = "identifier";
    String ID_FIELD = "id";
    String CUSTOMER_ID_FIELD = "customerId";

    URI getId();

    @Override
    SortableIdentifier getIdentifier();

    SortableIdentifier getResourceIdentifier();

    ChannelType getChannelType();

    @Override
    PublicationChannelDao toDao();
}
