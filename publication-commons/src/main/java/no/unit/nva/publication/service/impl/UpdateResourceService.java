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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.publication.exception.InvalidPublicationException;
import no.unit.nva.publication.exception.TransactionFailedException;
import no.unit.nva.publication.model.DeletePublicationStatusResponse;
import no.unit.nva.publication.model.PublishPublicationStatusResponse;
import no.unit.nva.publication.model.business.Entity;
import no.unit.nva.publication.model.business.Owner;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.model.storage.Dao;
import no.unit.nva.publication.model.storage.DynamoEntry;
import no.unit.nva.publication.model.storage.ResourceDao;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.NotFoundException;

public class UpdateResourceService extends ServiceWithTransactions {
    
    
    public static final String PUBLISH_COMPLETED = "Publication is published.";

    public static final String PUBLISH_IN_PROGRESS = "Publication is being published. This may take a while.";

    //TODO: fix affiliation update when updating owner
    private static final URI AFFILIATION_UPDATE_NOT_UPDATE_YET = null;
    public static final String RESOURCE_ALREADY_DELETED = "Resource already deleted";
    public static final String DELETION_IN_PROGRESS = "Deletion in progress. This may take a while";

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
        var transactionItems = new ArrayList<TransactWriteItem>();
        transactionItems.add(updateResourceTransactionItem);
        transactionItems.addAll(updateTicketsTransactionItems);
        
        var request = new TransactWriteItemsRequest().withTransactItems(transactionItems);
        sendTransactionWriteRequest(request);
        
        return publicationUpdate;
    }

    public void updateOwner(SortableIdentifier identifier, UserInstance oldOwner, UserInstance newOwner)
        throws NotFoundException {
        Resource existingResource = readResourceService.getResource(oldOwner, identifier);
        Resource newResource = updateResourceOwner(newOwner, existingResource);
        TransactWriteItem deleteAction = newDeleteTransactionItem(new ResourceDao(existingResource));
        TransactWriteItem insertionAction = newPutTransactionItem(new ResourceDao(newResource));
        TransactWriteItemsRequest request = newTransactWriteItemsRequest(deleteAction, insertionAction);
        sendTransactionWriteRequest(request);
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

    protected static DeletePublicationStatusResponse deletionStatusIsCompleted() {
        return new DeletePublicationStatusResponse(RESOURCE_ALREADY_DELETED,
                                                   HttpURLConnection.HTTP_NO_CONTENT);
    }

    protected static DeletePublicationStatusResponse deletionStatusChangeInProgress() {
        return new DeletePublicationStatusResponse(DELETION_IN_PROGRESS,
                                                   HttpURLConnection.HTTP_ACCEPTED);
    }

    private static boolean publicationIsPublished(Publication publication) {
        var status = publication.getStatus();
        return PublicationStatus.PUBLISHED.equals(status);
    }

    /**
     * Associated artifacts are NOT updated anymore. For now all files are just files,
     * i.e. we do not use Published/Unpublished temporary.
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
                       new ResourceOwner(newOwner.getUsername(), AFFILIATION_UPDATE_NOT_UPDATE_YET)))
                   .withModifiedDate(clockForTimestamps.instant())
                   .build();
    }
    
    private List<TransactWriteItem> updateTickets(Resource resource) {
        var dao = new ResourceDao(resource);
        var ticketDaos = dao.fetchAllTickets(getClient());
        return ticketDaos.stream()
                   .map(Dao::getData)
                   .map(TicketEntry.class::cast)
                   .map(ticket -> ticket.update(resource))
                   .map(Entity::toDao)
                   .map(DynamoEntry::toDynamoFormat)
                   .map(dynamoEntry -> new Put().withTableName(RESOURCES_TABLE_NAME).withItem(dynamoEntry))
                   .map(put -> new TransactWriteItem().withPut(put))
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
