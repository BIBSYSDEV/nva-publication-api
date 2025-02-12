package no.unit.nva.publication.service.impl;

import static java.util.Objects.nonNull;
import static no.unit.nva.model.PublicationStatus.DELETED;
import static no.unit.nva.model.PublicationStatus.DRAFT;
import static no.unit.nva.model.PublicationStatus.PUBLISHED_METADATA;
import static no.unit.nva.model.PublicationStatus.UNPUBLISHED;
import static no.unit.nva.publication.model.business.PublishingRequestCase.assertThatPublicationHasMinimumMandatoryFields;
import static no.unit.nva.publication.service.impl.ReadResourceService.RESOURCE_NOT_FOUND_MESSAGE;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.Username;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.publication.exception.InvalidPublicationException;
import no.unit.nva.publication.exception.TransactionFailedException;
import no.unit.nva.publication.exception.UnsupportedPublicationStatusTransition;
import no.unit.nva.publication.external.services.RawContentRetriever;
import no.unit.nva.publication.model.DeletePublicationStatusResponse;
import no.unit.nva.publication.model.PublishPublicationStatusResponse;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.Entity;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.Owner;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.TicketStatus;
import no.unit.nva.publication.model.business.UnpublishRequest;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.model.business.importcandidate.CandidateStatus;
import no.unit.nva.publication.model.business.importcandidate.ImportCandidate;
import no.unit.nva.publication.model.business.importcandidate.ImportStatus;
import no.unit.nva.publication.model.business.publicationstate.PublishedResourceEvent;
import no.unit.nva.publication.model.business.publicationstate.UnpublishedResourceEvent;
import no.unit.nva.publication.model.storage.Dao;
import no.unit.nva.publication.model.storage.DynamoEntry;
import no.unit.nva.publication.model.storage.FileDao;
import no.unit.nva.publication.model.storage.ResourceDao;
import no.unit.nva.publication.model.storage.TicketDao;
import no.unit.nva.publication.model.storage.UnpublishRequestDao;
import no.unit.nva.publication.model.utils.CuratingInstitutionsUtil;
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
    public static final String ILLEGAL_DELETE_WHEN_NOT_DRAFT =
        "Attempting to update publication to DRAFT_FOR_DELETION when current status "
        + "is not draft";
    private final String tableName;
    private final Clock clockForTimestamps;
    private final ReadResourceService readResourceService;
    private static final List<PublicationStatus> allowedPublicationStatusesForPublishing = List.of(
        DRAFT,
        PUBLISHED_METADATA,
        UNPUBLISHED
    );
    private final RawContentRetriever uriRetriever;

    public UpdateResourceService(AmazonDynamoDB client,
                                 String tableName,
                                 Clock clockForTimestamps,
                                 ReadResourceService readResourceService,
                                 RawContentRetriever uriRetriever) {
        super(client);
        this.tableName = tableName;
        this.clockForTimestamps = clockForTimestamps;
        this.readResourceService = readResourceService;
        this.uriRetriever = uriRetriever;
    }

    public Publication updatePublicationButDoNotChangeStatus(Publication publication) {
        var originalPublication = fetchExistingResource(publication).toPublication();
        if (originalPublication.getStatus().equals(publication.getStatus())) {
            return updatePublicationIncludingStatus(publication);
        }
        throw new IllegalStateException("Attempting to update publication status when it is not allowed");
    }

    public Publication updatePublicationDraftToDraftForDeletion(Publication publication)
        throws NotFoundException {
        var persistedPublication = attempt(() -> fetchExistingResource(publication))
                                       .map(Resource::toPublication)
                                       .orElseThrow(failure -> new NotFoundException(RESOURCE_NOT_FOUND_MESSAGE));
        if (persistedPublication.getStatus().equals(DRAFT)) {
            publication.setStatus(PublicationStatus.DRAFT_FOR_DELETION);
            publication.setModifiedDate(clockForTimestamps.instant());
            var resource = Resource.fromPublication(publication);

            var tickets = new ResourceDao(resource).fetchAllTickets(getClient()).stream()
                       .map(Dao::getData)
                       .map(TicketEntry.class::cast);

            var transactionItems = new ArrayList<TransactWriteItem>();
            transactionItems.add(createPutTransaction(resource));
            transactionItems.addAll(updateExistingPendingTicketsToNotApplicable(tickets));
            var request = newTransactWriteItemsRequest(transactionItems);
            sendTransactionWriteRequest(request);
            return updatePublicationIncludingStatus(publication);
        }
        throw new IllegalStateException(ILLEGAL_DELETE_WHEN_NOT_DRAFT);
    }

    private Publication updatePublicationIncludingStatus(Publication publicationUpdate) {
        var persistedResource = fetchExistingResource(publicationUpdate);
        publicationUpdate.setCreatedDate(persistedResource.getCreatedDate());
        publicationUpdate.setModifiedDate(clockForTimestamps.instant());

        if (isContributorsChanged(publicationUpdate, persistedResource.toPublication())) {
            publicationUpdate.setCuratingInstitutions(
                CuratingInstitutionsUtil.getCuratingInstitutionsOnline(publicationUpdate, uriRetriever));
        }

        var updatedFileEntriesTransactionWriteItems = persistedResource.getFileEntries().stream()
                                     .map(fileEntry -> updateFileEntry(fileEntry, publicationUpdate))
                                     .map(FileEntry::toDao)
                                     .map(dao -> dao.toPutTransactionItem(tableName))
                                     .toList();

        var resource = Resource.fromPublication(publicationUpdate);

        var updateResourceTransactionItem = createPutTransaction(resource);
        var updateTicketsTransactionItems = updateTickets(resource);

        var transactionItems = new ArrayList<TransactWriteItem>();
        transactionItems.add(updateResourceTransactionItem);
        transactionItems.addAll(updateTicketsTransactionItems);
        transactionItems.addAll(updatedFileEntriesTransactionWriteItems);

        var request = new TransactWriteItemsRequest().withTransactItems(transactionItems);
        sendTransactionWriteRequest(request);

        return publicationUpdate;
    }

    private static FileEntry updateFileEntry(FileEntry fileEntry, Publication publication) {
        return publication.getFile(fileEntry.getFile().getIdentifier())
                   .map(fileEntry::update)
                   .orElse(fileEntry);
    }

    private static boolean isContributorsChanged(Publication publicationUpdate, Publication persistedPublication) {
        return nonNull(publicationUpdate.getEntityDescription())
               && !getContributors(publicationUpdate).equals(getContributors(persistedPublication));
    }

    private static List<Contributor> getContributors(Publication persistedPublication) {
        return nonNull(persistedPublication.getEntityDescription())
                   ? persistedPublication.getEntityDescription().getContributors()
                   : List.of();
    }

    public void updateOwner(SortableIdentifier identifier, UserInstance oldOwner, UserInstance newOwner)
        throws NotFoundException {
        Resource existingResource = readResourceService.getResource(oldOwner, identifier);
        Resource newResource = updateResourceOwner(newOwner, existingResource);
        TransactWriteItem deleteAction = newDeleteTransactionItem(new ResourceDao(existingResource));
        TransactWriteItem insertionAction = newPutTransactionItem(new ResourceDao(newResource), tableName);
        TransactWriteItemsRequest request = newTransactWriteItemsRequest(deleteAction, insertionAction);
        sendTransactionWriteRequest(request);
    }

    public void updateResource(Resource resource) {
        TransactWriteItem insertionAction = createPutTransaction(resource);
        TransactWriteItemsRequest request = newTransactWriteItemsRequest(insertionAction);
        sendTransactionWriteRequest(request);
    }

    public ImportCandidate updateImportCandidate(ImportCandidate importCandidate) throws BadRequestException {
        var existingImportCandidate = fetchImportCandidate(importCandidate);
        if (isNotImported(existingImportCandidate)) {
            importCandidate.setCreatedDate(existingImportCandidate.getCreatedDate());
            importCandidate.setModifiedDate(clockForTimestamps.instant());

            if (isContributorsChanged(importCandidate, existingImportCandidate)) {
                importCandidate.setCuratingInstitutions(
                    CuratingInstitutionsUtil.getCuratingInstitutionsOnline(importCandidate, uriRetriever));
            }

            var existingFileEntries = Resource.fromImportCandidate(existingImportCandidate).getFileEntries().stream()
                                                           .map(FileEntry::toDao)
                                                           .collect(Collectors.toMap(FileDao::getIdentifier, dao -> dao));

            // Collect entries from importCandidate into a map
            var newFileEntries = Resource.fromImportCandidate(importCandidate).getFileEntries().stream()
                                                      .map(FileEntry::toDao)
                                                      .collect(Collectors.toMap(FileDao::getIdentifier, dao -> dao));

            // Merge the entries, giving priority to entries from importCandidate
            var mergedEntries = new HashMap<>(existingFileEntries);
            mergedEntries.putAll(newFileEntries); // This will overwrite any existing entries with those from importCandidate

            // Create lists for TransactWriteItem
            List<TransactWriteItem> transactions = new ArrayList<>();

            // Add the resource put transaction
            Resource resource = Resource.fromImportCandidate(importCandidate);
            transactions.add(createPutTransaction(resource));

            // Create TransactWriteItems from the merged entries
            for (FileDao fileDao : mergedEntries.values()) {
                if (newFileEntries.containsKey(fileDao.getIdentifier()) && ) {
                    // Add put transaction for entries from importCandidate
                    transactions.add(fileDao.toPutTransactionItem(tableName));
                } else if (existingFileEntries.containsKey(fileDao.getIdentifier())) {
                    // Add delete transaction for entries only in existingImportCandidate
                    transactions.add(fileDao.toDeleteTransactionItem(tableName));
                }
            }

            var request = new TransactWriteItemsRequest().withTransactItems(transactions);
            sendTransactionWriteRequest(request);
            return importCandidate;
        }
        throw new BadRequestException("Can not update already imported candidate");
    }

    public void unpublishPublication(Publication publication,
                                     Stream<TicketEntry> existingTicketStream,
                                     UnpublishRequest unpublishRequest,
                                     UserInstance userInstance) {
        publication.setStatus(UNPUBLISHED);
        var currentTime = clockForTimestamps.instant();
        publication.setModifiedDate(currentTime);
        var resource = Resource.fromPublication(publication);
        resource.setResourceEvent(UnpublishedResourceEvent.create(userInstance, currentTime));

        var transactionItems = new ArrayList<TransactWriteItem>();
        transactionItems.add(createPutTransaction(resource));
        transactionItems.addAll(updateExistingPendingTicketsToNotApplicable(existingTicketStream));
        transactionItems.addAll(createPendingUnpublishingRequestTicket(unpublishRequest));

        var request = new TransactWriteItemsRequest().withTransactItems(transactionItems);
        sendTransactionWriteRequest(request);
    }

    private static List<TransactWriteItem> createPendingUnpublishingRequestTicket(UnpublishRequest unpublishRequest) {
        return new UnpublishRequestDao(unpublishRequest).createInsertionTransactionRequest().getTransactItems();
    }

    private List<TransactWriteItem> updateExistingPendingTicketsToNotApplicable(
        Stream<TicketEntry> existingTicketStream) {
        return existingTicketStream
                   .filter(this::isPendingTicket)
                   .map(this::updateToNotApplicable)
                   .map(Entity::toDao)
                   .map(TicketDao.class::cast)
                   .map(this::createPutTransactionItems)
                   .toList();
    }

    private boolean isPendingTicket(TicketEntry ticketEntry) {
        return TicketStatus.PENDING.equals(ticketEntry.getStatus());
    }

    private TransactWriteItem createPutTransactionItems(TicketDao ticketDao) {

        var primaryKeyConditionAttributeValues =
            primaryKeyEqualityConditionAttributeValues(ticketDao);
        var put = new Put()
                      .withItem(ticketDao.toDynamoFormat())
                      .withTableName(tableName)
                      .withConditionExpression(PRIMARY_KEY_EQUALITY_CHECK_EXPRESSION)
                      .withExpressionAttributeNames(PRIMARY_KEY_EQUALITY_CONDITION_ATTRIBUTE_NAMES)
                      .withExpressionAttributeValues(primaryKeyConditionAttributeValues);

        return new TransactWriteItem().withPut(put);
    }

    private TicketEntry updateToNotApplicable(TicketEntry ticketEntry) {
        var updatedTicket = ticketEntry.copy();
        updatedTicket.setStatus(TicketStatus.NOT_APPLICABLE);
        updatedTicket.setModifiedDate(Instant.now());
        return updatedTicket;
    }

    public void terminateResource(Resource resource, UserInstance userInstance) {
        var softDeleteFilesTransactions = createSofDeleteFilesTransactions(resource, userInstance);
        var currentTime = clockForTimestamps.instant();
        var updateResourceTransactionItem = createPutTransaction(resource.delete(userInstance, currentTime));
        var transactions = new ArrayList<TransactWriteItem>();
        transactions.add(updateResourceTransactionItem);
        transactions.addAll(softDeleteFilesTransactions);
        var request = new TransactWriteItemsRequest().withTransactItems(transactions);
        sendTransactionWriteRequest(request);
    }

    private List<TransactWriteItem> createSofDeleteFilesTransactions(Resource resource, UserInstance userInstance) {
        return resource.getFileEntries().stream()
                   .map(fileEntry -> fileEntry.softDelete(userInstance.getUser()))
                   .map(FileEntry::toDao)
                   .map(fileDao -> fileDao.toPutTransactionItem(tableName))
                   .toList();
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
        var publication = readResourceService.getResourceByIdentifier(resourceIdentifier).orElseThrow().toPublication();
        if (publicationIsPublished(publication)) {
            return publishCompletedStatus();
        } else if (publicationIsAllowedForPublishing(publication)) {
            publishPublication(publication, userInstance);
            return publishingInProgressStatus();
        } else {
            throw new UnsupportedPublicationStatusTransition(String.format(
                "Publication status %s is not al1lowed for publishing",
                publication.getStatus()
            ));
        }
    }

    /**
     * Associated artifacts are NOT updated anymore. For now all files are just files, i.e. we do not use
     * Published/Unpublished temporary.
     **/

    private void publishPublication(Publication publication, UserInstance userInstance) throws InvalidPublicationException {
        assertThatPublicationHasMinimumMandatoryFields(publication);
        var persistedResource = fetchExistingResource(publication);
        var resource = Resource.fromPublication(publication);
        resource.setStatus(PublicationStatus.PUBLISHED);
        var currentTime = clockForTimestamps.instant();
        resource.setCreatedDate(persistedResource.getCreatedDate());
        resource.setModifiedDate(currentTime);
        resource.setPublishedDate(currentTime);
        resource.setResourceEvent(PublishedResourceEvent.create(userInstance, currentTime));
        var updateResourceTransactionItem = createPutTransaction(resource);
        var updateTicketsTransactionItems = updateTickets(resource);
        var transactionItems = new ArrayList<TransactWriteItem>();
        transactionItems.add(updateResourceTransactionItem);
        transactionItems.addAll(updateTicketsTransactionItems);

        var request = new TransactWriteItemsRequest().withTransactItems(transactionItems);
        sendTransactionWriteRequest(request);
    }

    DeletePublicationStatusResponse updatePublishedStatusToDeleted(SortableIdentifier resourceIdentifier)
        throws NotFoundException {
        var publication =
            readResourceService.getResourceByIdentifier(resourceIdentifier).orElseThrow().toPublication();
        if (DELETED.equals(publication.getStatus())) {
            return deletionStatusIsCompleted();
        } else {
            publication.setStatus(DELETED);
            publication.setPublishedDate(null);
            updatePublicationIncludingStatus(publication);
            return deletionStatusChangeInProgress();
        }
    }

    public ImportCandidate updateStatus(SortableIdentifier identifier, ImportStatus status)
        throws NotFoundException {
        var importCandidate = readResourceService.getResourceByIdentifier(identifier)
                                  .orElseThrow(() -> new NotFoundException("Import candidate not found!"))
                                  .toImportCandidate();
        importCandidate.setImportStatus(status);
        importCandidate.setModifiedDate(Instant.now());
        var resource = Resource.fromImportCandidate(importCandidate);
        var updateResourceTransactionItem = createPutTransaction(resource);
        var request = new TransactWriteItemsRequest().withTransactItems(updateResourceTransactionItem);
        sendTransactionWriteRequest(request);
        return importCandidate;
    }

    private static boolean isNotImported(ImportCandidate importCandidate) {
        return !importCandidate.getImportStatus().candidateStatus().equals(CandidateStatus.IMPORTED);
    }

    private static boolean publicationIsPublished(Publication publication) {
        var status = publication.getStatus();
        return PublicationStatus.PUBLISHED.equals(status);
    }

    private ImportCandidate fetchImportCandidate(ImportCandidate importCandidate) {
        return attempt(() -> readResourceService.getResourceByIdentifier(importCandidate.getIdentifier()))
                   .map(Optional::orElseThrow)
                   .map(Resource::toImportCandidate)
                   .orElseThrow(fail -> new TransactionFailedException(fail.getException()));
    }

    private Resource fetchExistingResource(Publication publication) {
        return attempt(() -> readResourceService.getResourceByIdentifier(publication.getIdentifier()))
                   .map(Optional::orElseThrow)
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

    private TransactWriteItem createPutTransaction(Resource resourceUpdate) {


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

    private boolean publicationIsAllowedForPublishing(Publication publication) {
        return allowedPublicationStatusesForPublishing.contains(publication.getStatus());
    }
}
