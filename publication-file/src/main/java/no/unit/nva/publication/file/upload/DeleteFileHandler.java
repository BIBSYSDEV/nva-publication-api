package no.unit.nva.publication.file.upload;

import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.publication.RequestUtil;
import no.unit.nva.publication.model.business.UserInstance;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.JacocoGenerated;

public class DeleteFileHandler extends ApiGatewayHandler<Void, Void> {

    private final FileService fileService;

    @JacocoGenerated
    private DeleteFileHandler() {
        this(FileService.defaultFileService());
    }

    public DeleteFileHandler(FileService fileService) {
        super(Void.class);
        this.fileService = fileService;
    }

    @Override
    protected void validateRequest(Void unused, RequestInfo requestInfo, Context context) throws ApiGatewayException {
        // No validation
    }

    @Override
    protected Void processInput(Void unused, RequestInfo requestInfo, Context context) throws ApiGatewayException {
        var userInstance = UserInstance.fromRequestInfo(requestInfo);
        var resourceIdentifier = RequestUtil.getIdentifier(requestInfo);
        var fileIdentifier = RequestUtil.getFileIdentifier(requestInfo);

        fileService.deleteFile(fileIdentifier, resourceIdentifier, userInstance);

        return null;
    }

    @Override
    protected Integer getSuccessStatusCode(Void unused, Void o) {
        return HTTP_ACCEPTED;
    }
}
