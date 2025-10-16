package no.unit.nva.publication.create;

import no.unit.nva.publication.model.business.importcandidate.CandidateStatus;
import no.unit.nva.publication.model.business.importcandidate.ImportCandidate;
import nva.commons.apigateway.exceptions.BadRequestException;

public final class ImportCandidateValidator {

    private static final String RESOURCE_HAS_ALREADY_BEEN_IMPORTED = "Resource has already been imported";
    private static final String RESOURCE_IS_MISSING_SCOPUS_IDENTIFIER = "Resource is missing scopus identifier";
    private static final String RESOURCE_IS_NOT_PUBLISHABLE = "Resource is not publishable";

    private ImportCandidateValidator() {
    }

    public static void validate(ImportCandidate candidate) throws BadRequestException {
        if (CandidateStatus.IMPORTED.equals(candidate.getImportStatus().candidateStatus())) {
            throw new BadRequestException(RESOURCE_HAS_ALREADY_BEEN_IMPORTED);
        }
        if (candidate.getScopusIdentifier().isEmpty()) {
            throw new BadRequestException(RESOURCE_IS_MISSING_SCOPUS_IDENTIFIER);
        }
        if (!candidate.isPublishable()) {
            throw new BadRequestException(RESOURCE_IS_NOT_PUBLISHABLE);
        }
    }
}