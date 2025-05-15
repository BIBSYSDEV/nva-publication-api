package no.unit.nva.publication.file.upload;

import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.publication.RequestUtil;
import no.unit.nva.publication.model.business.UserInstance;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public class UpdateFileHandler extends ApiGatewayHandler<File, Void> {

    private final FileService fileService;

    @JacocoGenerated
    public UpdateFileHandler() {
        this(FileService.defaultFileService(), new Environment());
    }

    public UpdateFileHandler(FileService fileService, Environment environment) {
        super(File.class, environment);
        this.fileService = fileService;
    }

    @Override
    protected void validateRequest(File file, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        if (!RequestUtil.getFileIdentifier(requestInfo).equals(file.getIdentifier())) {
            throw new BadRequestException("File identifier in request body does not match file identifier in path!");
        }
    }

    @Override
    protected Void processInput(File input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {

        var userInstance = UserInstance.fromRequestInfo(requestInfo);
        var fileIdentifier = RequestUtil.getFileIdentifier(requestInfo);
        var resourceIdentifier = RequestUtil.getIdentifier(requestInfo);

        fileService.updateFile(fileIdentifier, resourceIdentifier, userInstance, input);

        return null;
    }

    @Override
    protected Integer getSuccessStatusCode(File input, Void output) {
        return HTTP_ACCEPTED;
    }
}
