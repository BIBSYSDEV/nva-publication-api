package no.unit.nva.publication.delete;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.PublicationGenerator;
import no.unit.nva.publication.exception.NotFoundException;
import no.unit.nva.publication.service.PublicationService;
import no.unit.nva.publication.service.PublicationsDynamoDBLocal;
import no.unit.nva.publication.service.impl.DynamoDBPublicationService;
import nva.commons.exceptions.ApiGatewayException;
import nva.commons.utils.Environment;
import nva.commons.utils.JsonUtils;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.UUID;

import static nva.commons.handlers.ApiGatewayHandler.ALLOWED_ORIGIN_ENV;
import static nva.commons.utils.IoUtils.inputStreamFromResources;
import static nva.commons.utils.IoUtils.streamToString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@EnableRuleMigrationSupport
public class DeleteDraftPublicationHandlerTest {

    public static final String WILDCARD = "*";
    public static final String DELETE_DRAFT_PUBLICATION_WITHOUT_DOI_JSON = "delete_draft_publication_without_doi.json";
    public static final String DELETE_DRAFT_PUBLICATION_WITH_DOI_JSON = "delete_draft_publication_with_doi.json";

    private DeleteDraftPublicationHandler handler;
    private PublicationService publicationService;
    private Environment environment;
    private ByteArrayOutputStream outputStream;
    private Context context;
    private static final ObjectMapper objectMapper = JsonUtils.objectMapper;

    @Rule
    public PublicationsDynamoDBLocal db = new PublicationsDynamoDBLocal();

    @BeforeEach
    public void setUp() {
        prepareEnvironment();
        publicationService = new DynamoDBPublicationService(
                objectMapper,
                db.getTable(),
                db.getByPublisherIndex(),
                db.getByPublishedDateIndex()
        );
        handler = new DeleteDraftPublicationHandler(publicationService);
        outputStream = new ByteArrayOutputStream();
        context = Mockito.mock(Context.class);
    }

    private void prepareEnvironment() {
        environment = Mockito.mock(Environment.class);
        when(environment.readEnv(ALLOWED_ORIGIN_ENV)).thenReturn(WILDCARD);
        when(environment.readEnv(DynamoDBPublicationService.TABLE_NAME_ENV))
                .thenReturn(PublicationsDynamoDBLocal.NVA_RESOURCES_TABLE_NAME);
        when(environment.readEnv(DynamoDBPublicationService.BY_PUBLISHER_INDEX_NAME_ENV))
                .thenReturn(PublicationsDynamoDBLocal.BY_PUBLISHER_INDEX_NAME);
        when(environment.readEnv(DynamoDBPublicationService.BY_PUBLISHED_PUBLICATIONS_INDEX_NAME))
                .thenReturn(PublicationsDynamoDBLocal.BY_PUBLISHED_DATE_INDEX_NAME);
    }

    @Test
    public void handleRequestDeletesPublicationWithoutDoiWhenStatusIsDraftForDeletion() throws ApiGatewayException {
        Publication publication = insertPublicationWithStatus(PublicationStatus.DRAFT_FOR_DELETION);

        ByteArrayInputStream inputStream = getInputStreamForEvent(
                DELETE_DRAFT_PUBLICATION_WITHOUT_DOI_JSON, publication.getIdentifier());

        handler.handleRequest(inputStream, outputStream, context);

        NotFoundException exception = assertThrows(NotFoundException.class,
            () -> publicationService.getPublication(publication.getIdentifier()));
        String message = DynamoDBPublicationService.PUBLICATION_NOT_FOUND + publication.getIdentifier();
        assertThat(exception.getMessage(), equalTo(message));
    }

    @Test
    public void handleRequestThrowsRuntimeExceptionOnServiceException() {
        UUID identifier = UUID.randomUUID();
        ByteArrayInputStream inputStream = getInputStreamForEvent(
                DELETE_DRAFT_PUBLICATION_WITHOUT_DOI_JSON, identifier);

        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> handler.handleRequest(inputStream, outputStream, context));
        String message = DynamoDBPublicationService.PUBLICATION_NOT_FOUND + identifier;
        assertThat(exception.getMessage(), equalTo(message));
    }

    @Test
    public void handleRequestThrowsRuntimeExceptionOnEventWithDoi() {
        UUID identifier = UUID.randomUUID();
        ByteArrayInputStream inputStream = getInputStreamForEvent(
                DELETE_DRAFT_PUBLICATION_WITH_DOI_JSON, identifier);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> handler.handleRequest(inputStream, outputStream, context));
        String message = DeleteDraftPublicationHandler.DELETE_WITH_DOI_ERROR;
        assertThat(exception.getMessage(), equalTo(message));
    }

    private ByteArrayInputStream getInputStreamForEvent(String path, UUID identifier) {
        String eventTemplate = streamToString(inputStreamFromResources(
                path));
        String event = String.format(eventTemplate, identifier);

        return new ByteArrayInputStream(event.getBytes());
    }

    private Publication insertPublicationWithStatus(PublicationStatus status) throws ApiGatewayException {
        Publication publicationToCreate = PublicationGenerator.publicationWithoutIdentifier();
        publicationToCreate.setStatus(status);
        return publicationService.createPublication(publicationToCreate);
    }


}
