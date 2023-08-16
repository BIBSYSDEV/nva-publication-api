package no.unit.nva.publication.service.impl;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.Delete;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.importcandidate.CandidateStatus;
import no.unit.nva.publication.model.business.importcandidate.ImportCandidate;
import no.unit.nva.publication.model.storage.ContributionDao;
import no.unit.nva.publication.model.storage.IdentifierEntry;
import no.unit.nva.publication.model.storage.WithPrimaryKey;
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

    public void deleteImportCandidate(ImportCandidate candidate, List<ContributionDao> contributions) throws NotFoundException, BadMethodException {
        var importCandidate = readResourceService.getResourceByIdentifier(candidate.getIdentifier())
                                  .toImportCandidate();
        if (CandidateStatus.IMPORTED.equals( importCandidate.getImportStatus().getCandidateStatus())) {
            throw new BadMethodException(CAN_NOT_DELETE_IMPORT_CANDIDATE_MESSAGE);
        } else {
            var deleteResourceWriteItem = deleteResource(Resource.fromImportCandidate(importCandidate).toDao());
            var deleteContributionWriteItems = deleteContributionsTransactionItems(contributions);

            var transactionItems = new ArrayList<TransactWriteItem>();
            transactionItems.add(deleteResourceWriteItem);
            transactionItems.addAll(deleteContributionWriteItems);
            var request = new TransactWriteItemsRequest().withTransactItems(transactionItems);
            sendTransactionWriteRequest(request);
        }
    }

    private TransactWriteItem deleteResource(WithPrimaryKey entry) {
        var delete = new Delete()
                         .withKey(entry.primaryKey())
                         .withTableName(tableName);

        return new TransactWriteItem().withDelete(delete);
    }

    private List<TransactWriteItem> deleteContributionsTransactionItems(List<ContributionDao> contributionDaos) {
        return contributionDaos.stream()
                   .map(this::deleteContributionsTransactionItem)
                   .flatMap(Collection::stream).collect(Collectors.toList());
    }

    private List<TransactWriteItem> deleteContributionsTransactionItem(ContributionDao contributionDao) {
        WithPrimaryKey identifierEntry = IdentifierEntry.create(contributionDao);
        return
            Stream.of(contributionDao, identifierEntry)
                .map(this::deleteResource)
                .collect(Collectors.toList());
    }
}
