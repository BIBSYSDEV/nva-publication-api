package no.unit.nva.publication.service.impl;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.Delete;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsRequest;
import no.unit.nva.publication.model.business.importcandidate.CandidateStatus;
import no.unit.nva.publication.model.business.importcandidate.ImportCandidate;
import no.unit.nva.publication.model.business.Resource;
import nva.commons.apigateway.exceptions.BadMethodException;
import nva.commons.apigateway.exceptions.NotFoundException;

public class DeleteResourceService extends ServiceWithTransactions {

    public static final String CAN_NOT_DELETE_IMPORT_CANDIDATE_MESSAGE = "Can not delete already imported resource!";
    private final String tableName;
    private final ReadResourceService readResourceService;

    public DeleteResourceService(AmazonDynamoDB client,
                                 String tableName,
                                 ReadResourceService readResourceService) {
        super(client);
        this.tableName = tableName;
        this.readResourceService = readResourceService;
    }

    public void deleteImportCandidate(ImportCandidate candidate) throws NotFoundException, BadMethodException {
        var importCandidate = readResourceService.getResourceByIdentifier(candidate.getIdentifier())
                                  .toImportCandidate();
        if (CandidateStatus.IMPORTED.equals( importCandidate.getImportStatus().getCandidateStatus())) {
            throw new BadMethodException(CAN_NOT_DELETE_IMPORT_CANDIDATE_MESSAGE);
        } else {
            var transactionWriteItem = deleteResource(Resource.fromImportCandidate(importCandidate));
            var request = new TransactWriteItemsRequest().withTransactItems(transactionWriteItem);
            sendTransactionWriteRequest(request);
        }
    }

    private TransactWriteItem deleteResource(Resource resource) {
        var delete = new Delete()
                         .withKey(resource.toDao().primaryKey())
                         .withTableName(tableName);

        return new TransactWriteItem().withDelete(delete);
    }
}
