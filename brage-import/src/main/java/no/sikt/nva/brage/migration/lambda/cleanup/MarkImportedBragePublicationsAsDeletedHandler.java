package no.sikt.nva.brage.migration.lambda.cleanup;

import static no.unit.nva.publication.s3imports.ApplicationConstants.defaultS3Client;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.ioutils.IoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;

public class MarkImportedBragePublicationsAsDeletedHandler implements RequestHandler<InputStream, String> {

    public static final String PUBLICATIONS_DELETED_MESSAGE = "Publications were marked as deleted";
    private static final Logger logger = LoggerFactory.getLogger(MarkImportedBragePublicationsAsDeletedHandler.class);
    private final ResourceService resourceService;
    private final S3Client s3Client;

    public MarkImportedBragePublicationsAsDeletedHandler(ResourceService resourceService, S3Client s3Client) {
        this.resourceService = resourceService;
        this.s3Client = s3Client;
    }

    @JacocoGenerated
    public MarkImportedBragePublicationsAsDeletedHandler() {
        this(ResourceService.defaultService(), defaultS3Client());
    }

    @Override
    public String handleRequest(InputStream input, Context context) {
        var identifiers = listIdentifiers(input, context);
        runDeleteWork(context, identifiers);
        logger.info(PUBLICATIONS_DELETED_MESSAGE);
        return null;
    }

    @SuppressWarnings("PMD.CloseResource")
    private static String toJsonString(String identifier) throws IOException {
        Writer writer = new StringWriter();
        JsonGenerator jsonGenerator = new JsonFactory().createGenerator(writer);
        new ObjectMapper().writeTree(jsonGenerator, new ObjectMapper().createObjectNode().put("id", identifier));
        return writer.toString();
    }

    private void runDeleteWork(Context context, List<String> identifiers) {
        var handler = new DeleteImportedBragePublicationHandler(this.resourceService);
        identifiers.forEach(id -> {
            try {
                handler.handleRequest(IoUtils.stringToStream(toJsonString(id)), context);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private List<String> listIdentifiers(InputStream input, Context context) {
        var handler = new ListImportedBragePublicationsHandler(this.s3Client);
        return handler.handleRequest(input, context);
    }
}
