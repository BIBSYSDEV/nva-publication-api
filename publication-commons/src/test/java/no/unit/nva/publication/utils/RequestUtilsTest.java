package no.unit.nva.publication.utils;

import static java.util.Objects.nonNull;
import static no.unit.nva.model.PublicationStatus.PUBLISHED;
import static no.unit.nva.model.testing.PublicationGenerator.fromInstanceClassesExcluding;
import static no.unit.nva.publication.utils.RequestUtils.PUBLICATION_IDENTIFIER;
import static no.unit.nva.publication.utils.RequestUtils.TICKET_IDENTIFIER;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.apigateway.AccessRight.MANAGE_DOI;
import static nva.commons.apigateway.AccessRight.MANAGE_PUBLISHING_REQUESTS;
import static nva.commons.apigateway.AccessRight.MANAGE_RESOURCES_STANDARD;
import static nva.commons.apigateway.AccessRight.SUPPORT;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.Username;
import no.unit.nva.publication.external.services.UriRetriever;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.GeneralSupportRequest;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UnpublishRequest;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permission.strategy.PermissionStrategy;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class RequestUtilsTest {

    private ResourceService resourceService;
    private UriRetriever uriRetriever;

    @BeforeEach
    public void setup() {
        this.resourceService = mock(ResourceService.class);
        this.uriRetriever = mock(UriRetriever.class);
    }

    public static Stream<Arguments> ticketTypeAndAccessRightProvider() {
        return Stream.of(Arguments.of(PUBLISHED, DoiRequest.class, MANAGE_DOI),
                         Arguments.of(PublicationStatus.DRAFT, PublishingRequestCase.class,
                                      MANAGE_PUBLISHING_REQUESTS),
                         Arguments.of(PUBLISHED, UnpublishRequest.class, MANAGE_PUBLISHING_REQUESTS),
                         Arguments.of(PUBLISHED, GeneralSupportRequest.class, SUPPORT));
    }

    @Test
    void shouldReturnFalseWhenCheckingAuthorizationForNullTicket() throws UnauthorizedException {
        Assertions.assertFalse(
            RequestUtils.fromRequestInfo(mockedRequestInfo()).isAuthorizedToManage(null));
    }

    @Test
    void shouldThrowIllegalArgumentWhenExtractingMissingPathParamAsIdentifier() throws UnauthorizedException {
        var requestInfo = mockedRequestInfoWithoutPathParams();

        assertThrows(IllegalArgumentException.class,
                     () -> RequestUtils.fromRequestInfo(requestInfo).publicationIdentifier());
        assertThrows(IllegalArgumentException.class,
                     () -> RequestUtils.fromRequestInfo(requestInfo).ticketIdentifier());
    }

    @Test
    void shouldReturnIdentifiersFromPathParamsWhenTheyArePresent() throws UnauthorizedException {
        var requestUtils = RequestUtils.fromRequestInfo(mockedRequestInfo());

        Assertions.assertTrue(nonNull(requestUtils.publicationIdentifier()));
        Assertions.assertTrue(nonNull(requestUtils.ticketIdentifier()));
    }

    @ParameterizedTest
    @MethodSource("ticketTypeAndAccessRightProvider")
    void shouldReturnTrueWhenUserHasAccessRightToManageTicket(PublicationStatus publicationStatus,
                                                              Class<? extends TicketEntry> ticketType,
                                                              AccessRight accessRight)
        throws UnauthorizedException, NotFoundException {
        var publication = publicationWithOwner(randomString()).copy().withStatus(publicationStatus).build();
        var requestInfo = requestInfoWithAccessRight(publication.getResourceOwner().getOwnerAffiliation(), accessRight);
        var ticket = TicketEntry.requestNewTicket(publication, ticketType);
        var requestUtils = RequestUtils.fromRequestInfo(requestInfo);

        when(resourceService.getPublicationByIdentifier(any())).thenReturn(publication);

        Assertions.assertTrue(requestUtils.isAuthorizedToManage(ticket));
    }

    @Test
    void shouldReturnTrueWhenUserIsTicketOwner() throws UnauthorizedException {
        var requestInfo = mockedRequestInfo();
        var ticket = TicketEntry.requestNewTicket(publicationWithOwner(requestInfo.getUserName()), DoiRequest.class);
        var requestUtils = RequestUtils.fromRequestInfo(requestInfo);

        Assertions.assertTrue(requestUtils.isTicketOwner(ticket));
    }

    @Test
    void shouldConvertRequestUtilToUserInstance() throws UnauthorizedException {
        var requestInfo = mockedRequestInfo();

        var expectedUserInstance = new UserInstance(requestInfo.getUserName(),
                                                    requestInfo.getCurrentCustomer(),
                                                    requestInfo.getTopLevelOrgCristinId().orElseThrow(),
                                                    requestInfo.getPersonCristinId(),
                                                    requestInfo.getAccessRights());
        var createdUserInstance = RequestUtils.fromRequestInfo(requestInfo).toUserInstance();
        Assertions.assertEquals(createdUserInstance, expectedUserInstance);
    }

    @Test
    void shouldReturnTrueWhenUserHasOneOfAccessRights() throws UnauthorizedException {
        var requestInfo = requestInfoWithAccessRight(randomUri(), MANAGE_DOI);

        Assertions.assertTrue(RequestUtils.fromRequestInfo(requestInfo)
                                  .hasOneOfAccessRights(MANAGE_DOI, MANAGE_PUBLISHING_REQUESTS));
    }

    private static Publication publicationWithOwner(String owner) {
        return fromInstanceClassesExcluding(PermissionStrategy.PROTECTED_DEGREE_INSTANCE_TYPES).copy()
                   .withStatus(PUBLISHED)
                   .withResourceOwner(new ResourceOwner(new Username(owner), randomUri())).build();
    }

    private RequestInfo requestInfoWithAccessRight(URI customer, AccessRight accessRight) throws UnauthorizedException {
        var requestInfo = mock(RequestInfo.class);
        when(requestInfo.getAccessRights()).thenReturn(List.of(MANAGE_RESOURCES_STANDARD, accessRight));
        when(requestInfo.getCurrentCustomer()).thenReturn(customer);
        when(requestInfo.getTopLevelOrgCristinId()).thenReturn(Optional.of(customer));
        return requestInfo;
    }

    private static RequestInfo mockedRequestInfo() throws UnauthorizedException {
        var requestInfo = mock(RequestInfo.class);
        when(requestInfo.getCurrentCustomer()).thenReturn(randomUri());
        when(requestInfo.getUserName()).thenReturn(randomString());
        when(requestInfo.getPathParameters()).thenReturn(Map.of(PUBLICATION_IDENTIFIER, randomSortableIdentifier(),
                                                                TICKET_IDENTIFIER, randomSortableIdentifier()));
        when(requestInfo.getAccessRights()).thenReturn(List.of());
        when(requestInfo.getPersonCristinId()).thenReturn(randomUri());
        when(requestInfo.getTopLevelOrgCristinId()).thenReturn(Optional.of(randomUri()));
        return requestInfo;
    }

    private static RequestInfo mockedRequestInfoWithoutPathParams() throws UnauthorizedException {
        var requestInfo = mock(RequestInfo.class);
        when(requestInfo.getCurrentCustomer()).thenReturn(randomUri());
        when(requestInfo.getUserName()).thenReturn(randomString());
        when(requestInfo.getPathParameters()).thenReturn(Map.of());
        when(requestInfo.getAccessRights()).thenReturn(List.of());
        when(requestInfo.getPersonCristinId()).thenReturn(randomUri());
        when(requestInfo.getTopLevelOrgCristinId()).thenReturn(Optional.of(randomUri()));
        return requestInfo;
    }

    private static String randomSortableIdentifier() {
        return SortableIdentifier.next().toString();
    }
}