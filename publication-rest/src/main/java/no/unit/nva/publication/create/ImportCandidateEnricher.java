package no.unit.nva.publication.create;

import no.unit.nva.model.Organization;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.Username;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.model.business.importcandidate.ImportCandidate;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.UnauthorizedException;

public final class ImportCandidateEnricher {

    private ImportCandidateEnricher() {
    }

    public static Resource createResourceToImport(RequestInfo requestInfo,
                                                  ImportCandidate input,
                                                  ImportCandidate databaseVersion) throws UnauthorizedException {
        var userInstance = UserInstance.fromRequestInfo(requestInfo);
        var enrichedCandidate = databaseVersion.copy()
            .withEntityDescription(input.getEntityDescription())
            .withAssociatedArtifacts(input.getAssociatedArtifacts())
            .withAdditionalIdentifiers(input.getAdditionalIdentifiers())
            .withPublisher(Organization.fromUri(userInstance.getCustomerId()))
            .withResourceOwner(new ResourceOwner(
                new Username(userInstance.getUsername()),
                userInstance.getTopLevelOrgCristinId()))
            .build();
        return Resource.fromImportCandidate(enrichedCandidate);
    }
}