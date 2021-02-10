package no.unit.nva.publication.service.impl;

import static no.unit.nva.publication.service.impl.ReadResourceService.RESOURCE_NOT_FOUND_MESSAGE;
import static nva.commons.core.JsonUtils.objectMapper;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.Delete;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsRequest;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsResult;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import no.unit.nva.publication.service.impl.exceptions.BadRequestException;
import no.unit.nva.publication.storage.model.Resource;
import no.unit.nva.publication.storage.model.daos.Dao;
import no.unit.nva.publication.storage.model.daos.DoiRequestDao;
import no.unit.nva.publication.storage.model.daos.ResourceDao;
import no.unit.nva.publication.storage.model.daos.WithPrimaryKey;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.core.attempt.Failure;

public abstract class ServiceWithTransactions {

    public static final String EMPTY_STRING = "";
    public static final String DOUBLE_QUOTES = "\"";
    public static final String RAWTYPES = "rawtypes";

    public static final String RESOURCE_FIELD_IN_RESOURCE_DAO = ResourceDao.CONTAINED_DATA_FIELD_NAME;
    public static final String STATUS_FIELD_IN_RESOURCE = "status";
    public static final String MODIFIED_FIELD_IN_RESOURCE = "modifiedDate";
    public static final String RESOURCE_FILE_SET_FIELD = "fileSet";
    public static final int DOI_REQUEST_INDEX_IN_QUERY_RESULT_WHEN_DOI_REQUEST_EXISTS = 0;
    private static final int RESOURCE_INDEX_IN_QUERY_RESULT_WHEN_DOI_REQUEST_EXISTS = 1;
    private static final int RESOURCE_INDEX_IN_QUERY_RESULT_WHEN_DOI_REQUEST_NOT_EXISTS = 0;

    protected abstract String getTableName();

    protected abstract AmazonDynamoDB getClient();

    protected TransactWriteItem createTransactionEntryForInsertingResource(Resource resource) {
        return createTransactionPutEntry(new ResourceDao(resource));
    }

    protected <T extends WithPrimaryKey> TransactWriteItem createTransactionPutEntry(T resourceDao) {
        return ResourceServiceUtils.createTransactionPutEntry(resourceDao, getTableName());
    }

    protected <T extends WithPrimaryKey> TransactWriteItem newDeleteTransactionItem(Resource resource) {
        ResourceDao resourceDao = new ResourceDao(resource);
        return new TransactWriteItem()
            .withDelete(new Delete().withTableName(getTableName()).withKey(resourceDao.primaryKey()));
    }

    protected TransactWriteItemsResult sendTransactionWriteRequest(TransactWriteItemsRequest transactWriteItemsRequest)
        throws ConflictException {
        return attempt(() -> getClient().transactWriteItems(transactWriteItemsRequest))
            .orElseThrow(this::handleTransactionFailure);
    }

    @SuppressWarnings(RAWTYPES)
    protected Optional<DoiRequestDao> extractDoiRequest(List<Dao> daos) {
        if (doiRequestExists(daos)) {
            return Optional.of((DoiRequestDao) daos.get(DOI_REQUEST_INDEX_IN_QUERY_RESULT_WHEN_DOI_REQUEST_EXISTS));
        }
        return Optional.empty();
    }

    @SuppressWarnings(RAWTYPES)
    protected ResourceDao extractResourceDao(List<Dao> daos) throws BadRequestException {
        if (doiRequestExists(daos)) {
            return (ResourceDao) daos.get(RESOURCE_INDEX_IN_QUERY_RESULT_WHEN_DOI_REQUEST_EXISTS);
        } else if (onlyResourceExists(daos)) {
            return (ResourceDao) daos.get(RESOURCE_INDEX_IN_QUERY_RESULT_WHEN_DOI_REQUEST_NOT_EXISTS);
        }
        throw new BadRequestException(RESOURCE_NOT_FOUND_MESSAGE);
    }

    protected String nowAsString() {
        String jsonString = attempt(() -> objectMapper.writeValueAsString(getClock().instant()))
            .orElseThrow();
        return jsonString.replaceAll(DOUBLE_QUOTES, EMPTY_STRING);
    }

    protected abstract Clock getClock();

    private ConflictException handleTransactionFailure(Failure<TransactWriteItemsResult> fail) {
        return new ConflictException(fail.getException());
    }

    @SuppressWarnings(RAWTYPES)
    private boolean onlyResourceExists(List<Dao> daos) {
        return daos.size() == 1;
    }

    @SuppressWarnings(RAWTYPES)
    private boolean doiRequestExists(List<Dao> daos) {
        return daos.size() == 2;
    }
}
