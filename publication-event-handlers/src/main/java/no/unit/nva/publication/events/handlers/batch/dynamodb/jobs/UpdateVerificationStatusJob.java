package no.unit.nva.publication.events.handlers.batch.dynamodb.jobs;

import static java.util.Objects.nonNull;
import static no.unit.nva.publication.storage.model.DatabaseConstants.KEY_FIELDS_DELIMITER;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import no.unit.nva.clients.cristin.CristinClient;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.ContributorVerificationStatus;
import no.unit.nva.model.Identity;
import no.unit.nva.publication.events.handlers.batch.dynamodb.BatchWorkItem;
import no.unit.nva.publication.events.handlers.batch.dynamodb.DynamodbResourceBatchJobExecutor;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpdateVerificationStatusJob implements DynamodbResourceBatchJobExecutor {

    private static final Logger logger = LoggerFactory.getLogger(UpdateVerificationStatusJob.class);
    private static final String JOB_TYPE = "UPDATE_VERIFICATION_STATUS";
    private static final int IDENTIFIER_INDEX = 1;

    private final ResourceService resourceService;
    private final CristinClient cristinClient;

    @JacocoGenerated
    public UpdateVerificationStatusJob() {
        this(ResourceService.defaultService(), CristinClient.defaultClient());
    }

    public UpdateVerificationStatusJob(ResourceService resourceService, CristinClient cristinClient) {
        this.resourceService = resourceService;
        this.cristinClient = cristinClient;
    }

    @Override
    public void executeBatch(List<BatchWorkItem> workItems) {
        if (workItems.isEmpty()) {
            return;
        }

        workItems.stream()
            .map(this::extractIdentifier)
            .forEach(this::updateVerificationStatusForResource);
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

    private void updateVerificationStatusForResource(SortableIdentifier identifier) {
        try {
            var resource = resourceService.getResourceByIdentifier(identifier);
            updateVerificationStatus(resource);
        } catch (NotFoundException e) {
            logger.warn("Resource not found: {}", identifier);
        }
    }

    private void updateVerificationStatus(Resource resource) {
        var entityDescription = resource.getEntityDescription();

        if (entityDescription == null) {
            return;
        }

        var contributors = entityDescription.getContributors();

        if (contributors == null || contributors.isEmpty()) {
            return;
        }

        var updatedContributors = contributors.stream()
                                      .map(this::updateContributorVerificationStatus)
                                      .toList();

        if (hasChanges(contributors, updatedContributors)) {
            entityDescription.setContributors(updatedContributors);
            resourceService.updateResource(resource, UserInstance.fromResource(resource));
            logger.info("Updated verification status for resource: {}", resource.getIdentifier());
        }
    }

    private boolean hasChanges(List<Contributor> original, List<Contributor> updated) {
        return !original.equals(updated);
    }

    private Contributor updateContributorVerificationStatus(Contributor contributor) {
        var identity = contributor.getIdentity();
        var cristinId = extractCristinId(identity);

        if (cristinId.isEmpty()) {
            return contributor;
        }

        var verificationStatus = fetchVerificationStatus(cristinId.get());

        if (verificationStatus.equals(identity.getVerificationStatus())) {
            return contributor;
        }

        var updatedIdentity = createUpdatedIdentity(identity, verificationStatus);
        return contributor.copy().withIdentity(updatedIdentity).build();
    }

    private Optional<URI> extractCristinId(Identity identity) {
        return Optional.ofNullable(identity)
                   .map(Identity::getId)
                   .filter(this::isCristinPersonUri);
    }

    private boolean isCristinPersonUri(URI uri) {
        return nonNull(uri) && uri.toString().contains("/cristin/person/");
    }

    private ContributorVerificationStatus fetchVerificationStatus(URI cristinId) {
        return cristinClient.getPerson(cristinId)
                   .map(person -> person.verified()
                                      ? ContributorVerificationStatus.VERIFIED
                                      : ContributorVerificationStatus.NOT_VERIFIED)
                   .orElse(ContributorVerificationStatus.CANNOT_BE_ESTABLISHED);
    }

    private Identity createUpdatedIdentity(Identity original, ContributorVerificationStatus verificationStatus) {
        return new Identity.Builder()
                   .withId(original.getId())
                   .withName(original.getName())
                   .withNameType(original.getNameType())
                   .withOrcId(original.getOrcId())
                   .withAdditionalIdentifiers(original.getAdditionalIdentifiers())
                   .withVerificationStatus(verificationStatus)
                   .build();
    }
}
