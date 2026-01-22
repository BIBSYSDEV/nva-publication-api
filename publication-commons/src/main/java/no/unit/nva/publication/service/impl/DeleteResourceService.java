package no.unit.nva.publication.service.impl;

import static no.unit.nva.publication.storage.model.DatabaseConstants.IMPORT_CANDIDATE_KEY_PATTERN;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_SORT_KEY_NAME;
import java.util.Map;
import no.unit.nva.importcandidate.CandidateStatus;
import no.unit.nva.importcandidate.ImportCandidate;
import nva.commons.apigateway.exceptions.BadMethodException;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;

public class DeleteResourceService extends ServiceWithTransactions {

    public static final String CAN_NOT_DELETE_IMPORT_CANDIDATE_MESSAGE = "Can not delete already imported resource!";
    private final String tableName;
    private final ReadResourceService readResourceService;

    public DeleteResourceService(DynamoDbClient client,
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
            var deleteRequest = DeleteItemRequest.builder()
                                    .tableName(tableName)
                                    .key(Map.of(PRIMARY_KEY_PARTITION_KEY_NAME, primaryKey,
                                                PRIMARY_KEY_SORT_KEY_NAME, primaryKey))
                                    .build();
            client.deleteItem(deleteRequest);
        }
    }

    private static AttributeValue getAttributeValue(ImportCandidate candidate) {
        return AttributeValue.builder().s(IMPORT_CANDIDATE_KEY_PATTERN.formatted(candidate.getIdentifier())).build();
    }
}
