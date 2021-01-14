package no.unit.nva.doi.handler;

import static no.unit.nva.doi.UpdateDoiStatusProcess.ERROR_BAD_DOI_UPDATE_HOLDER_FORMAT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;
import no.unit.nva.doi.UpdateDoiStatusProcess;
import no.unit.nva.doi.handler.exception.DependencyRemoteNvaApiException;
import no.unit.nva.model.Publication;
import no.unit.nva.model.Publication.Builder;
import no.unit.nva.publication.service.PublicationService;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.exceptions.ApiIoException;
import nva.commons.core.ioutils.IoUtils;import nva.commons.utils.log.LogUtils;
import nva.commons.utils.log.TestAppender;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class UpdateDoiStatusHandlerTest {

    public static final Path BAD_EVENT_WITH_DATE_IN_FUTURE = Path.of(
        "update_doi_status_event_bad_date_in_the_future.json");
    public static final String NULL_OBJECT = "{ }";
    public static final URI EXAMPLE_DOI = URI.create("https://doi.org/10.1103/physrevd.100.085005");
    public static final Instant EXAMPLE_DOI_MODIFIED_DATE = Instant.parse("2020-08-14T10:30:10.019991Z");
    private static final UUID EXAMPLE_PUBLICATION_ID = UUID.fromString("41076d56-2839-11eb-b644-1bb5be85c01b");
    private static final Path BAD_EVENT_WITH_BAD_PAYLOAD_NOT_MATCHING_POJO = Path.of(
        "update_doi_status_event_bad_input_not_matching_pojo.json");
    private static final Path OK_EVENT = Path.of("update_doi_status_event.json");
    private UpdateDoiStatusHandler handler;
    private ByteArrayOutputStream outputStream;
    private Context context;
    private PublicationService publicationService;
    private TestAppender logger;

    @BeforeEach
    void setUp() throws ApiGatewayException {
        logger = LogUtils.getTestingAppender(UpdateDoiStatusProcess.class);
        publicationService = mock(PublicationService.class);
        handler = new UpdateDoiStatusHandler(publicationService);
        context = mock(Context.class);
        outputStream = new ByteArrayOutputStream();

        when(publicationService.getPublication(EXAMPLE_PUBLICATION_ID)).thenReturn(new Publication.Builder()
            .withIdentifier(EXAMPLE_PUBLICATION_ID)
            .build());
    }

    @Test
    void handleRequestThrowsIllegalSateExceptionWhereRequestedDoiModificationTimeIsInTheFuture() {
        var eventInputStream = IoUtils.inputStreamFromResources(BAD_EVENT_WITH_DATE_IN_FUTURE);

        var actualException = assertThrows(IllegalStateException.class,
            () -> handler.handleRequest(eventInputStream, outputStream, context));
        assertThat(actualException.getMessage(), is(equalTo("Modified doi is in the future, bailing!")));
    }

    @Test
    void handleRequestThrowsRuntimeExceptionCausedByMismatchedInputExceptionWhereDoiRequestHolderIsNullDueToBadInput() {
        var inputStream = new ByteArrayInputStream(new byte[0]);

        var actualException = assertThrows(RuntimeException.class,
            () -> handler.handleRequest(inputStream, outputStream, context));
        assertThat(actualException.getCause(), is(instanceOf(MismatchedInputException.class)));
        assertThat(actualException.getMessage(), containsString("No content to map due to end-of-input"));
    }

    @Test
    void handleRequestThrowsIllegalArgumentExceptionWherePayloadNotMatchingDoiRequestHolderPojo() {
        var eventInputStream = IoUtils.inputStreamFromResources(BAD_EVENT_WITH_BAD_PAYLOAD_NOT_MATCHING_POJO);

        var actualException = assertThrows(IllegalArgumentException.class,
            () -> handler.handleRequest(eventInputStream, outputStream, context));
        assertThat(actualException.getMessage(), is(equalTo(String.format(ERROR_BAD_DOI_UPDATE_HOLDER_FORMAT,
            NULL_OBJECT))));
    }

    @Test
    void handleRequestThrowsDependencyRemoteNvaApiExceptionWhenPublicationServiceFailsToFetchPublication()
        throws ApiGatewayException {
        when(publicationService.getPublication(EXAMPLE_PUBLICATION_ID)).thenThrow(ApiIoException.class);

        var eventInputStream = IoUtils.inputStreamFromResources(OK_EVENT);

        var actualException = assertThrows(DependencyRemoteNvaApiException.class,
            () -> handler.handleRequest(eventInputStream, outputStream, context));
        assertThat(actualException.getCause(), is(Matchers.isA(ApiGatewayException.class)));
    }

    @Test
    void handleRequestThrowsDependencyRemoteNvaApiExceptionWhenPublicationServiceFailsToUpdatePublication()
        throws ApiGatewayException, JsonProcessingException {
        Publication publication = new Builder()
            .withIdentifier(EXAMPLE_PUBLICATION_ID)
            .build();
        when(publicationService.getPublication(EXAMPLE_PUBLICATION_ID)).thenReturn(publication);
        when(publicationService.updatePublication(eq(EXAMPLE_PUBLICATION_ID), any(Publication.class)))
            .thenThrow(ApiIoException.class);

        var eventInputStream = IoUtils.inputStreamFromResources(OK_EVENT);
        var actualException = assertThrows(DependencyRemoteNvaApiException.class,
            () -> handler.handleRequest(eventInputStream, outputStream, context));
        assertThat(actualException.getCause(), is(Matchers.isA(ApiGatewayException.class)));
    }

    @Test
    void handleRequestSuccessfullyWhenPayloadContainsDoiUpdateHolderWithValidFields()
        throws ApiGatewayException, JsonProcessingException {
        var publication = new Builder()
            .withIdentifier(EXAMPLE_PUBLICATION_ID)
            .build();

        var expectedPublicationUpdate = new Builder()
            .withIdentifier(EXAMPLE_PUBLICATION_ID)
            .withDoi(EXAMPLE_DOI)
            .withModifiedDate(EXAMPLE_DOI_MODIFIED_DATE)
            .build();

        stubSuccessfullDoiStatusUpdate(publication, expectedPublicationUpdate);

        var eventInputStream = IoUtils.inputStreamFromResources(OK_EVENT);
        handler.handleRequest(eventInputStream, outputStream, context);
        verifySuccessfulDoiStatusUpdate(expectedPublicationUpdate);
    }

    @Test
    void handleRequestSuccessfullyThenLogsInformationMessage()
        throws ApiGatewayException, JsonProcessingException {
        var publication = new Builder()
            .withIdentifier(EXAMPLE_PUBLICATION_ID)
            .build();

        var expectedPublicationUpdate = new Builder()
            .withIdentifier(EXAMPLE_PUBLICATION_ID)
            .withDoi(EXAMPLE_DOI)
            .withModifiedDate(EXAMPLE_DOI_MODIFIED_DATE)
            .build();

        stubSuccessfullDoiStatusUpdate(publication, expectedPublicationUpdate);

        var eventInputStream = IoUtils.inputStreamFromResources(OK_EVENT);
        handler.handleRequest(eventInputStream, outputStream, context);

        assertThat(logger.getMessages(), containsString(String.format(UpdateDoiStatusProcess.UPDATED_PUBLICATION_FORMAT,
            EXAMPLE_PUBLICATION_ID,
            EXAMPLE_DOI,
            EXAMPLE_DOI_MODIFIED_DATE
        )));
    }

    private void verifySuccessfulDoiStatusUpdate(Publication expectedPublicationUpdate) throws ApiGatewayException {
        ArgumentCaptor<Publication> publicationServiceCaptor = ArgumentCaptor.forClass(Publication.class);
        verify(publicationService).updatePublication(eq(EXAMPLE_PUBLICATION_ID), publicationServiceCaptor.capture());
        Publication actualPublicationUpdate = publicationServiceCaptor.getValue();
        assertThat(actualPublicationUpdate, is(equalTo(expectedPublicationUpdate)));
    }

    private void stubSuccessfullDoiStatusUpdate(Publication publication, Publication expectedPublicationUpdate)
        throws ApiGatewayException {
        when(publicationService.getPublication(EXAMPLE_PUBLICATION_ID)).thenReturn(publication);
        when(publicationService.updatePublication(eq(EXAMPLE_PUBLICATION_ID), any(Publication.class)))
            .thenReturn(expectedPublicationUpdate);
    }
}