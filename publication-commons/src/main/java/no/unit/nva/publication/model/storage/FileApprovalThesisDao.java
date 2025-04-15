package no.unit.nva.publication.model.storage;

import static no.unit.nva.publication.model.storage.PublishingRequestDao.BY_RESOURCE_INDEX_ORDER_PREFIX;
import static no.unit.nva.publication.storage.model.DatabaseConstants.KEY_FIELDS_DELIMITER;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsRequest;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.publication.model.business.FilesApprovalThesis;
import no.unit.nva.publication.model.business.TicketEntry;
import nva.commons.core.JacocoGenerated;

@JsonTypeName(FileApprovalThesisDao.TYPE)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class FileApprovalThesisDao extends TicketDao implements JoinWithResource, JsonSerializable {

    public static final String TYPE = "FileApprovalThesis";

    @JacocoGenerated
    public FileApprovalThesisDao() {
        super();
    }

    public FileApprovalThesisDao(TicketEntry data) {
        super(data);
    }

    @Override
    public TransactWriteItemsRequest createInsertionTransactionRequest() {
        var insertionEntry = createInsertionEntry();
        var identifierEntry = createUniqueIdentifierEntry();
        return new TransactWriteItemsRequest().withTransactItems(identifierEntry, insertionEntry);
    }

    @Override
    public String joinByResourceOrderedType() {
        return joinByResourceContainedOrderedType();
    }

    @JsonIgnore
    private static String joinByResourceContainedOrderedType() {
        return BY_RESOURCE_INDEX_ORDER_PREFIX + KEY_FIELDS_DELIMITER + FilesApprovalThesis.TYPE;
    }

    private TransactWriteItem createUniqueIdentifierEntry() {
        var identifierEntry = new IdentifierEntry(getData().getIdentifier().toString());
        return newPutTransactionItem(identifierEntry);
    }

    private TransactWriteItem createInsertionEntry() {
        return newPutTransactionItem(new FileApprovalThesisDao(getTicketEntry()));
    }
}
