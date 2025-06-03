package no.unit.nva.publication.ticket.create;

import static no.unit.nva.publication.ticket.test.TicketTestUtils.createPersistedDegreePublication;
import static no.unit.nva.publication.ticket.test.TicketTestUtils.createPersistedPublication;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.util.List;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.model.business.FilesApprovalThesis;
import no.unit.nva.publication.model.business.PublishingWorkflow;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permissions.ticket.TicketPermissions;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.ticket.FilesApprovalThesisDto;
import no.unit.nva.publication.ticket.PublishingRequestDto;
import no.unit.nva.publication.ticket.TicketTestLocal;
import no.unit.nva.publication.utils.RequestUtils;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.ForbiddenException;
import nva.commons.apigateway.exceptions.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TicketResolverTest extends TicketTestLocal {

    private TicketResolver ticketResolver;
    private ResourceService resourceService;

    @BeforeEach
    public void setup() {
        super.init();
        resourceService = getResourceServiceBuilder().build();
        ticketResolver = new TicketResolver(resourceService, getTicketService());
    }

    @Test
    void shouldThrowForbiddenExceptionWhenCreatingPublishingRequest() throws ApiGatewayException {
        var publication = createPersistedPublication(PublicationStatus.PUBLISHED, resourceService);
        var requestUtils = createRequestUtils(publication);

        assertThrows(ForbiddenException.class,
                     () -> ticketResolver.resolveAndPersistTicket(PublishingRequestDto.empty(), requestUtils));
    }

    @Test
    void shouldThrowForbiddenExceptionWhenCreatingFilesApprovalThesis() throws ApiGatewayException {
        var publication = createPersistedDegreePublication(PublicationStatus.PUBLISHED, resourceService);
        var requestUtils = createRequestUtils(publication);
        var ticket = FilesApprovalThesis.createForUserInstitution(Resource.fromPublication(publication),
                                                                  UserInstance.fromPublication(publication),
                                                                  PublishingWorkflow.REGISTRATOR_PUBLISHES_METADATA_ONLY);
        assertThrows(ForbiddenException.class,
                     () -> ticketResolver.resolveAndPersistTicket(FilesApprovalThesisDto.fromTicket(ticket, List.of(),
                                                                                                    List.of(),
                                                                                                    mock(
                                                                                                        TicketPermissions.class)),
                                                                  requestUtils));
    }

    private RequestUtils createRequestUtils(Publication publication) throws NotFoundException {
        var requestUtils = mock(RequestUtils.class);
        when(requestUtils.publicationIdentifier()).thenReturn(publication.getIdentifier());
        when(requestUtils.toUserInstance()).thenReturn(UserInstance.fromPublication(publication));
        return requestUtils;
    }
}