package no.unit.nva.publication.events.handlers.batch.dynamodb.jobs;

import static no.unit.nva.publication.storage.model.DatabaseConstants.KEY_FIELDS_DELIMITER;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import no.unit.nva.clients.cristin.CristinClient;
import no.unit.nva.clients.cristin.CristinPersonDto;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.ContributorVerificationStatus;
import no.unit.nva.model.EntityDescription;
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
        workItems.stream()
            .map(this::extractIdentifier)
            .map(this::fetchResource)
            .flatMap(this::updateVerificationStatus)
            .forEach(this::persistResourceUpdate);
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

    private Resource fetchResource(SortableIdentifier identifier) {
        try {
            return resourceService.getResourceByIdentifier(identifier);
        } catch (NotFoundException e) {
            throw new RuntimeException("Resource not found: " + identifier, e);
        }
    }

    private Stream<Resource> updateVerificationStatus(Resource resource) {
        return Optional.ofNullable(resource.getEntityDescription())
                   .map(EntityDescription::getContributors)
                   .map(this::updateContributors)
                   .filter(updatedContributors -> hasChanges(resource.getEntityDescription().getContributors(),
                                                             updatedContributors))
                   .map(updatedContributors -> applyContributorUpdates(resource, updatedContributors))
                   .stream();
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

    private void persistResourceUpdate(Resource resource) {
        resourceService.updateResource(resource, UserInstance.fromPublication(resource.toPublication()));
        logger.info("Updated verification status for resource: {}", resource.getIdentifier());
    }

    private boolean hasChanges(Collection<Contributor> original, Collection<Contributor> updated) {
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
                   .map(identity -> createUpdatedIdentity(identity, status))
                   .map(updatedIdentity -> contributor.copy().withIdentity(updatedIdentity).build());
    }

    private ContributorVerificationStatus fetchVerificationStatus(URI cristinId) {
        return cristinClient.getPerson(cristinId)
                   .filter(CristinPersonDto::verified)
                   .map(person -> ContributorVerificationStatus.VERIFIED)
                   .orElse(ContributorVerificationStatus.NOT_VERIFIED);
    }

    private Identity createUpdatedIdentity(Identity original, ContributorVerificationStatus verificationStatus) {
        return original.copy()
                   .withVerificationStatus(verificationStatus)
                   .build();
    }
}
