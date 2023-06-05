package no.unit.nva.publication.service.impl;

import static no.unit.nva.publication.service.impl.ResourceServiceUtils.PRIMARY_KEY_EQUALITY_CHECK_EXPRESSION;
import static no.unit.nva.publication.service.impl.ResourceServiceUtils.PRIMARY_KEY_EQUALITY_CONDITION_ATTRIBUTE_NAMES;
import static no.unit.nva.publication.service.impl.ResourceServiceUtils.primaryKeyEqualityConditionAttributeValues;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.Delete;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsRequest;
import java.time.Clock;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.business.ImportStatus;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.storage.ResourceDao;
import nva.commons.apigateway.exceptions.BadMethodException;
import nva.commons.apigateway.exceptions.NotFoundException;

public class DeleteResourceService extends ServiceWithTransactions {

    public static final String CAN_NOT_DELETE_IMPORT_CANDIDATE_MESSAGE = "Can not delete already imported resource!";
    private final AmazonDynamoDB client;
    private final String tableName;
    private final Clock clockForTimestamps;
    private final ReadResourceService readResourceService;

    public DeleteResourceService(AmazonDynamoDB client,
                                 String tableName,
                                 Clock clockForTimestamps,
                                 ReadResourceService readResourceService) {
        super(client);
        this.client = client;
        this.tableName = tableName;
        this.clockForTimestamps = clockForTimestamps;

        this.readResourceService = readResourceService;
    }

    public void deleteImportCandidate(SortableIdentifier identifier) throws NotFoundException, BadMethodException {
        var importCandidate = readResourceService.getResourceByIdentifier(identifier).toImportCandidate();
        if (ImportStatus.IMPORTED.equals(importCandidate.getImportStatus())) {
            throw new BadMethodException(CAN_NOT_DELETE_IMPORT_CANDIDATE_MESSAGE);
        } else {
            var transactionWriteItem = deleteResource(Resource.fromImportCandidate(importCandidate));
            var request = new TransactWriteItemsRequest().withTransactItems(transactionWriteItem);
            sendTransactionWriteRequest(request);
        }
    }

    private TransactWriteItem deleteResource(Resource resource) {
        var resourceDao = new ResourceDao(resource);

        var primaryKeyConditionAttributeValues =
            primaryKeyEqualityConditionAttributeValues(resourceDao);

        var delete = new Delete()
                         .withKey(primaryKeyConditionAttributeValues)
                         .withTableName(tableName)
                         .withConditionExpression(PRIMARY_KEY_EQUALITY_CHECK_EXPRESSION)
                         .withExpressionAttributeNames(PRIMARY_KEY_EQUALITY_CONDITION_ATTRIBUTE_NAMES)
                         .withExpressionAttributeValues(primaryKeyConditionAttributeValues);

        return new TransactWriteItem().withDelete(delete);
    }
}
