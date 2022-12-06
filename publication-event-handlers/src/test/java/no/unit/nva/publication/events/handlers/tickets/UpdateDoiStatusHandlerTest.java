package no.unit.nva.publication.events.handlers.tickets;

import static no.unit.nva.publication.events.handlers.tickets.UpdateDoiStatusProcess.ERROR_BAD_DOI_UPDATE_HOLDER_FORMAT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import no.unit.nva.events.handlers.EventHandler;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.Publication.Builder;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.ioutils.IoUtils;
import nva.commons.logutils.LogUtils;
import nva.commons.logutils.TestAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class UpdateDoiStatusHandlerTest {

    public static final String BAD_EVENT_WITH_DATE_IN_FUTURE =
        "doirequests/update_doi_status_event_bad_date_in_the_future.json";
    public static final String EMPTY_OBJECT = "{}";
    public static final URI EXAMPLE_DOI = URI.create("https://doi.org/10.1103/physrevd.100.085005");
    public static final Instant EXAMPLE_DOI_MODIFIED_DATE = Instant.parse("2020-08-14T10:30:10.019991Z");
    private static final SortableIdentifier PUBLICATION_IDENTIFIER_IN_RESOURCES =
        new SortableIdentifier("41076d56-2839-11eb-b644-1bb5be85c01b");
    private static final String BAD_EVENT_WITH_BAD_PAYLOAD_NOT_MATCHING_POJO =
        "doirequests/update_doi_status_event_bad_input_not_matching_pojo.json";
    private static final String OK_EVENT = "doirequests/update_doi_status_event.json";
    private static final String WITHOUT_DOI_EVENT = "doirequests/update_doi_status_event_without_doi_draft_deleted"
                                                    + ".json";
    private UpdateDoiStatusHandler handler;
    private ByteArrayOutputStream outputStream;
    private Context context;
    private ResourceService resourceService;
    private TestAppender logger;

    @BeforeEach
    void setUp() throws ApiGatewayException {
        logger = LogUtils.getTestingAppender(UpdateDoiStatusProcess.class);
        resourceService = mock(ResourceService.class);
        handler = new UpdateDoiStatusHandler(resourceService);
        context = mock(Context.class);
        outputStream = new ByteArrayOutputStream();

        when(resourceService.getPublicationByIdentifier(PUBLICATION_IDENTIFIER_IN_RESOURCES)).thenReturn(
            new Publication.Builder()
                .withIdentifier(PUBLICATION_IDENTIFIER_IN_RESOURCES)
                .build());
    }

    @Test
    void handleRequestThrowsIllegalStateExceptionWhereRequestedDoiModificationTimeIsInTheFuture() throws IOException {
        try (var eventInputStream = IoUtils.inputStreamFromResources(BAD_EVENT_WITH_DATE_IN_FUTURE)) {

            var actualException = assertThrows(RuntimeException.class,
                                               () -> handler.handleRequest(eventInputStream, outputStream, context));
            assertThat(actualException.getMessage(), is(equalTo("Modified doi is in the future, bailing!")));
        }
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
    void handleRequestThrowsIllegalArgumentExceptionWherePayloadNotMatchingDoiRequestHolderPojo() throws IOException {

        try (var eventInputStream
                 = IoUtils.inputStreamFromResources(BAD_EVENT_WITH_BAD_PAYLOAD_NOT_MATCHING_POJO)) {

            IllegalArgumentException actualException = assertThrows(IllegalArgumentException.class,
                                                                    () -> handler.handleRequest(eventInputStream,
                                                                                                outputStream, context));

            assertThat(actualException.getMessage(), is(equalTo(String.format(ERROR_BAD_DOI_UPDATE_HOLDER_FORMAT,
                                                                              EMPTY_OBJECT))));
        }
    }

    @Test
    void handleRequestThrowsDependencyRemoteNvaApiExceptionWhenPublicationServiceFailsToFetchPublication()
        throws ApiGatewayException, IOException {

        when(resourceService.getPublicationByIdentifier(PUBLICATION_IDENTIFIER_IN_RESOURCES)).thenThrow(
            NotFoundException.class);

        try (var eventInputStream = IoUtils.inputStreamFromResources(OK_EVENT)) {

            var actualException = assertThrows(DependencyRemoteNvaApiException.class,
                                               () -> handler.handleRequest(eventInputStream, outputStream, context));
            assertThat(actualException, is(instanceOf(DependencyRemoteNvaApiException.class)));
        }
    }

    @Test
    void handleRequestLogsUnexpectedExceptions() throws ApiGatewayException, IOException {
        final TestAppender testAppender = LogUtils.getTestingAppender(EventHandler.class);
        Publication publication = new Builder()
                                      .withIdentifier(PUBLICATION_IDENTIFIER_IN_RESOURCES)
                                      .build();
        when(resourceService.getPublicationByIdentifier(PUBLICATION_IDENTIFIER_IN_RESOURCES)).thenReturn(publication);

        String expectedMessage = "someMessage";
        RuntimeException expectedException = new RuntimeException(expectedMessage);
        when(resourceService.updatePublication(any(Publication.class))).thenThrow(expectedException);

        try (var eventInputStream = IoUtils.inputStreamFromResources(OK_EVENT)) {
            var actualException = assertThrows(RuntimeException.class,
                                               () -> handler.handleRequest(eventInputStream, outputStream, context));
            assertThat(actualException.getMessage(), containsString(expectedMessage));
            assertThat(testAppender.getMessages(), containsString(expectedMessage));
        }
    }

    @Test
    void handleRequestSuccessfullyWhenPayloadContainsDoiUpdateHolderWithValidFields() throws ApiGatewayException {
        var publication = new Builder()
                              .withIdentifier(PUBLICATION_IDENTIFIER_IN_RESOURCES)
                              .build();

        var expectedPublicationUpdate = new Builder()
                                            .withIdentifier(PUBLICATION_IDENTIFIER_IN_RESOURCES)
                                            .withDoi(EXAMPLE_DOI)
                                            .withModifiedDate(EXAMPLE_DOI_MODIFIED_DATE)
                                            .build();

        stubSuccessfulDoiStatusUpdate(publication, expectedPublicationUpdate);

        var eventInputStream = IoUtils.inputStreamFromResources(OK_EVENT);
        handler.handleRequest(eventInputStream, outputStream, context);
        verifySuccessfulDoiStatusUpdate(expectedPublicationUpdate);
    }

    @Test
    void shouldRemoveDoiOnReceiptOfDoiUpdateWithoutDoiForPublicationWithDraftDoi() throws ApiGatewayException {
        var publication = new Builder()
                              .withIdentifier(PUBLICATION_IDENTIFIER_IN_RESOURCES)
                              .withDoi(EXAMPLE_DOI)
                              .withStatus(PublicationStatus.DRAFT)
                              .build();

        var expectedPublicationUpdate = new Builder()
                                            .withIdentifier(PUBLICATION_IDENTIFIER_IN_RESOURCES)
                                            .withStatus(PublicationStatus.DRAFT)
                                            .withModifiedDate(EXAMPLE_DOI_MODIFIED_DATE)
                                            .build();

        stubSuccessfulDoiStatusUpdate(publication, expectedPublicationUpdate);

        var eventInputStream = IoUtils.inputStreamFromResources(WITHOUT_DOI_EVENT);
        handler.handleRequest(eventInputStream, outputStream, context);

        verifySuccessfulDoiStatusUpdate(expectedPublicationUpdate);
    }

    @Test
    void handleRequestSuccessfullyThenLogsInformationMessage()
        throws ApiGatewayException {
        var publication = new Builder()
                              .withIdentifier(PUBLICATION_IDENTIFIER_IN_RESOURCES)
                              .build();

        var expectedPublicationUpdate = new Builder()
                                            .withIdentifier(PUBLICATION_IDENTIFIER_IN_RESOURCES)
                                            .withDoi(EXAMPLE_DOI)
                                            .withModifiedDate(EXAMPLE_DOI_MODIFIED_DATE)
                                            .build();

        stubSuccessfulDoiStatusUpdate(publication, expectedPublicationUpdate);

        var eventInputStream = IoUtils.inputStreamFromResources(OK_EVENT);
        handler.handleRequest(eventInputStream, outputStream, context);

        assertThat(logger.getMessages(), containsString(String.format(UpdateDoiStatusProcess.UPDATED_PUBLICATION_FORMAT,
                                                                      PUBLICATION_IDENTIFIER_IN_RESOURCES,
                                                                      EXAMPLE_DOI,
                                                                      EXAMPLE_DOI_MODIFIED_DATE
        )));
    }

    private void verifySuccessfulDoiStatusUpdate(Publication expectedPublicationUpdate) {
        ArgumentCaptor<Publication> publicationServiceCaptor = ArgumentCaptor.forClass(Publication.class);
        verify(resourceService).updatePublication(publicationServiceCaptor.capture());
        Publication actualPublicationUpdate = publicationServiceCaptor.getValue();
        assertThat(actualPublicationUpdate, is(equalTo(expectedPublicationUpdate)));
    }

    private void stubSuccessfulDoiStatusUpdate(Publication publication, Publication expectedPublicationUpdate)
        throws ApiGatewayException {
        when(resourceService.getPublicationByIdentifier(PUBLICATION_IDENTIFIER_IN_RESOURCES)).thenReturn(publication);
        when(resourceService.updatePublication(any(Publication.class))).thenReturn(expectedPublicationUpdate);
    }
}