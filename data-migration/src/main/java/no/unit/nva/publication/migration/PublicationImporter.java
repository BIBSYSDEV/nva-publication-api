package no.unit.nva.publication.migration;

import static nva.commons.core.attempt.Try.attempt;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.exception.TransactionFailedException;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.storage.model.Resource;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.attempt.Try;

public class PublicationImporter extends DataImporter {

    public static final String RESOURCE_TYPE = "publications";
    private final ResourceService resourceService;

    public PublicationImporter(S3Driver s3Client, Path dataPath, ResourceService resourceService) {
        super(s3Client, dataPath);
        this.resourceService = resourceService;
    }

    public List<Resource> createResources(List<Publication> publications) {
        return publications.stream().map(Resource::fromPublication).collect(Collectors.toList());
    }

    public List<ResourceUpdate> insertPublications(List<Publication> publications) {
        return publications.stream()
                   .map(attempt(publication -> insertPublication(resourceService, publication)))
                   .map(attempt -> attempt.map(this::fetchUpdatedPublication))
                   .map(Try::orElseThrow)
                   .collect(Collectors.toList());
    }
    private Publication insertPublication(ResourceService resourceService, Publication publication)
        throws TransactionFailedException {
        resourceService.insertPreexistingPublication(publication);
        return publication;
    }

    private ResourceUpdate fetchUpdatedPublication(Publication oldPublicationVersion) {
        return attempt(() -> resourceService.getPublication(oldPublicationVersion))
                   .map(updatedPublication -> successfulUpdate(oldPublicationVersion, updatedPublication))
                   .orElse(fail -> failedUpdate(oldPublicationVersion, fail.getException()));
    }

    private ResourceUpdate failedUpdate(Publication oldPublicationVersion, Exception exception) {
        return ResourceUpdate.createFailedUpdate(RESOURCE_TYPE, oldPublicationVersion, exception);
    }

    private ResourceUpdate successfulUpdate(Publication oldPublicationVersion, Publication updatedPublication) {
        return ResourceUpdate.createSuccessfulUpdate(RESOURCE_TYPE, oldPublicationVersion, updatedPublication);
    }


}
