package no.unit.nva.publication.update;

import static no.unit.nva.model.PublicationOperation.REPUBLISH;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permissions.publication.PublicationPermissions;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.NotFoundException;
import org.junit.jupiter.api.Test;

class RepublishUtilTest extends ResourcesLocalTest {

    @Test
    void shouldThrowNotFoundExceptionWhenResourceIsNotFound() throws ApiGatewayException {
        var publication = randomPublication().copy().withStatus(PublicationStatus.UNPUBLISHED).build();
        var userInstance = UserInstance.fromPublication(publication);

        var resourceService = mock(ResourceService.class);
        var permissionStrategy = mock(PublicationPermissions.class);
        var ticketService = mock(TicketService.class);
        var republishUtil = RepublishUtil.create(resourceService, ticketService, permissionStrategy);

        when(permissionStrategy.allowsAction(REPUBLISH)).thenReturn(true);
        doNothing().when(resourceService).updateResource(any());
        when(resourceService.getResourceByIdentifier(publication.getIdentifier()))
            .thenReturn(Resource.fromPublication(publication))
            .thenThrow(NotFoundException.class);

        assertThrows(NotFoundException.class, () -> republishUtil.republish(publication, userInstance));
    }
}