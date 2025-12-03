package no.unit.nva.publication.events.handlers.batch.dynamodb.jobs;

import static no.unit.nva.publication.storage.model.DatabaseConstants.KEY_FIELDS_DELIMITER;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import no.unit.nva.clients.cristin.CristinClient;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.ContributorVerificationStatus;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Identity;
import no.unit.nva.publication.events.handlers.batch.dynamodb.BatchWorkItem;
import no.unit.nva.publication.events.handlers.batch.dynamodb.DynamodbResourceBatchJobExecutor;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.storage.ResourceDao;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.ServiceWithTransactions;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpdateVerificationStatusJob extends ServiceWithTransactions
    implements DynamodbResourceBatchJobExecutor {

    private static final Logger logger = LoggerFactory.getLogger(UpdateVerificationStatusJob.class);
    private static final String JOB_TYPE = "UPDATE_VERIFICATION_STATUS";
    private static final String TABLE_NAME_ENV = "TABLE_NAME";
    private static final int IDENTIFIER_INDEX = 1;

    private final ResourceService resourceService;
    private final CristinClient cristinClient;
    private final String tableName;

    @JacocoGenerated
    public UpdateVerificationStatusJob() {
        this(ResourceService.defaultService(), CristinClient.defaultClient(),
             AmazonDynamoDBClientBuilder.defaultClient(), new Environment().readEnv(TABLE_NAME_ENV));
    }

    public UpdateVerificationStatusJob(ResourceService resourceService, CristinClient cristinClient,
                                        AmazonDynamoDB dynamoDbClient, String tableName) {
        super(dynamoDbClient);
        this.resourceService = resourceService;
        this.cristinClient = cristinClient;
        this.tableName = tableName;
    }

    @Override
    public void executeBatch(List<BatchWorkItem> workItems) {
        var transactItems = workItems.stream()
                                .map(this::extractIdentifier)
                                .map(this::fetchResourceWithVersion)
                                .flatMap(this::updateVerificationStatus)
                                .map(this::toTransactWriteItem)
                                .toList();

        if (!transactItems.isEmpty()) {
            sendTransactionWriteRequest(newTransactWriteItemsRequest(transactItems));
        }
    }

    @Override
    public String getJobType() {
        return JOB_TYPE;
    }

    private SortableIdentifier extractIdentifier(BatchWorkItem workItem) {
        var sortKey = workItem.dynamoDbKey().sortKey();
        var parts = sortKey.split(KEY_FIELDS_DELIMITER);
        return new SortableIdentifier(parts[IDENTIFIER_INDEX]);
    }

    private ResourceWithOriginalVersion fetchResourceWithVersion(SortableIdentifier identifier) {
        try {
            var resource = resourceService.getResourceByIdentifier(identifier);
            return new ResourceWithOriginalVersion(resource, resource.getVersion());
        } catch (NotFoundException e) {
            throw new RuntimeException("Resource not found: " + identifier, e);
        }
    }

    private Stream<ResourceWithOriginalVersion> updateVerificationStatus(
        ResourceWithOriginalVersion resourceWithVersion) {
        var resource = resourceWithVersion.resource();
        return Optional.ofNullable(resource.getEntityDescription())
                   .map(EntityDescription::getContributors)
                   .map(this::updateContributors)
                   .filter(updatedContributors -> hasChanges(resource.getEntityDescription().getContributors(),
                                                             updatedContributors))
                   .map(updatedContributors -> applyContributorUpdates(resource, updatedContributors))
                   .map(updatedResource -> new ResourceWithOriginalVersion(updatedResource,
                                                                            resourceWithVersion.originalVersion()))
                   .stream();
    }

    private TransactWriteItem toTransactWriteItem(
        ResourceWithOriginalVersion resourceWithVersion) {
        var dao = (ResourceDao) resourceWithVersion.resource().toDao();
        return newPutTransactionItemWithLocking(dao, resourceWithVersion.originalVersion(), tableName);
    }

    private List<Contributor> updateContributors(Collection<Contributor> contributors) {
        return contributors.stream()
                   .map(this::updateContributorVerificationStatus)
                   .toList();
    }

    private Resource applyContributorUpdates(Resource resource, List<Contributor> updatedContributors) {
        resource.getEntityDescription().setContributors(updatedContributors);
        return resource;
    }

    private boolean hasChanges(List<Contributor> original, List<Contributor> updated) {
        return !original.equals(updated);
    }

    private Contributor updateContributorVerificationStatus(Contributor contributor) {
        return extractCristinId(contributor)
                   .map(this::fetchVerificationStatus)
                   .flatMap(status -> applyVerificationStatus(contributor, status))
                   .orElse(contributor);
    }

    private Optional<URI> extractCristinId(Contributor contributor) {
        return Optional.ofNullable(contributor.getIdentity())
                   .map(Identity::getId);
    }

    private Optional<Contributor> applyVerificationStatus(Contributor contributor,
                                                          ContributorVerificationStatus status) {
        return Optional.of(contributor.getIdentity())
                   .filter(identity -> !status.equals(identity.getVerificationStatus()))
                   .map(identity -> logStatusChange(identity, status))
                   .map(identity -> createUpdatedIdentity(identity, status))
                   .map(updatedIdentity -> contributor.copy().withIdentity(updatedIdentity).build());
    }

    private Identity logStatusChange(Identity identity, ContributorVerificationStatus newStatus) {
        logger.info("Updating verification status for contributor {}: {} -> {}",
                    identity.getId(), identity.getVerificationStatus(), newStatus);
        return identity;
    }

    private ContributorVerificationStatus fetchVerificationStatus(URI cristinId) {
        var person = cristinClient.getPerson(cristinId)
                         .orElseThrow(() -> new RuntimeException("Cristin person not found: " + cristinId));
        return person.verified()
                   ? ContributorVerificationStatus.VERIFIED
                   : ContributorVerificationStatus.NOT_VERIFIED;
    }

    private Identity createUpdatedIdentity(Identity original, ContributorVerificationStatus verificationStatus) {
        return original.copy()
                   .withVerificationStatus(verificationStatus)
                   .build();
    }

    private record ResourceWithOriginalVersion(Resource resource, UUID originalVersion) {

    }
}
