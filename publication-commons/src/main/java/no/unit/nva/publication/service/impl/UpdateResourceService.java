package no.unit.nva.publication.service.impl;

import static java.util.Objects.nonNull;
import static no.unit.nva.model.PublicationStatus.DELETED;
import static no.unit.nva.model.PublicationStatus.DRAFT;
import static no.unit.nva.model.PublicationStatus.UNPUBLISHED;
import static no.unit.nva.publication.model.business.publicationchannel.PublicationChannelUtil.createPublicationChannelDao;
import static no.unit.nva.publication.model.business.publicationchannel.PublicationChannelUtil.getPublisherIdentifierWhenDegree;
import static no.unit.nva.publication.service.impl.ReadResourceService.RESOURCE_NOT_FOUND_MESSAGE;
import static no.unit.nva.publication.service.impl.ResourceServiceUtils.PRIMARY_KEY_EQUALITY_CHECK_EXPRESSION;
import static no.unit.nva.publication.service.impl.ResourceServiceUtils.PRIMARY_KEY_EQUALITY_CONDITION_ATTRIBUTE_NAMES;
import static no.unit.nva.publication.service.impl.ResourceServiceUtils.primaryKeyEqualityConditionAttributeValues;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.Put;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsRequest;
import java.net.HttpURLConnection;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import no.unit.nva.auth.uriretriever.RawContentRetriever;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.importcandidate.CandidateStatus;
import no.unit.nva.importcandidate.ImportCandidate;
import no.unit.nva.importcandidate.ImportStatus;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.ImportSource;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.exception.TransactionFailedException;
import no.unit.nva.publication.external.services.ChannelClaimClient;
import no.unit.nva.publication.model.DeletePublicationStatusResponse;
import no.unit.nva.publication.model.business.Entity;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.TicketStatus;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.model.business.publicationstate.UnpublishedResourceEvent;
import no.unit.nva.publication.model.storage.Dao;
import no.unit.nva.publication.model.storage.ResourceDao;
import no.unit.nva.publication.model.storage.TicketDao;
import no.unit.nva.publication.model.storage.importcandidate.ImportCandidateDao;
import no.unit.nva.publication.model.utils.CuratingInstitutionsUtil;
import no.unit.nva.publication.model.utils.CustomerService;
import no.unit.nva.publication.utils.CristinUnitsUtil;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.NotFoundException;

@SuppressWarnings({"PMD.GodClass", "PMD.CouplingBetweenObjects"})
public class UpdateResourceService extends ServiceWithTransactions {

    public static final String RESOURCE_ALREADY_DELETED = "Resource already deleted";
    public static final String DELETION_IN_PROGRESS = "Deletion in progress. This may take a while";
    public static final String ILLEGAL_DELETE_WHEN_NOT_DRAFT =
        "Attempting to update publication to DRAFT_FOR_DELETION when current status " + "is not draft";
    private final String tableName;
    private final Clock clockForTimestamps;
    private final ReadResourceService readResourceService;
    private final RawContentRetriever uriRetriever;
    private final CustomerService customerService;
    private final ChannelClaimClient channelClaimClient;
    private final CristinUnitsUtil cristinUnitsUtil;

    public UpdateResourceService(AmazonDynamoDB client, String tableName, Clock clockForTimestamps,
                                 ReadResourceService readResourceService, RawContentRetriever uriRetriever,
                                 ChannelClaimClient channelClaimClient,
                                 CustomerService customerService,
                                 CristinUnitsUtil cristinUnitsUtil) {
        super(client);
        this.tableName = tableName;
        this.clockForTimestamps = clockForTimestamps;
        this.readResourceService = readResourceService;
        this.uriRetriever = uriRetriever;
        this.channelClaimClient = channelClaimClient;
        this.customerService = customerService;
        this.cristinUnitsUtil = cristinUnitsUtil;
    }

    public Publication updatePublicationButDoNotChangeStatus(Publication publication) {
        var originalPublication = fetchExistingResource(Resource.fromPublication(publication)).toPublication();
        if (originalPublication.getStatus().equals(publication.getStatus())) {
            return updatePublicationIncludingStatus(publication);
        }
        throw new IllegalStateException("Attempting to update publication status when it is not allowed");
    }

