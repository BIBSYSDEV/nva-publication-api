package no.unit.nva.publication.model.storage;

import static no.unit.nva.publication.storage.model.DatabaseConstants.KEY_FIELDS_DELIMITER;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCE_INDEX_FIELD_PREFIX;
import static no.unit.nva.publication.storage.model.DatabaseConstants.STRING_PLACEHOLDER;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsRequest;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Contributor;
import no.unit.nva.publication.model.business.Contribution;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.User;
import nva.commons.core.JacocoGenerated;

@JsonTypeName(ContributionDao.TYPE)
public class ContributionDao extends Dao implements JoinWithResource {

    public static final String EMPTY_STRING = "";
    public static final String BY_CONTRIBUTION_INDEX_ORDER_PREFIX = "e";
    public static final String TYPE = "Contribution";

    public static final String CONTRIBUTION_PRIMARY_KEY_PARTITION_KEY_FORMAT =
        String.join(KEY_FIELDS_DELIMITER, STRING_PLACEHOLDER, STRING_PLACEHOLDER, STRING_PLACEHOLDER);
    public static final String CONTRIBUTION_PRIMARY_KEY_SORT_KEY_FORMAT =
        String.join(KEY_FIELDS_DELIMITER, STRING_PLACEHOLDER, STRING_PLACEHOLDER, STRING_PLACEHOLDER, STRING_PLACEHOLDER);

    @JacocoGenerated
    protected ContributionDao() {
        super();
    }

    public ContributionDao(Contribution contribution) {
        super(contribution);
    }

    public ContributionDao(Resource resource, Contributor contribution) {
        super(Contribution.create(resource, contribution));
    }

    @Override
    public final String indexingType() {
        return TYPE;
    }

    @JsonIgnore
    public static String joinByResourceContainedOrderedType() {
        return BY_CONTRIBUTION_INDEX_ORDER_PREFIX + KEY_FIELDS_DELIMITER + TYPE;
    }

    @Override
    @JacocoGenerated
    public final String getPrimaryKeyPartitionKey() {
        return String.format(CONTRIBUTION_PRIMARY_KEY_PARTITION_KEY_FORMAT, RESOURCE_INDEX_FIELD_PREFIX,
                             getCustomerIdentifier(), getOwner().toString());
    }

    @Override
    @JacocoGenerated
    public final String getPrimaryKeySortKey() {
        return String.format(CONTRIBUTION_PRIMARY_KEY_SORT_KEY_FORMAT, RESOURCE_INDEX_FIELD_PREFIX, getResourceIdentifier(),
                             indexingType(), getIdentifier());
    }

    @Override
    public String joinByResourceOrderedType() {
        return joinByResourceContainedOrderedType();
    }

    @Override
    public SortableIdentifier getResourceIdentifier() {
        return ((Contribution) getData()).getResourceIdentifier();
    }

    @Override
    public URI getCustomerId() {
        return getContribution().getCustomerId();
    }

    @Override
    public TransactWriteItemsRequest createInsertionTransactionRequest() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateExistingEntry(AmazonDynamoDB client) {
        throw new UnsupportedOperationException("Not implemented yet.Call the appropriate resource service method");
    }

    @Override
    protected User getOwner() {
        return getData().getOwner();
    }

    @JsonIgnore
    public Contribution getContribution() {
        return (Contribution) getData();
    }

    public static ContributionDao queryObject(ResourceDao queryObject) {
        var doiRequest = Contribution.builder().withResourceIdentifier(queryObject.getResourceIdentifier()).build();
        return new ContributionDao(doiRequest);
    }

}
