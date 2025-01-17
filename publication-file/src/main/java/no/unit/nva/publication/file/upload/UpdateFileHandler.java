package no.unit.nva.publication.file.upload;

import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.publication.RequestUtil;
import no.unit.nva.publication.file.upload.restmodel.UpdateFileRequest;
import no.unit.nva.publication.model.business.UserInstance;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.JacocoGenerated;

public class UpdateFileHandler extends ApiGatewayHandler<UpdateFileRequest, Void> {

    private final FileService fileService;

    @JacocoGenerated
    public UpdateFileHandler() {
        this(FileService.defaultFileService());
    }

    public UpdateFileHandler(FileService fileService) {
        super(UpdateFileRequest.class);
        this.fileService = fileService;
    }

    @Override
    protected void validateRequest(UpdateFileRequest updateFileRequest, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        if (!RequestUtil.getFileIdentifier(requestInfo).equals(updateFileRequest.identifier())) {
            throw new BadRequestException("File identifier in request body does not match file identifier in path!");
        }
    }

    @Override
    protected Void processInput(UpdateFileRequest input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {

        var userInstance = UserInstance.fromRequestInfo(requestInfo);
        var fileIdentifier = RequestUtil.getFileIdentifier(requestInfo);
        var resourceIdentifier = RequestUtil.getIdentifier(requestInfo);

        fileService.updateFile(fileIdentifier, resourceIdentifier, userInstance, input);

        return null;
    }

    @Override
    protected Integer getSuccessStatusCode(UpdateFileRequest input, Void output) {
        return HTTP_ACCEPTED;
    }
}
