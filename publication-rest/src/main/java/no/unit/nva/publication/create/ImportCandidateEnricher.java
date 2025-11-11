package no.unit.nva.publication.create;

import no.unit.nva.model.Organization;
import no.unit.nva.publication.ImportCandidateToResourceConverter;
import no.unit.nva.publication.model.business.Owner;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.model.business.importcandidate.ImportCandidate;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.UnauthorizedException;

public final class ImportCandidateEnricher {

    private ImportCandidateEnricher() {
    }

    public static Resource createResourceToImport(RequestInfo requestInfo,
                                                  ImportCandidate importCandidate,
                                                  ImportCandidate databaseVersion) throws UnauthorizedException {
        var userInstance = UserInstance.fromRequestInfo(requestInfo);

        var resource = ImportCandidateToResourceConverter.convert(databaseVersion);

        return resource.copy()
            .withEntityDescription(ImportCandidateToResourceConverter.toEntityDescription(importCandidate))
            .withAssociatedArtifactsList(importCandidate.getAssociatedArtifacts())
            .withAdditionalIdentifiers(importCandidate.getAdditionalIdentifiers())
            .withPublisher(Organization.fromUri(userInstance.getCustomerId()))
            .withResourceOwner(new Owner(userInstance.getUsername(), userInstance.getTopLevelOrgCristinId()))
            .build();
    }
}