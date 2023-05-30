package no.unit.nva.publication.create;

import static java.net.HttpURLConnection.HTTP_OK;
import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.api.PublicationResponse;
import no.unit.nva.publication.model.business.ImportCandidate;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public class CreatePublicationFromImportCandidateHandler extends ApiGatewayHandler<ImportCandidate,
                                                                                      PublicationResponse> {

    public static final String IMPORT_CANDIDATES_TABLE = new Environment().readEnv("IMPORT_CANDIDATES_TABLE");
    public static final String PUBLICATIONS_TABLE = new Environment().readEnv("PUBLICATIONS_TABLE");

    private final ResourceService importCandidatesService;
    private ResourceService publicationService;

    @JacocoGenerated
    public CreatePublicationFromImportCandidateHandler() {
        this(ResourceService.defaultService(IMPORT_CANDIDATES_TABLE),
             ResourceService.defaultService(PUBLICATIONS_TABLE));
    }

    public CreatePublicationFromImportCandidateHandler(ResourceService importCandidateService) {
        super(ImportCandidate.class);
        this.importCandidatesService = importCandidateService;
    }

    public CreatePublicationFromImportCandidateHandler(ResourceService importCandidateService,
                                                       ResourceService publicationService) {
        super(ImportCandidate.class);
        this.importCandidatesService = importCandidateService;
        this.publicationService = publicationService;
    }

    @Override
    protected PublicationResponse processInput(ImportCandidate input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        var importCandidate = importCandidatesService.updateImportStatus(input.getIdentifier());
        var publication = importCandidate.toPublication();

        var userInstance = UserInstance.fromPublication(publication);
        var persistedPublication = Resource.fromPublication(publication).persistNew(publicationService, userInstance);
        publicationService.publishPublication(userInstance, persistedPublication.getIdentifier());
        return PublicationResponse.fromPublication(publication);
    }

    @Override
    protected Integer getSuccessStatusCode(ImportCandidate input, PublicationResponse output) {
        return HTTP_OK;
    }
}
