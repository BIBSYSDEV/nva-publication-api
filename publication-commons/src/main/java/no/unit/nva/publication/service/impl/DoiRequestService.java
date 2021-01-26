package no.unit.nva.publication.service.impl;

import static no.unit.nva.publication.service.impl.ResourceServiceUtils.parseAttributeValuesMap;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCES_TABLE_NAME;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsRequest;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsResult;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.DoiRequestStatus;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.service.impl.exceptions.BadRequestException;
import no.unit.nva.publication.storage.model.DoiRequest;
import no.unit.nva.publication.storage.model.daos.DoiRequestDao;
import no.unit.nva.publication.storage.model.daos.IdentifierEntry;
import no.unit.nva.publication.storage.model.daos.UniqueDoiRequestEntry;
import no.unit.nva.publication.storage.model.daos.WithPrimaryKey;
import nva.commons.core.attempt.Failure;

public class DoiRequestService {

    private final AmazonDynamoDB client;
    private final Clock clock;
    private final ResourceService resourceService;
    private final String tableName;

    public DoiRequestService(AmazonDynamoDB client, Clock clock) {
        this.client = client;
        this.clock = clock;
        this.resourceService = new ResourceService(client, clock);
        this.tableName = RESOURCES_TABLE_NAME;
    }

    public SortableIdentifier createDoiRequest(UserInstance userInstance, SortableIdentifier resourceIdentifier)
        throws BadRequestException {

        Publication publication = fetchPublication(userInstance, resourceIdentifier);
        DoiRequest doiRequest = createNewDoiRequestEntry(publication);
        TransactWriteItemsRequest request = createInsertionTransactionRequest(doiRequest);

        attempt(() -> client.transactWriteItems(request))
            .orElseThrow(this::handleFailedTransactionError);
        return doiRequest.getIdentifier();
    }

    private BadRequestException handleFailedTransactionError(Failure<TransactWriteItemsResult> fail) {
        return new BadRequestException(fail.getException());
    }

    public DoiRequest getDoiRequest(UserInstance userInstance, SortableIdentifier identifier) {

        DoiRequestDao queryObject = DoiRequestDao
            .queryObject(userInstance.getOrganizationUri(), userInstance.getUserIdentifier(), identifier);
        GetItemRequest getItemRequest = new GetItemRequest()
            .withTableName(RESOURCES_TABLE_NAME)
            .withKey(queryObject.primaryKey());
        GetItemResult result = client.getItem(getItemRequest);
        Map<String, AttributeValue> item = result.getItem();
        DoiRequestDao dao = parseAttributeValuesMap(item, DoiRequestDao.class);
        return dao.getData();
    }

    private Publication fetchPublication(UserInstance userInstance, SortableIdentifier resourceIdentifier)
        throws BadRequestException {
        return attempt(() -> resourceService.getPublication(userInstance, resourceIdentifier))
            .orElseThrow(this::handleResourceNotFetchedError);
    }

    private BadRequestException handleResourceNotFetchedError(Failure<Publication> fail) {
        return new BadRequestException(fail.getException());
    }

    private DoiRequest createNewDoiRequestEntry(Publication publication) {
        Instant now = clock.instant();
        return new DoiRequest(
            SortableIdentifier.next(),
            publication.getIdentifier(),
            publication.getOwner(),
            publication.getPublisher().getId(),
            DoiRequestStatus.REQUESTED.toString(),
            now,
            now);
    }

    private TransactWriteItemsRequest createInsertionTransactionRequest(DoiRequest doiRequest) {
        TransactWriteItem doiRequestEntry = createDoiRequestInsertionEntry(doiRequest);
        TransactWriteItem identifierEntry = createUniqueIdentifierEntry(doiRequest);
        TransactWriteItem uniqueDoiRequestEntry = createUniqueDoiRequestEntry(doiRequest);

        return new TransactWriteItemsRequest()
            .withTransactItems(
                identifierEntry,
                uniqueDoiRequestEntry,
                doiRequestEntry);
    }

    private TransactWriteItem createUniqueDoiRequestEntry(DoiRequest doiRequest) {
        UniqueDoiRequestEntry uniqueDoiRequestEntry = new UniqueDoiRequestEntry(
            doiRequest.getResourceIdentifier().toString());
        return createTransactionPutEntry(uniqueDoiRequestEntry);
    }

    private <T extends WithPrimaryKey> TransactWriteItem createTransactionPutEntry(T uniqueDoiRequestEntry) {
        return ResourceServiceUtils.createTransactionPutEntry(uniqueDoiRequestEntry, tableName);
    }

    private TransactWriteItem createDoiRequestInsertionEntry(DoiRequest doiRequest) {
        return createTransactionPutEntry(new DoiRequestDao(doiRequest));
    }

    private TransactWriteItem createUniqueIdentifierEntry(DoiRequest doiRequest) {
        IdentifierEntry identifierEntry = new IdentifierEntry(doiRequest.getIdentifier().toString());
        return createTransactionPutEntry(identifierEntry);
    }
}
