package no.unit.nva.publication.service.impl;

import static no.unit.nva.publication.service.impl.ResourceServiceUtils.parseAttributeValuesMap;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCES_TABLE_NAME;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsRequest;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsResult;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.function.Supplier;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.DoiRequestStatus;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.service.impl.exceptions.BadRequestException;
import no.unit.nva.publication.storage.model.DoiRequest;
import no.unit.nva.publication.storage.model.daos.DoiRequestDao;
import no.unit.nva.publication.storage.model.daos.IdentifierEntry;
import no.unit.nva.publication.storage.model.daos.UniqueDoiRequestEntry;
import no.unit.nva.publication.storage.model.daos.WithPrimaryKey;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.core.attempt.Failure;

public class DoiRequestService {

    public static final String RESOURCE_IS_NOT_PUBLISHED_ERROR = "Resource is not published";
    private static final Supplier<SortableIdentifier> DEFAULT_IDENTIFIER_PROVIDER = SortableIdentifier::next;
    private final AmazonDynamoDB client;
    private final Clock clock;
    private final ResourceService resourceService;
    private final String tableName;
    private final Supplier<SortableIdentifier> identifierProvider;

    public DoiRequestService(AmazonDynamoDB client, Clock clock) {
        this(client,
            clock,
            DEFAULT_IDENTIFIER_PROVIDER);
    }

    protected DoiRequestService(AmazonDynamoDB client, Clock clock, Supplier<SortableIdentifier> identifierProvider) {
        this.client = client;
        this.clock = clock;
        this.resourceService = new ResourceService(client, clock);
        this.tableName = RESOURCES_TABLE_NAME;
        this.identifierProvider = identifierProvider;
    }

    public SortableIdentifier createDoiRequest(UserInstance userInstance, SortableIdentifier resourceIdentifier)
        throws BadRequestException, ConflictException {

        Publication publication = fetchPublication(userInstance, resourceIdentifier);
        checkResourceIsPublished(publication);
        DoiRequest doiRequest = createNewDoiRequestEntry(publication);
        TransactWriteItemsRequest request = createInsertionTransactionRequest(doiRequest);

        attempt(() -> client.transactWriteItems(request))
            .orElseThrow(this::handleFailedTransactionError);
        return doiRequest.getIdentifier();
    }


    private void checkResourceIsPublished(Publication publication) throws BadRequestException {
        if (!PublicationStatus.PUBLISHED.equals(publication.getStatus())) {
            throw new BadRequestException(RESOURCE_IS_NOT_PUBLISHED_ERROR);
        }
    }

    private ConflictException handleFailedTransactionError(Failure<TransactWriteItemsResult> fail) {
        return new ConflictException(fail.getException());
    }

    public DoiRequest getDoiRequest(UserInstance userInstance, SortableIdentifier identifier) {

        DoiRequestDao queryObject = DoiRequestDao
            .queryObject(userInstance.getOrganizationUri(), userInstance.getUserIdentifier(), identifier);
        GetItemRequest getItemRequest = new GetItemRequest()
            .withTableName(RESOURCES_TABLE_NAME)
            .withKey(queryObject.primaryKey());
        GetItemResult result = client.getItem(getItemRequest);
        Map<String, AttributeValue> item = result.getItem();
        DoiRequestDao dao = parseAttributeValuesMap(item, DoiRequestDao.class);
        return dao.getData();
    }

    private Publication fetchPublication(UserInstance userInstance, SortableIdentifier resourceIdentifier)
        throws BadRequestException {
        return attempt(() -> resourceService.getPublication(userInstance, resourceIdentifier))
            .orElseThrow(this::handleResourceNotFetchedError);
    }

    private BadRequestException handleResourceNotFetchedError(Failure<Publication> fail) {
        return new BadRequestException(fail.getException());
    }

    private DoiRequest createNewDoiRequestEntry(Publication publication) {
        Instant now = clock.instant();
        return new DoiRequest(
            identifierProvider.get(),
            publication.getIdentifier(),
            publication.getEntityDescription().getMainTitle(),
            publication.getOwner(),
            publication.getPublisher().getId(),
            DoiRequestStatus.REQUESTED,
            publication.getStatus(),
            now,
            now);
    }

    private TransactWriteItemsRequest createInsertionTransactionRequest(DoiRequest doiRequest) {
        TransactWriteItem doiRequestEntry = createDoiRequestInsertionEntry(doiRequest);
        TransactWriteItem identifierEntry = createUniqueIdentifierEntry(doiRequest);
        TransactWriteItem uniqueDoiRequestEntry = createUniqueDoiRequestEntry(doiRequest);

        return new TransactWriteItemsRequest()
            .withTransactItems(
                identifierEntry,
                uniqueDoiRequestEntry,
                doiRequestEntry);
    }

    private TransactWriteItem createUniqueDoiRequestEntry(DoiRequest doiRequest) {
        UniqueDoiRequestEntry uniqueDoiRequestEntry = new UniqueDoiRequestEntry(
            doiRequest.getResourceIdentifier().toString());
        return createTransactionPutEntry(uniqueDoiRequestEntry);
    }

    private <T extends WithPrimaryKey> TransactWriteItem createTransactionPutEntry(T uniqueDoiRequestEntry) {
        return ResourceServiceUtils.createTransactionPutEntry(uniqueDoiRequestEntry, tableName);
    }

    private TransactWriteItem createDoiRequestInsertionEntry(DoiRequest doiRequest) {
        return createTransactionPutEntry(new DoiRequestDao(doiRequest));
    }

    private TransactWriteItem createUniqueIdentifierEntry(DoiRequest doiRequest) {
        IdentifierEntry identifierEntry = new IdentifierEntry(doiRequest.getIdentifier().toString());
        return createTransactionPutEntry(identifierEntry);
    }
}