    public Publication updatePublicationDraftToDraftForDeletion(Publication publication) throws NotFoundException {
        var persistedPublication =
            attempt(() -> fetchExistingResource(Resource.fromPublication(publication))).map(Resource::toPublication)
                                       .orElseThrow(failure -> new NotFoundException(RESOURCE_NOT_FOUND_MESSAGE));
        if (persistedPublication.getStatus().equals(DRAFT)) {
            publication.setStatus(PublicationStatus.DRAFT_FOR_DELETION);
            publication.setModifiedDate(clockForTimestamps.instant());
            var resource = Resource.fromPublication(publication);

            var tickets = new ResourceDao(resource).fetchAllTickets(getClient())
                              .stream()
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

    public List<TransactWriteItem> updatePublicationChannelsForPublisherWhenDegree(Resource resource,
                                                                                   Resource persistedResource) {
        var transactWriteItems = new ArrayList<TransactWriteItem>();

        var oldPublisherIdentifier = getPublisherIdentifierWhenDegree(persistedResource);
        var newPublisherIdentifier = getPublisherIdentifierWhenDegree(resource);

        if (!Objects.equals(oldPublisherIdentifier, newPublisherIdentifier)) {
            oldPublisherIdentifier.ifPresent(
                identifier -> removePublicationChannel(persistedResource, identifier, transactWriteItems));
            newPublisherIdentifier.ifPresent(identifier -> addPublicationChannel(resource, transactWriteItems));
        }

        return transactWriteItems;
    }

    public Resource updateResourceFromImport(Resource resource, UserInstance userInstance, ImportSource importSource) {
        var persistedResource = fetchExistingResource(resource);

        if (resource.hasEffectiveChanges(persistedResource)) {
            resource.setCreatedDate(persistedResource.getCreatedDate());
            resource.setModifiedDate(clockForTimestamps.instant());

            updateCuratingInstitutions(resource, persistedResource);

            var transactionItems = createTransactions(resource, userInstance, persistedResource, importSource);

            var transactWriteItemsRequest = new TransactWriteItemsRequest()
                                                .withTransactItems(transactionItems);

            sendTransactionWriteRequest(transactWriteItemsRequest);
            return resource;
        }
        return resource;
    }

    private ArrayList<TransactWriteItem> createTransactions(Resource resource, UserInstance userInstance,
                                                            Resource persistedResource, ImportSource importSource) {
        var transactionItems = new ArrayList<TransactWriteItem>();

        var ticketsTransactions = refreshTicketsTransactions(resource);
        transactionItems.add(createPutTransaction(resource));
        transactionItems.addAll(updateFilesTransactions(resource, userInstance, persistedResource, importSource));
        transactionItems.addAll(ticketsTransactions);
        transactionItems.addAll(updatePublicationChannelsForPublisherWhenDegree(resource, persistedResource));
        return transactionItems;
    }

    private Collection<? extends TransactWriteItem> updateFilesTransactions(Resource resource,
                                                                            UserInstance userInstance,
                                                                            Resource persistedResource,
                                                                            ImportSource importSource) {
        return persistedResource.getFileEntries()
                   .stream()
                   .map(fileEntry -> updateFileEntry(fileEntry, resource.toPublication(), userInstance, importSource))
                   .map(FileEntry::toDao)
                   .map(dao -> dao.toPutTransactionItem(tableName))
                   .toList();
    }

    private FileEntry updateFileEntry(FileEntry fileEntry, Publication publication,
                                      UserInstance userInstance, ImportSource importSource) {
        return publication.getFile(fileEntry.getFile().getIdentifier())
                   .map(file -> nonNull(importSource)
                                    ? fileEntry.updateFromImport(file, userInstance, importSource)
                                    : fileEntry.update(file, userInstance))
                   .orElse(fileEntry);
    }

    private void removePublicationChannel(Resource persistedResource, UUID publisherIdentifier,
                                          List<TransactWriteItem> transactWriteItems) {
        persistedResource.getPublicationChannelByIdentifier(new SortableIdentifier(publisherIdentifier.toString()))
            .ifPresent(publicationChannel -> {
                TransactWriteItem deleteAction = newDeleteTransactionItem(publicationChannel.toDao());
                transactWriteItems.add(deleteAction);
            });
    }

    private void addPublicationChannel(Resource resource, List<TransactWriteItem> transactWriteItems) {
        var publisher = resource.getPublisherWhenDegree().orElseThrow();
        var publicationChannelDao = createPublicationChannelDao(channelClaimClient, resource, publisher);
        var insertionAction = newPutTransactionItem(publicationChannelDao, tableName);
        transactWriteItems.add(insertionAction);
    }

    public void refreshResource(Resource resource) {
        var transactionItems = new ArrayList<TransactWriteItem>();
        transactionItems.add(createPutTransaction(resource));
        var transactWriteItemsRequest = new TransactWriteItemsRequest().withTransactItems(transactionItems);
        sendTransactionWriteRequest(transactWriteItemsRequest);
    }

    public Resource updateResource(Resource resource, UserInstance userInstance) {
        var persistedResource = fetchExistingResource(resource);

        if (resource.hasEffectiveChanges(persistedResource)) {
            resource.setCreatedDate(persistedResource.getCreatedDate());
            resource.setModifiedDate(clockForTimestamps.instant());

            updateCuratingInstitutions(resource, persistedResource);

            var transactWriteItemsRequest = new TransactWriteItemsRequest()
                                                .withTransactItems(createTransactions(resource, userInstance,
                                                                                      persistedResource, null));

            sendTransactionWriteRequest(transactWriteItemsRequest);
            return resource;
        }
        return persistedResource;
    }

    public ImportCandidate updateImportCandidate(ImportCandidate importCandidate)
        throws BadRequestException, NotFoundException {
        var persistedImportCandidate =
            readResourceService.getImportCandidateByIdentifier(importCandidate.getIdentifier())
                .orElseThrow(() -> new NotFoundException(RESOURCE_NOT_FOUND_MESSAGE + importCandidate.getIdentifier()));
        if (isNotImported(persistedImportCandidate)) {
            importCandidate.setCreatedDate(persistedImportCandidate.getCreatedDate());
            importCandidate.setModifiedDate(clockForTimestamps.instant());
            var dao = new ImportCandidateDao(importCandidate, persistedImportCandidate.getIdentifier());
            var putItemRequest = new PutItemRequest().withTableName(tableName).withItem(dao.toDynamoFormat());
            client.putItem(putItemRequest);
            return importCandidate;
        }
        throw new BadRequestException("Can not update already imported candidate");
    }

    public void unpublishPublication(Publication publication, Stream<TicketEntry> existingTicketStream,
                                     UserInstance userInstance) {
        publication.setStatus(UNPUBLISHED);
        var currentTime = clockForTimestamps.instant();
        publication.setModifiedDate(currentTime);
        var resource = Resource.fromPublication(publication);
        resource.setResourceEvent(UnpublishedResourceEvent.create(userInstance, currentTime));

        var transactionItems = new ArrayList<TransactWriteItem>();
        transactionItems.add(createPutTransaction(resource));
        transactionItems.addAll(updateExistingPendingTicketsToNotApplicable(existingTicketStream));

        var request = new TransactWriteItemsRequest().withTransactItems(transactionItems);
        sendTransactionWriteRequest(request);
    }

    public void terminateResource(Resource resource, UserInstance userInstance) {
        var currentTime = clockForTimestamps.instant();
        var transactions = new ArrayList<TransactWriteItem>();
        transactions.add(createPutTransaction(resource.delete(userInstance, currentTime)));
        transactions.addAll(createSofDeleteFilesTransactions(resource, userInstance));
        var request = new TransactWriteItemsRequest().withTransactItems(transactions);
        sendTransactionWriteRequest(request);
    }

    public ImportCandidate updateStatus(SortableIdentifier identifier, ImportStatus status) throws NotFoundException {
        var updatedImportCandidate = createUpdatedImportCandidate(identifier, status);
        var dao = new ImportCandidateDao(updatedImportCandidate, identifier);
        var putItemRequest = new PutItemRequest().withTableName(tableName).withItem(dao.toDynamoFormat());
        client.putItem(putItemRequest);
        return updatedImportCandidate;
    }

    protected static DeletePublicationStatusResponse deletionStatusIsCompleted() {
        return new DeletePublicationStatusResponse(RESOURCE_ALREADY_DELETED, HttpURLConnection.HTTP_NO_CONTENT);
    }

    protected static DeletePublicationStatusResponse deletionStatusChangeInProgress() {
        return new DeletePublicationStatusResponse(DELETION_IN_PROGRESS, HttpURLConnection.HTTP_ACCEPTED);
    }

    DeletePublicationStatusResponse updatePublishedStatusToDeleted(SortableIdentifier resourceIdentifier) {
        var publication = readResourceService.getResourceByIdentifier(resourceIdentifier).orElseThrow().toPublication();
        return DELETED.equals(publication.getStatus()) ? deletionStatusIsCompleted() : delete(publication);
    }

    private static boolean isContributorsChanged(Resource resource, Resource existingResource) {
        return nonNull(resource.getEntityDescription())
               && !getContributors(resource).equals(getContributors(existingResource));
    }

    private static List<Contributor> getContributors(Resource resource) {
        return nonNull(resource.getEntityDescription()) ? resource.getEntityDescription().getContributors() : List.of();
    }

    private static boolean isNotImported(ImportCandidate importCandidate) {
        return !importCandidate.getImportStatus().candidateStatus().equals(CandidateStatus.IMPORTED);
    }

    private Publication updatePublicationIncludingStatus(Publication publicationUpdate) {
        var resource = Resource.fromPublication(publicationUpdate);
        var userInstance = UserInstance.fromPublication(publicationUpdate);
        return updateResource(resource, userInstance).toPublication();
    }

    private List<TransactWriteItem> refreshTicketsTransactions(Resource resource) {
        return readResourceService.fetchAllTicketsForResource(resource)
                   .map(TicketEntry::refresh)
                   .map(TicketEntry::toDao)
                   .map(ticketDao -> ticketDao.toPutTransactionItem(tableName))
                   .toList();
    }

    private void updateCuratingInstitutions(Resource resource, Resource persistedResource) {
        if (isContributorsChanged(resource, persistedResource)) {
            resource.setCuratingInstitutions(new CuratingInstitutionsUtil(uriRetriever, customerService)
                                                 .getCuratingInstitutions(
                                                     resource.toPublication().getEntityDescription(),
                                                     cristinUnitsUtil));
        }
    }

    private List<TransactWriteItem> updateExistingPendingTicketsToNotApplicable(
        Stream<TicketEntry> existingTicketStream) {
        return existingTicketStream.filter(this::isPendingTicket)
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

        var primaryKeyConditionAttributeValues = primaryKeyEqualityConditionAttributeValues(ticketDao);
        var put = new Put().withItem(ticketDao.toDynamoFormat())
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

    private List<TransactWriteItem> createSofDeleteFilesTransactions(Resource resource, UserInstance userInstance) {
        return resource.getFileEntries()
                   .stream()
                   .map(fileEntry -> fileEntry.softDelete(userInstance.getUser()))
                   .map(FileEntry::toDao)
                   .map(fileDao -> fileDao.toPutTransactionItem(tableName))
                   .toList();
    }

    private DeletePublicationStatusResponse delete(Publication publication) {
        publication.setStatus(DELETED);
        publication.setPublishedDate(null);
        updatePublicationIncludingStatus(publication);
        return deletionStatusChangeInProgress();
    }

    private ImportCandidate createUpdatedImportCandidate(SortableIdentifier identifier, ImportStatus status)
        throws NotFoundException {
        var importCandidate = readResourceService.getImportCandidateByIdentifier(identifier)
                                  .orElseThrow(() -> new NotFoundException(RESOURCE_NOT_FOUND_MESSAGE + identifier));
        importCandidate.setImportStatus(status);
        importCandidate.setModifiedDate(Instant.now());
        return importCandidate;
    }

    private Resource fetchExistingResource(Resource resource) {
        return attempt(() -> readResourceService.getResourceByIdentifier(resource.getIdentifier())).map(
            Optional::orElseThrow).orElseThrow(fail -> new TransactionFailedException(fail.getException()));
    }

    private TransactWriteItem createPutTransaction(Resource resourceUpdate) {

        ResourceDao resourceDao = new ResourceDao(resourceUpdate);

        Map<String, AttributeValue> primaryKeyConditionAttributeValues = primaryKeyEqualityConditionAttributeValues(
            resourceDao);

        Put put = new Put().withItem(resourceDao.toDynamoFormat())
                      .withTableName(tableName)
                      .withConditionExpression(PRIMARY_KEY_EQUALITY_CHECK_EXPRESSION)
                      .withExpressionAttributeNames(PRIMARY_KEY_EQUALITY_CONDITION_ATTRIBUTE_NAMES)
                      .withExpressionAttributeValues(primaryKeyConditionAttributeValues);

        return new TransactWriteItem().withPut(put);
    }
}
