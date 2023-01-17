package no.sikt.nva.brage.migration.lambda.cleanup;

import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import no.sikt.nva.brage.migration.testutils.FakeResourceService;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.service.ResourcesLocalTest;
import nva.commons.core.ioutils.IoUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DeleteImportedBragePublicationHandlerTest extends ResourcesLocalTest {

    public static final Context context = mock(Context.class);
    private FakeResourceService resourceService;
    private DeleteImportedBragePublicationHandler handler;

    @BeforeEach
    public void init() {
        super.init();
        this.resourceService = new FakeResourceService();
        this.handler = new DeleteImportedBragePublicationHandler(resourceService);
    }

    @Test
    void shouldDeleteImportedPublication() throws IOException {
        var publication = randomPublication();
        resourceService.addPublicationWithCristinIdentifier(publication);
        var expectedPublication = publication.copy().withStatus(PublicationStatus.DELETED).build();
        handler.handleRequest(toJsonStream(publication), context);
        var actualPublication = resourceService.getPublicationByIdentifier(publication.getIdentifier());
        assertThat(actualPublication, is(equalTo(expectedPublication)));
    }

    private static InputStream toJsonStream(Publication publication) throws IOException {
        return IoUtils.stringToStream(toJsonString(publication.getIdentifier().toString()));
    }

    private static String toJsonString(String identifier) throws IOException {
        var node = new ObjectMapper().createObjectNode();
        node.put("id", identifier);
        JsonFactory jsonFactory = new JsonFactory();
        Writer writer = new StringWriter();
        JsonGenerator jsonGenerator = jsonFactory.createGenerator(writer);
        new ObjectMapper().writeTree(jsonGenerator, node);
        return writer.toString();
    }
}
