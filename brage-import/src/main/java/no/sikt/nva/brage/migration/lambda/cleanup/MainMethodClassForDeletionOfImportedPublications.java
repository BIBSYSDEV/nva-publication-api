package no.sikt.nva.brage.migration.lambda.cleanup;

import static no.unit.nva.publication.s3imports.ApplicationConstants.defaultS3Client;
import static org.mockito.Mockito.mock;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.ioutils.IoUtils;

@JacocoGenerated
public class MainMethodClassForDeletionOfImportedPublications {

    /**
     * Required environment variables are: TABLE_NAME, EVENT_BUS_NAME, EVENTS_BUCKET, API_HOST, AWS_REGION. Put s3
     * location uri instead of empty string and run main method
     */

    public static final String S3LOCATION = "{ 'uri' : 's3://brage-migration-reports-884807050265/HANDLE_REPORTS/2022-12-13:13/'}";
    public static final Context context = mock(Context.class);

    public static void main(String[] args) {
        var s3Client = defaultS3Client();
        var getHandler = new ListImportedBragePublicationsHandler(s3Client);
        var deleteHandler = new DeleteImportedBragePublicationHandler(ResourceService.defaultService());
        var identifiers = getHandler.handleRequest(IoUtils.stringToStream(S3LOCATION), context);
        identifiers.forEach(identifier -> {
            try {
                deleteHandler.handleRequest(IoUtils.stringToStream(toJsonString(identifier)), context);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @SuppressWarnings("PMD.CloseResource")
    private static String toJsonString(String identifier) throws IOException {
        Writer writer = new StringWriter();
        JsonGenerator jsonGenerator = new JsonFactory().createGenerator(writer);
        new ObjectMapper().writeTree(jsonGenerator, new ObjectMapper().createObjectNode().put("id", identifier));
        return writer.toString();
    }
}
