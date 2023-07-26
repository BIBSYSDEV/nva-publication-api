package no.unit.nva.publication.service.impl;

import static no.unit.nva.publication.model.business.PublishingRequestCase.assertThatPublicationHasMinimumMandatoryFields;
import static no.unit.nva.publication.service.impl.ResourceServiceUtils.PRIMARY_KEY_EQUALITY_CHECK_EXPRESSION;
import static no.unit.nva.publication.service.impl.ResourceServiceUtils.PRIMARY_KEY_EQUALITY_CONDITION_ATTRIBUTE_NAMES;
import static no.unit.nva.publication.service.impl.ResourceServiceUtils.primaryKeyEqualityConditionAttributeValues;
import static no.unit.nva.publication.service.impl.ResourceServiceUtils.userOrganization;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCES_TABLE_NAME;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.Put;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsRequest;
import java.net.HttpURLConnection;
import java.net.URI;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.Username;
import no.unit.nva.publication.exception.InvalidPublicationException;
import no.unit.nva.publication.exception.TransactionFailedException;
import no.unit.nva.publication.model.DeletePublicationStatusResponse;
import no.unit.nva.publication.model.PublishPublicationStatusResponse;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.Entity;
import no.unit.nva.publication.model.business.importcandidate.CandidateStatus;
import no.unit.nva.publication.model.business.importcandidate.ImportCandidate;
import no.unit.nva.publication.model.business.Owner;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.model.business.importcandidate.ImportStatus;
import no.unit.nva.publication.model.storage.ContributionDao;
import no.unit.nva.publication.model.storage.Dao;
import no.unit.nva.publication.model.storage.DynamoEntry;
import no.unit.nva.publication.model.storage.IdentifierEntry;
import no.unit.nva.publication.model.storage.ResourceDao;
import no.unit.nva.publication.model.storage.WithPrimaryKey;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.NotFoundException;

public class UpdateResourceService extends ServiceWithTransactions {

    public static final String PUBLISH_COMPLETED = "Publication is published.";

    public static final String PUBLISH_IN_PROGRESS = "Publication is being published. This may take a while.";
    public static final String RESOURCE_ALREADY_DELETED = "Resource already deleted";
    public static final String DELETION_IN_PROGRESS = "Deletion in progress. This may take a while";
    //TODO: fix affiliation update when updating owner
    private static final URI AFFILIATION_UPDATE_NOT_UPDATE_YET = null;
    private final String tableName;
    private final Clock clockForTimestamps;
    private final ReadResourceService readResourceService;

    public UpdateResourceService(AmazonDynamoDB client,
                                 String tableName,
                                 Clock clockForTimestamps,
                                 ReadResourceService readResourceService) {
        super(client);
        this.tableName = tableName;
        this.clockForTimestamps = clockForTimestamps;
        this.readResourceService = readResourceService;
    }

    public Publication updatePublicationButDoNotChangeStatus(Publication publication) {
        var originalPublication = fetchExistingPublication(publication);
        if (originalPublication.getStatus().equals(publication.getStatus())) {
            return updatePublicationIncludingStatus(publication);
        }
        throw new IllegalStateException("Attempting to update publication status when it is not allowed");
    }

    public Publication updatePublicationIncludingStatus(Publication publicationUpdate) {
        var persistedPublication = fetchExistingPublication(publicationUpdate);
        publicationUpdate.setCreatedDate(persistedPublication.getCreatedDate());
        publicationUpdate.setModifiedDate(clockForTimestamps.instant());
        var resource = Resource.fromPublication(publicationUpdate);

        var updateResourceTransactionItem = updateResource(resource);
        var updateTicketsTransactionItems = updateTickets(resource);
        var updateContributionTransactionItems = updateContributions(resource);
        var transactionItems = new ArrayList<TransactWriteItem>();
        transactionItems.add(updateResourceTransactionItem);
        transactionItems.addAll(updateTicketsTransactionItems);
        transactionItems.addAll(updateContributionTransactionItems);

        var request = new TransactWriteItemsRequest().withTransactItems(transactionItems);
        sendTransactionWriteRequest(request);

        return publicationUpdate;
    }

    public void updateOwner(SortableIdentifier identifier, UserInstance oldOwner, UserInstance newOwner)
        throws NotFoundException {
        var existingResource = readResourceService.getResource(oldOwner, identifier);
        var existingResourceDao = new ResourceDao(existingResource);
        var newResource = updateResourceOwner(newOwner, existingResource);
        var contributions = existingResourceDao.fetchAllContributions(getClient(), tableName);
        var deleteResourceAction = newDeleteTransactionItem(existingResourceDao);
        var insertResourceAction = newPutTransactionItem(new ResourceDao(newResource), tableName);

        var newContributionDaos = contributionDaosForContributionsInsertion(newResource);

        var deleteOldContributions = deleteContributionTransactionItems(contributions);
        var newContributions = transactionItemsForContributionsInsertion(newContributionDaos);
        var newContributionIdentifiers = transactionItemsForContributionIdentifiers(newContributionDaos);

        var transactions =  new ArrayList<TransactWriteItem>();
        transactions.add(deleteResourceAction);
        transactions.add(insertResourceAction);
        transactions.addAll(deleteOldContributions);
        transactions.addAll(newContributions);
        transactions.addAll(newContributionIdentifiers);

        TransactWriteItemsRequest request = newTransactWriteItemsRequest(transactions);
        sendTransactionWriteRequest(request);
    }

