package no.unit.nva.publication.events.handlers.delete;

import static no.unit.nva.model.PublicationStatus.DRAFT_FOR_DELETION;
import static no.unit.nva.publication.model.business.UserInstance.fromPublication;
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
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ReadResourceService;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mockito;

public class DeleteDraftPublicationHandlerTest extends ResourcesLocalTest {
    
    public static final String DELETE_DRAFT_PUBLICATION_WITHOUT_DOI_JSON =
        "delete/delete_draft_publication_without_doi.json";
    public static final String DELETE_DRAFT_PUBLICATION_WITH_DOI_JSON =
        "delete/delete_draft_publication_with_doi.json";
    
    private DeleteDraftPublicationHandler handler;
    private ByteArrayOutputStream outputStream;
    private Context context;
    private ResourceService resourceService;
    private ByteArrayInputStream inputStream;

    @BeforeEach
    public void setUp() {
        super.init();
        resourceService = new ResourceService(client, Clock.systemDefaultZone());
        handler = new DeleteDraftPublicationHandler(resourceService);
        outputStream = new ByteArrayOutputStream();
        context = Mockito.mock(Context.class);
    }
    
    @Test
    void shouldDeleteDraftPublicationWithoutDoiWhenStatusIsDraftForDeletion() throws ApiGatewayException {
        var publication = insertDeletablePublication();

        inputStream = getInputStreamForEvent(DELETE_DRAFT_PUBLICATION_WITHOUT_DOI_JSON, publication.getIdentifier());
        handler.handleRequest(inputStream, outputStream, context);

        Executable executable = () -> resourceService.getPublicationByIdentifier(publication.getIdentifier());
        var exception = assertThrows(NotFoundException.class, executable);
        var message = ReadResourceService.PUBLICATION_NOT_FOUND_CLIENT_MESSAGE + publication.getIdentifier();
        assertThat(exception.getMessage(), equalTo(message));
    }
    
    @Test
    void shouldThrowExceptionWhenPublicationDoesNotExist() {
        var nonExistingPublicationIdentifier = SortableIdentifier.next();

        inputStream = getInputStreamForEvent(DELETE_DRAFT_PUBLICATION_WITHOUT_DOI_JSON,
            nonExistingPublicationIdentifier);

        Executable executable = () -> handler.handleRequest(inputStream, outputStream, context);
        var exception = assertThrows(RuntimeException.class, executable);
        var message = ReadResourceService.PUBLICATION_NOT_FOUND_CLIENT_MESSAGE + nonExistingPublicationIdentifier;
        assertThat(exception.getMessage(), containsString(message));
    }
    
    @Test
    void shouldThrowRuntimeExceptionWhenPublicationForDeletionHasDoi() {
        var identifier = SortableIdentifier.next();
        inputStream = getInputStreamForEvent(DELETE_DRAFT_PUBLICATION_WITH_DOI_JSON, identifier);

        Executable executable = () -> handler.handleRequest(inputStream, outputStream, context);
        var exception = assertThrows(RuntimeException.class, executable);
        var message = DeleteDraftPublicationHandler.DELETE_WITH_DOI_ERROR;
        assertThat(exception.getMessage(), equalTo(message));
    }
    
    private ByteArrayInputStream getInputStreamForEvent(String path, SortableIdentifier identifier) {
        var eventTemplate = streamToString(inputStreamFromResources(path));
        var event = String.format(eventTemplate, identifier);
        
        return new ByteArrayInputStream(event.getBytes());
    }
    
    private Publication insertDeletablePublication() throws ApiGatewayException {
        var publicationToCreate = PublicationGenerator.publicationWithoutIdentifier().copy()
            .withDoi(null)
            .withStatus(DRAFT_FOR_DELETION)
            .build();
        return resourceService.createPublication(fromPublication(publicationToCreate), publicationToCreate);
    }
}
