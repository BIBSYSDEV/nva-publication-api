package no.unit.nva.publication.delete;

import static nva.commons.core.ioutils.IoUtils.inputStreamFromResources;
import static nva.commons.core.ioutils.IoUtils.streamToString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.amazonaws.services.lambda.runtime.Context;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.Clock;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.PublicationGenerator;
import no.unit.nva.publication.service.ResourcesDynamoDbLocalTest;
import no.unit.nva.publication.service.impl.ReadResourceService;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.mockito.Mockito;

@EnableRuleMigrationSupport
public class DeleteDraftPublicationHandlerTest extends ResourcesDynamoDbLocalTest {

    public static final String WILDCARD = "*";
    public static final String DELETE_DRAFT_PUBLICATION_WITHOUT_DOI_JSON = "delete_draft_publication_without_doi.json";
    public static final String DELETE_DRAFT_PUBLICATION_WITH_DOI_JSON = "delete_draft_publication_with_doi.json";
    
    private DeleteDraftPublicationHandler handler;
    private ByteArrayOutputStream outputStream;
    private Context context;
    private ResourceService resourceService;

    @BeforeEach
    public void setUp() {
        super.init();
        resourceService = new ResourceService(client,Clock.systemDefaultZone());
        handler = new DeleteDraftPublicationHandler(resourceService);
        outputStream = new ByteArrayOutputStream();
        context = Mockito.mock(Context.class);
    }

    @Test
    public void handleRequestDeletesPublicationWithoutDoiWhenStatusIsDraftForDeletion() throws ApiGatewayException {
        Publication publication = insertPublicationWithStatus(PublicationStatus.DRAFT_FOR_DELETION);

        ByteArrayInputStream inputStream = getInputStreamForEvent(
            DELETE_DRAFT_PUBLICATION_WITHOUT_DOI_JSON, publication.getIdentifier());

        handler.handleRequest(inputStream, outputStream, context);

        NotFoundException exception = assertThrows(NotFoundException.class,
            () -> resourceService.getPublicationByIdentifier(publication.getIdentifier()));
        String message = ReadResourceService.PUBLICATION_NOT_FOUND_CLIENT_MESSAGE + publication.getIdentifier();
        assertThat(exception.getMessage(), equalTo(message));
    }

    @Test
    public void handleRequestThrowsRuntimeExceptionOnServiceException() {
        SortableIdentifier identifier = SortableIdentifier.next();
        ByteArrayInputStream inputStream = getInputStreamForEvent(
            DELETE_DRAFT_PUBLICATION_WITHOUT_DOI_JSON, identifier);

        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> handler.handleRequest(inputStream, outputStream, context));
        String message = ReadResourceService.PUBLICATION_NOT_FOUND_CLIENT_MESSAGE + identifier;
        assertThat(exception.getMessage(), containsString(message));
    }

    @Test
    public void handleRequestThrowsRuntimeExceptionOnEventWithDoi() {
        SortableIdentifier identifier = SortableIdentifier.next();
        ByteArrayInputStream inputStream = getInputStreamForEvent(
            DELETE_DRAFT_PUBLICATION_WITH_DOI_JSON, identifier);

        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> handler.handleRequest(inputStream, outputStream, context));
        String message = DeleteDraftPublicationHandler.DELETE_WITH_DOI_ERROR;
        assertThat(exception.getMessage(), equalTo(message));
    }

    private ByteArrayInputStream getInputStreamForEvent(String path, SortableIdentifier identifier) {
        String eventTemplate = streamToString(inputStreamFromResources(path));
        String event = String.format(eventTemplate, identifier);

        return new ByteArrayInputStream(event.getBytes());
    }

    private Publication insertPublicationWithStatus(PublicationStatus status) throws ApiGatewayException {
        Publication publicationToCreate = PublicationGenerator.publicationWithoutIdentifier();
        publicationToCreate.setStatus(status);
        return resourceService.createPublication(publicationToCreate);
    }
}
