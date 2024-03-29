package no.unit.nva.publication.model.storage;

import static no.unit.nva.publication.storage.model.DatabaseConstants.KEY_FIELDS_DELIMITER;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsRequest;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.publication.model.business.GeneralSupportRequest;

@JsonTypeName(GeneralSupportRequestDao.TYPE)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class GeneralSupportRequestDao extends TicketDao implements JsonSerializable {
    
    public static final String JOIN_BY_RESOURCE_INDEX_ORDER_PREFIX = "d";
    
    public static final String TYPE = "GeneralSupportCase";
    public static final String LEGACY_TYPE = "GeneralSupportRequest";
    
    public GeneralSupportRequestDao() {
        super();
    }
    
    public GeneralSupportRequestDao(GeneralSupportRequest data) {
        super(data);
    }
    
    @Override
    public URI getCustomerId() {
        return getData().getCustomerId();
    }
    
    @Override
    public TransactWriteItemsRequest createInsertionTransactionRequest() {
        var dataEntry = newPutTransactionItem(this);
        var uniquenessEntry = newPutTransactionItem(new IdentifierEntry(this));
        return new TransactWriteItemsRequest().withTransactItems(dataEntry, uniquenessEntry);
    }

    @Override
    public String joinByResourceOrderedType() {
        return JOIN_BY_RESOURCE_INDEX_ORDER_PREFIX + KEY_FIELDS_DELIMITER + indexingType();
    }
}
