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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeleteResourceService extends ServiceWithTransactions {

    private static final Logger logger = LoggerFactory.getLogger(DeleteResourceService.class);
    public static final String CAN_NOT_DELETE_IMPORT_CANDIDATE_MESSAGE = "Can not delete already imported resource!";
    public static final String IMPORT_CANDIDATE_DELETION_MESSAGE = "Import candidate with id ‰s has been permanently "
                                                                   + "deleted {}";
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
            logger.info(IMPORT_CANDIDATE_DELETION_MESSAGE, importCandidate.getIdentifier());
        }
    }

    private TransactWriteItem deleteResource(Resource resource) {
        var delete = new Delete()
                         .withKey(resource.toDao().primaryKey())
                         .withTableName(tableName);

        return new TransactWriteItem().withDelete(delete);
    }
}
