package no.unit.nva.publication.service.impl;

import static no.unit.nva.publication.storage.model.DatabaseConstants.IMPORT_CANDIDATE_KEY_PATTERN;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.DeleteItemRequest;
import java.util.Map;
import no.unit.nva.publication.model.business.importcandidate.CandidateStatus;
import no.unit.nva.publication.model.business.importcandidate.ImportCandidate;
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
        var importCandidate = readResourceService.getImportCandidateByIdentifier(candidate.getIdentifier());
        if (importCandidate.isPresent() && CandidateStatus.IMPORTED.equals(importCandidate.get().getImportStatus().candidateStatus())) {
            throw new BadMethodException(CAN_NOT_DELETE_IMPORT_CANDIDATE_MESSAGE);
        } else {
            client.deleteItem(new DeleteItemRequest(tableName, Map.of("PK0", getAttributeValue(candidate),
                                                                      "SK0", getAttributeValue(candidate))));
        }
    }

    private static AttributeValue getAttributeValue(ImportCandidate candidate) {
        return new AttributeValue(IMPORT_CANDIDATE_KEY_PATTERN.formatted(candidate.getIdentifier()));
    }
}
