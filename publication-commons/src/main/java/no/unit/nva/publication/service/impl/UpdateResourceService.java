package no.unit.nva.publication.service.impl;

import static no.unit.nva.publication.service.impl.ResourceServiceUtils.PRIMARY_KEY_EQUALITY_CHECK_EXPRESSION;
import static no.unit.nva.publication.service.impl.ResourceServiceUtils.PRIMARY_KEY_EQUALITY_CONDITION_ATTRIBUTE_NAMES;
import static no.unit.nva.publication.service.impl.ResourceServiceUtils.newTransactWriteItemsRequest;
import static no.unit.nva.publication.service.impl.ResourceServiceUtils.primaryKeyEqualityConditionAttributeValues;
import static no.unit.nva.publication.service.impl.ResourceServiceUtils.toDynamoFormat;
import static no.unit.nva.publication.service.impl.ResourceServiceUtils.userOrganization;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.Put;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsRequest;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.storage.model.DoiRequest;
import no.unit.nva.publication.storage.model.Resource;
import no.unit.nva.publication.storage.model.UserInstance;
import no.unit.nva.publication.storage.model.daos.DoiRequestDao;
import no.unit.nva.publication.storage.model.daos.ResourceDao;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.attempt.Failure;

public class UpdateResourceService extends ServiceWithTransactions {

    private final String tableName;
    private final AmazonDynamoDB client;
    private final Clock clockForTimestamps;
    private final ReadResourceService readResourceService;

    public UpdateResourceService(AmazonDynamoDB client, String tableName, Clock clockForTimestamps,
                                 ReadResourceService readResourceService) {
        super();
        this.tableName = tableName;
        this.client = client;
        this.clockForTimestamps = clockForTimestamps;
        this.readResourceService = readResourceService;
    }

    public Publication updatePublication(Publication publication) throws ConflictException {
        Resource resource = Resource.fromPublication(publication);
        UserInstance userInstance = new UserInstance(resource.getOwner(), resource.getCustomerId());

        TransactWriteItem updateResourceTransactionItem = updateResource(resource);
        Optional<TransactWriteItem> updateDoiRequestTransactionItem = updateDoiRequest(userInstance, resource);
        ArrayList<TransactWriteItem> transactionItems = new ArrayList<>();
        transactionItems.add(updateResourceTransactionItem);
        updateDoiRequestTransactionItem.ifPresent(transactionItems::add);

        TransactWriteItemsRequest request = new TransactWriteItemsRequest().withTransactItems(transactionItems);
        sendTransactionWriteRequest(request);

        return publication;
    }

    public void updateOwner(SortableIdentifier identifier, UserInstance oldOwner, UserInstance newOwner)
        throws NotFoundException, ConflictException {
        Resource existingResource = readResourceService.getResource(oldOwner, identifier);
        Resource newResource = updateResourceOwner(newOwner, existingResource);
        TransactWriteItem deleteAction = newDeleteTransactionItem(existingResource);
        TransactWriteItem insertionAction = createTransactionEntryForInsertingResource(newResource);
        TransactWriteItemsRequest request = newTransactWriteItemsRequest(deleteAction, insertionAction);
        sendTransactionWriteRequest(request);
    }

    @Override
    public String getTableName() {
        return tableName;
    }

    @Override
    protected AmazonDynamoDB getClient() {
        return client;
    }

    protected Resource updateResourceOwner(UserInstance newOwner, Resource existingResource) {
        return existingResource
            .copy()
            .withPublisher(userOrganization(newOwner))
            .withOwner(newOwner.getUserIdentifier())
            .withModifiedDate(clockForTimestamps.instant())
            .build();
    }

    private Optional<TransactWriteItem> updateDoiRequest(UserInstance userinstance, Resource resource) {
        Optional<DoiRequest> existingDoiRequest = attempt(() -> fetchExistingDoiRequest(userinstance, resource))
            .orElse(this::doiRequestNotFound);

        return
            existingDoiRequest.map(doiRequest -> doiRequest.update(resource))
                .map(DoiRequestDao::new)
                .map(ResourceServiceUtils::toDynamoFormat)
                .map(dynamoEntry -> new Put().withTableName(tableName).withItem(dynamoEntry))
                .map(put -> new TransactWriteItem().withPut(put));
    }

    private Optional<DoiRequest> doiRequestNotFound(Failure<Optional<DoiRequest>> fail) {
        if (fail.getException() instanceof NotFoundException) {
            return Optional.empty();
        }
        throw new RuntimeException(fail.getException());
    }

    private Optional<DoiRequest> fetchExistingDoiRequest(UserInstance userinstance, Resource resource)
        throws NotFoundException {
        return Optional.of(DoiRequestService.getDoiRequestByResourceIdentifier(userinstance,
            resource.getIdentifier(), tableName, client));
    }

    private TransactWriteItem updateResource(Resource resourceUpdate) {

        ResourceDao resourceDao = new ResourceDao(resourceUpdate);

        Map<String, AttributeValue> primaryKeyConditionAttributeValues =
            primaryKeyEqualityConditionAttributeValues(resourceDao);

        Put put = new Put()
            .withItem(toDynamoFormat(resourceDao))
            .withTableName(tableName)
            .withConditionExpression(PRIMARY_KEY_EQUALITY_CHECK_EXPRESSION)
            .withExpressionAttributeNames(PRIMARY_KEY_EQUALITY_CONDITION_ATTRIBUTE_NAMES)
            .withExpressionAttributeValues(primaryKeyConditionAttributeValues);

        return new TransactWriteItem().withPut(put);
    }
}
