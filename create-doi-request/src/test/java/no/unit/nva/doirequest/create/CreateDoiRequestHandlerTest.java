package no.unit.nva.doirequest.create;

import static nva.commons.core.JsonUtils.objectMapper;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.PublicationGenerator;
import no.unit.nva.publication.RequestUtil;
import no.unit.nva.publication.exception.InvalidPublicationException;
import no.unit.nva.publication.service.ResourcesDynamoDbLocalTest;
import no.unit.nva.publication.service.impl.DoiRequestService;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.UserInstance;
import no.unit.nva.publication.storage.model.DoiRequest;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zalando.problem.Problem;

public class CreateDoiRequestHandlerTest extends ResourcesDynamoDbLocalTest {

    public static final String HTTP_PATH_SEPARATOR = "/";
    public static final String NOT_THE_RESOURCE_OWNER = "someOther@owner.org";
    private static final Instant PUBLICATION_CREATION_TIME = Instant.parse("2010-01-01T10:15:30.00Z");
    private static final Instant PUBLICATION_UPDATE_TIME = Instant.parse("2011-02-02T10:15:30.00Z");
    private static final Instant DOI_REQUEST_CREATION_TIME = Instant.parse("2012-02-02T10:15:30.00Z");
    private static final Instant DOI_REQUEST_UPDATE_TIME = Instant.parse("2013-02-02T10:15:30.00Z");
    private CreateDoiRequestHandler handler;
    private ResourceService resourceService;
    private Clock mockClock;
    private ByteArrayOutputStream outputStream;
    private Context context;
    private DoiRequestService doiRequestService;

    @BeforeEach
    public void initialize() {
        init();
        setupClock();
        resourceService = new ResourceService(client, mockClock);
        doiRequestService = new DoiRequestService(client, mockClock);
        outputStream = new ByteArrayOutputStream();
        context = mock(Context.class);
        Environment environment = mock(Environment.class);
        when(environment.readEnv(anyString())).thenReturn("*");
        handler = new CreateDoiRequestHandler(doiRequestService, environment);
    }

    @Test
    public void createDoiRequestStoresNewDoiRequestForPublishedResource()
        throws ConflictException, NotFoundException, InvalidPublicationException, IOException {
        Publication publication = createPublication();
        resourceService.publishPublication(publication);

        CreateDoiRequestRequest request = new CreateDoiRequestRequest(publication.getIdentifier(), null);
        InputStream inputStream = new HandlerRequestBuilder<CreateDoiRequestRequest>(objectMapper)
            .withCustomerId(publication.getPublisher().getId().toString())
            .withFeideId(publication.getOwner())
            .withBody(request)
            .build();

        handler.handleRequest(inputStream, outputStream, context);

        GatewayResponse<Void> response = GatewayResponse.fromOutputStream(outputStream);
        String doiRequestIdentifier = extractLocationHeader(response);

        DoiRequest doiRequest = readDoiRequestDirectlyFromService(publication, doiRequestIdentifier);

        assertThat(doiRequest, is(not(nullValue())));
    }

    @Test
    public void createDoiRequestReturnsErrorWhenUserTriesToCreateDoiRequestOnNotOwnedPublication()
        throws ConflictException, NotFoundException, InvalidPublicationException, IOException {
        Publication publication = createPublication();
        resourceService.publishPublication(publication);

        CreateDoiRequestRequest request = new CreateDoiRequestRequest(publication.getIdentifier(), null);
        InputStream inputStream = new HandlerRequestBuilder<CreateDoiRequestRequest>(objectMapper)
            .withCustomerId(publication.getPublisher().getId().toString())
            .withFeideId(NOT_THE_RESOURCE_OWNER)
            .withPathParameters(Map.of(RequestUtil.IDENTIFIER, publication.getIdentifier().toString()))
            .withBody(request)
            .build();

        handler.handleRequest(inputStream, outputStream, context);

        GatewayResponse<Problem> response = GatewayResponse.fromOutputStream(outputStream);
        assertThat(response.getStatusCode(), is(HttpURLConnection.HTTP_BAD_REQUEST));
    }

    private DoiRequest readDoiRequestDirectlyFromService(Publication publication, String doiRequestIdentifier) {
        UserInstance userInstance = new UserInstance(publication.getOwner(), publication.getPublisher().getId());

        DoiRequest doiRequest = doiRequestService.getDoiRequest(userInstance, new SortableIdentifier(
            doiRequestIdentifier));
        return doiRequest;
    }

    private String extractLocationHeader(GatewayResponse<Void> response) {
        //TODO: replace magic string with nva-commons headers when headers have been updated.
        String locationHeader = response.getHeaders().get("Location");
        String[] headerArray = locationHeader.split(HTTP_PATH_SEPARATOR);
        return headerArray[headerArray.length - 1];
    }

    private void setupClock() {
        mockClock = mock(Clock.class);
        when(mockClock.instant())
            .thenReturn(PUBLICATION_CREATION_TIME)
            .thenReturn(PUBLICATION_UPDATE_TIME)
            .thenReturn(DOI_REQUEST_CREATION_TIME)
            .thenReturn(DOI_REQUEST_UPDATE_TIME);
    }

    private Publication createPublication() throws ConflictException {
        return resourceService.createPublication(PublicationGenerator.publicationWithoutIdentifier());
    }
}
