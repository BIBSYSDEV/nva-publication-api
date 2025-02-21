package no.unit.nva.publication.model.storage;

import static no.unit.nva.publication.storage.model.DatabaseConstants.KEY_FIELDS_DELIMITER;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsRequest;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Objects;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.publication.model.business.UnpublishRequest;
import nva.commons.core.JacocoGenerated;

@JsonTypeName(UnpublishRequestDao.TYPE)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class UnpublishRequestDao extends TicketDao implements JoinWithResource, JsonSerializable {

    public static final String TYPE = "UnpublishRequest";
    public static final String JOIN_BY_RESOURCE_INDEX_ORDER_PREFIX = TicketDao.ALPHABETICALLY_ORDERED_LAST_TICKET_TYPE;

    @JacocoGenerated
    public UnpublishRequestDao() {
        super();
    }

    public UnpublishRequestDao(UnpublishRequest unpublishRequest) {
        super(unpublishRequest);
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

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getData());
    }

    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UnpublishRequestDao)) {
            return false;
        }
        UnpublishRequestDao that = (UnpublishRequestDao) o;
        return Objects.equals(getData(), that.getData());
    }
}