    public ImportCandidate updateImportCandidate(ImportCandidate importCandidate) throws BadRequestException {
        var existingImportCandidate = fetchImportCandidate(importCandidate);
        if (isNotImported(existingImportCandidate)) {
            importCandidate.setCreatedDate(existingImportCandidate.getCreatedDate());
            importCandidate.setModifiedDate(clockForTimestamps.instant());
            var resource = Resource.fromImportCandidate(importCandidate);
            var updateResourceTransactionItem = updateResource(resource);
            var request = new TransactWriteItemsRequest().withTransactItems(List.of(updateResourceTransactionItem));
            sendTransactionWriteRequest(request);
            return importCandidate;
        }
        throw new BadRequestException("Can not update already imported candidate");
    }

    protected static DeletePublicationStatusResponse deletionStatusIsCompleted() {
        return new DeletePublicationStatusResponse(RESOURCE_ALREADY_DELETED,
                                                   HttpURLConnection.HTTP_NO_CONTENT);
    }

    protected static DeletePublicationStatusResponse deletionStatusChangeInProgress() {
        return new DeletePublicationStatusResponse(DELETION_IN_PROGRESS,
                                                   HttpURLConnection.HTTP_ACCEPTED);
    }

    PublishPublicationStatusResponse publishPublication(UserInstance userInstance,
                                                        SortableIdentifier resourceIdentifier)
        throws ApiGatewayException {
        var publication = readResourceService.getPublication(userInstance, resourceIdentifier);
        if (publicationIsPublished(publication)) {
            return publishCompletedStatus();
        } else if (publicationIsDraftOrPublishedMetadataOnly(publication)) {
            publishPublication(publication);
            return publishingInProgressStatus();
        } else {
            throw new UnsupportedOperationException("Functionality not specified");
        }
    }

    DeletePublicationStatusResponse updatePublishedStatusToDeleted(SortableIdentifier resourceIdentifier)
        throws NotFoundException {
        var publication =
            readResourceService.getResourceByIdentifier(resourceIdentifier).toPublication();
        if (PublicationStatus.DELETED.equals(publication.getStatus())) {
            return deletionStatusIsCompleted();
        } else {
            publication.setStatus(PublicationStatus.DELETED);
            publication.setPublishedDate(null);
            updatePublicationIncludingStatus(publication);
            return deletionStatusChangeInProgress();
        }
    }

    ImportCandidate updateStatus(SortableIdentifier identifier, ImportStatus status)
        throws NotFoundException {
        var importCandidate = readResourceService.getResourceByIdentifier(identifier).toImportCandidate();
        importCandidate.setImportStatus(status);
        var resource = Resource.fromImportCandidate(importCandidate);
        var updateResourceTransactionItem = updateResource(resource);
        var request = new TransactWriteItemsRequest().withTransactItems(updateResourceTransactionItem);
        sendTransactionWriteRequest(request);
        return importCandidate;
    }

    private static boolean isNotImported(ImportCandidate importCandidate) {
        return !importCandidate.getImportStatus().getCandidateStatus().equals(CandidateStatus.IMPORTED);
    }

    private static boolean publicationIsPublished(Publication publication) {
        var status = publication.getStatus();
        return PublicationStatus.PUBLISHED.equals(status);
    }

    private ImportCandidate fetchImportCandidate(ImportCandidate importCandidate) {
        return attempt(() -> readResourceService.getResourceByIdentifier(importCandidate.getIdentifier()))
                   .map(Resource::toImportCandidate)
                   .orElseThrow(fail -> new TransactionFailedException(fail.getException()));
    }

    /**
     * Associated artifacts are NOT updated anymore. For now all files are just files, i.e. we do not use
     * Published/Unpublished temporary.
     **/

    private void publishPublication(Publication publication) throws InvalidPublicationException {
        assertThatPublicationHasMinimumMandatoryFields(publication);
        publication.setStatus(PublicationStatus.PUBLISHED);
        publication.setPublishedDate(clockForTimestamps.instant());
        updatePublicationIncludingStatus(publication);
    }

    private boolean publicationIsDraftOrPublishedMetadataOnly(Publication publication) {
        var status = publication.getStatus();
        return PublicationStatus.DRAFT.equals(status)
               || PublicationStatus.PUBLISHED_METADATA.equals(status);
    }

