package no.unit.nva.publication.service.impl;

import static no.unit.nva.publication.storage.model.DatabaseConstants.IMPORT_CANDIDATE_KEY_PATTERN;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_SORT_KEY_NAME;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.DeleteItemRequest;
import java.util.Map;
import no.unit.nva.importcandidate.CandidateStatus;
import no.unit.nva.importcandidate.ImportCandidate;
import nva.commons.apigateway.exceptions.BadMethodException;

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

    public void deleteImportCandidate(ImportCandidate candidate) throws BadMethodException {
        var importCandidate = readResourceService.getImportCandidateByIdentifier(candidate.getIdentifier()).orElseThrow();
        if (CandidateStatus.IMPORTED.equals(importCandidate.getImportStatus().candidateStatus())) {
            throw new BadMethodException(CAN_NOT_DELETE_IMPORT_CANDIDATE_MESSAGE);
        } else {
            var primaryKey = getAttributeValue(importCandidate);
            client.deleteItem(new DeleteItemRequest(tableName, Map.of(PRIMARY_KEY_PARTITION_KEY_NAME, primaryKey,
                                                                      PRIMARY_KEY_SORT_KEY_NAME, primaryKey)));
        }
    }

    private static AttributeValue getAttributeValue(ImportCandidate candidate) {
        return new AttributeValue(IMPORT_CANDIDATE_KEY_PATTERN.formatted(candidate.getIdentifier()));
    }
}
