package no.unit.nva.publication.service.impl;

import static java.net.HttpURLConnection.HTTP_CONFLICT;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static no.unit.nva.publication.ticket.test.TicketTestUtils.createPersistedPublication;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.net.URI;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import no.unit.nva.auth.uriretriever.UriRetriever;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.model.ResourceWithId;
import no.unit.nva.publication.model.SearchResourceApiResponse;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.SearchService.SearchServiceException;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SearchServiceTest extends ResourcesLocalTest {

    private UriRetriever uriRetriever;
    private ResourceService resourceService;
    private SearchService searchService;

    @BeforeEach
    public void setUp() {
        super.init();
        uriRetriever = mock(UriRetriever.class);
        resourceService = getResourceServiceBuilder().build();
        searchService = SearchService.create(uriRetriever, resourceService);
    }

    @Test
    void shouldReturnResourcesFetchedBySearchApiAndResourceService() throws ApiGatewayException {
        var publication = createPersistedPublication(PublicationStatus.PUBLISHED, resourceService);
        var searchParams = Map.of(randomString(), randomString());
        var id = createPublicationId(publication.getIdentifier());
        var responseBody = new SearchResourceApiResponse(1, List.of(new ResourceWithId(id)));
        var response = httpResponse(HTTP_OK, responseBody.toJsonString());
        when(uriRetriever.fetchResponse(any(), any())).thenReturn(Optional.of(response));

        var fetchedPublications = searchService.searchPublicationsByParam(searchParams);

        var resource = Resource.fromPublication(publication).fetch(resourceService).orElseThrow();
        assertThat(fetchedPublications, hasItem(resource));
    }

    @Test
    void shouldFilterOutPublicationsThatDoesNotLongerExist() throws ApiGatewayException {
        var existingPublication = createPersistedPublication(PublicationStatus.PUBLISHED, resourceService);
        var searchParams = Map.of(randomString(), randomString());
        var id = createPublicationId(existingPublication.getIdentifier());
        var responseBody = new SearchResourceApiResponse(1, List.of(new ResourceWithId(id)));
        var response = httpResponse(HTTP_OK, responseBody.toJsonString());
        when(uriRetriever.fetchResponse(any(), any())).thenReturn(Optional.of(response));
        resourceService = mock(ResourceService.class);
        when(resourceService.getPublicationByIdentifier(any())).thenThrow(NotFoundException.class);

        var fetchedPublications = SearchService.create(uriRetriever, resourceService).searchPublicationsByParam(searchParams);

        assertThat(fetchedPublications, is(emptyIterable()));
    }

    @Test
    void shouldThrowExceptionWhenCouldNotParseResponse() {
        var searchParams = Map.of(randomString(), randomString());
        var response = httpResponse(HTTP_NOT_FOUND, randomString());
        when(uriRetriever.fetchResponse(any(), any())).thenReturn(Optional.of(response));

        assertThrows(SearchServiceException.class, () -> searchService.searchPublicationsByParam(searchParams));
    }

    @Test
    void shouldThrowExceptionWhenResponseStatusCodeIsNotHttpOk() {
        var searchParams = Map.of(randomString(), randomString());
        var responseBody = new SearchResourceApiResponse(1, List.of(
            new ResourceWithId(createPublicationId(SortableIdentifier.next()))));
        var response = httpResponse(HTTP_CONFLICT, responseBody.toJsonString());
        when(uriRetriever.fetchResponse(any(), any())).thenReturn(Optional.of(response));

        assertThrows(SearchServiceException.class, () -> searchService.searchPublicationsByParam(searchParams));
    }

    private static URI createPublicationId(SortableIdentifier sortableIdentifier) {
        return UriWrapper.fromUri(randomUri()).addChild(sortableIdentifier.toString()).getUri();
    }

    private HttpResponse<String> httpResponse(int statusCode, String responseBody) {
        var response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(statusCode);
        when(response.body()).thenReturn(responseBody);
        return response;
    }
}