    private Publication fetchExistingPublication(Publication publication) {
        return attempt(() -> readResourceService.getPublication(publication))
                   .orElseThrow(fail -> new TransactionFailedException(fail.getException()));
    }

    private Resource updateResourceOwner(UserInstance newOwner, Resource existingResource) {
        return existingResource
                   .copy()
                   .withPublisher(userOrganization(newOwner))
                   .withResourceOwner(Owner.fromResourceOwner(
                       new ResourceOwner(new Username(newOwner.getUsername()), AFFILIATION_UPDATE_NOT_UPDATE_YET)))
                   .withModifiedDate(clockForTimestamps.instant())
                   .build();
    }

    private List<TransactWriteItem> updateTickets(Resource resource) {
        var dao = new ResourceDao(resource);
        var ticketDaos = dao.fetchAllTickets(getClient());
        return ticketDaos.stream()
                   .map(Dao::getData)
                   .map(TicketEntry.class::cast)
                   .filter(ticketEntry -> ticketEntry instanceof DoiRequest)
                   .map(DoiRequest.class::cast)
                   .map(ticket -> ticket.update(resource))
                   .map(Entity::toDao)
                   .map(DynamoEntry::toDynamoFormat)
                   .map(dynamoEntry -> new Put().withTableName(RESOURCES_TABLE_NAME).withItem(dynamoEntry))
                   .map(put -> new TransactWriteItem().withPut(put))
                   .collect(Collectors.toList());
    }

    private List<TransactWriteItem> updateContributions(Resource resource) {
        var dao = new ResourceDao(resource);
        var exisitingContributionDao = dao.fetchAllContributions(getClient(), tableName);
        var newContributionDaos = contributionDaosForContributionsInsertion(resource);

        var delete = deleteContributionTransactionItems(exisitingContributionDao);
        var contributionTransactions = transactionItemsForContributionsInsertion(newContributionDaos);
        var contributionIdentifierTransactions = transactionItemsForContributionIdentifiers(newContributionDaos);

        var transactions =  new ArrayList<TransactWriteItem>();
        transactions.addAll(delete);
        transactions.addAll(contributionTransactions);
        transactions.addAll(contributionIdentifierTransactions);
        return transactions;
    }

    private List<TransactWriteItem> transactionItemsForContributionsInsertion(List<ContributionDao> daos) {
        return daos
                   .stream()
                   .map(contributionDao -> newPutTransactionItem(contributionDao, tableName))
                   .collect(Collectors.toList());
    }

    private List<ContributionDao> contributionDaosForContributionsInsertion(Resource resource) {
        return Optional.ofNullable(resource.getEntityDescription())
                   .orElseGet(EntityDescription::new)
                   .getContributors()
                   .stream()
                   .map(contributor -> new ContributionDao(resource, contributor))
                   .collect(Collectors.toList());
    }

    private List<TransactWriteItem> transactionItemsForContributionIdentifiers(List<ContributionDao> daos) {
        return daos.stream()
                   .map(Dao::getIdentifier)
                   .map(Objects::toString)
                   .map(IdentifierEntry::new)
                   .map(identity -> newPutTransactionItem(identity, tableName))
                   .collect(Collectors.toList());
    }

    private List<TransactWriteItem> deleteContributionTransactionItems(List<ContributionDao> daos) {
        if (!daos.isEmpty()) {
            return deleteContributionsTransactionItems(daos);
        }
        return Collections.emptyList();
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
                .map(this::newDeleteTransactionItem)
                .collect(Collectors.toList());
    }


    private TransactWriteItem updateResource(Resource resourceUpdate) {

        ResourceDao resourceDao = new ResourceDao(resourceUpdate);

        Map<String, AttributeValue> primaryKeyConditionAttributeValues =
            primaryKeyEqualityConditionAttributeValues(resourceDao);

        Put put = new Put()
                      .withItem(resourceDao.toDynamoFormat())
                      .withTableName(tableName)
                      .withConditionExpression(PRIMARY_KEY_EQUALITY_CHECK_EXPRESSION)
                      .withExpressionAttributeNames(PRIMARY_KEY_EQUALITY_CONDITION_ATTRIBUTE_NAMES)
                      .withExpressionAttributeValues(primaryKeyConditionAttributeValues);

        return new TransactWriteItem().withPut(put);
    }

    private PublishPublicationStatusResponse publishingInProgressStatus() {
        return new PublishPublicationStatusResponse(PUBLISH_IN_PROGRESS, HttpURLConnection.HTTP_ACCEPTED);
    }

    private PublishPublicationStatusResponse publishCompletedStatus() {
        return new PublishPublicationStatusResponse(PUBLISH_COMPLETED, HttpURLConnection.HTTP_NO_CONTENT);
    }
}
