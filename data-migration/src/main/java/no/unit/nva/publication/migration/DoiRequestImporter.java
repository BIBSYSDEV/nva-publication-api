package no.unit.nva.publication.migration;

import static java.util.Objects.nonNull;
import static no.unit.nva.publication.service.impl.ResourceServiceUtils.extractOwner;
import static nva.commons.core.attempt.Try.attempt;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.DoiRequestStatus;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.service.impl.DoiRequestService;
import no.unit.nva.publication.storage.model.DoiRequest;
import no.unit.nva.publication.storage.model.UserInstance;
import no.unit.nva.s3.S3Driver;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.attempt.Failure;

public class DoiRequestImporter extends DataImporter {

    private static final String RESOURCE_TYPE = "doiRequest";
    private final DoiRequestService doiRequestService;

    public DoiRequestImporter(S3Driver s3Client, Path dataPath, DoiRequestService doiRequestService) {
        super(s3Client, dataPath);
        this.doiRequestService = doiRequestService;
    }

    public List<ResourceUpdate> insertDoiRequests(List<Publication> publications) {
        return publications
                   .stream()
                   .filter(this::hasDoiRequest)
                   .map(this::createDoiRequestForPublication)
                   .collect(Collectors.toList());
    }

    private boolean hasDoiRequest(Publication pub) {
        return nonNull(pub.getDoiRequest());
    }

    private ResourceUpdate createDoiRequestForPublication(Publication publication) {
        return attempt(() -> insertDoiRequestToDb(publication))
                   .map(doiRequest -> successfulUpdate(publication, doiRequest))
                   .orElse(fail -> failedUpdate(fail, publication));
    }

    private ResourceUpdate failedUpdate(Failure<ResourceUpdate> fail,
                                        Publication publication) {
        return ResourceUpdate.createFailedUpdate(RESOURCE_TYPE, publication, fail.getException());
    }

    private ResourceUpdate successfulUpdate(Publication oldEntry, DoiRequest doiRequest) {
        return ResourceUpdate.createSuccessfulUpdate(RESOURCE_TYPE, oldEntry, doiRequest.toPublication());
    }

    private DoiRequest insertDoiRequestToDb(Publication publication) throws ApiGatewayException {
        SortableIdentifier doiRequestIdentifier = doiRequestService.createDoiRequest(publication);
        UserInstance owner = extractOwner(publication);
        DoiRequestStatus doiRequestStatus = publication.getDoiRequest().getStatus();
        if (!doiRequestStatus.equals(DoiRequestStatus.REQUESTED)) {
            doiRequestService.updateDoiRequest(owner, publication.getIdentifier(), doiRequestStatus);
        }

        return doiRequestService.getDoiRequest(owner, doiRequestIdentifier);
    }
}
