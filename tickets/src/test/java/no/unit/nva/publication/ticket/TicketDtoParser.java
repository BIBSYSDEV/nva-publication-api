package no.unit.nva.publication.ticket;

import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.GeneralSupportRequest;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.TicketStatus;
import no.unit.nva.publication.model.business.UnpublishRequest;

public final class TicketDtoParser {

    private TicketDtoParser() {

    }

    public static TicketEntry toTicket(TicketDto ticketDto) {
        if (ticketDto instanceof DoiRequestDto doiRequestDto) {
            return toTicket(doiRequestDto);
        } else if (ticketDto instanceof PublishingRequestDto publishingRequestDto) {
            return toTicket(publishingRequestDto);
        } else if (ticketDto instanceof GeneralSupportRequestDto generalSupportRequestDto) {
            return toTicket(generalSupportRequestDto);
        } else if (ticketDto instanceof UnpublishRequestDto unpublishRequestDto) {
            return toTicket(unpublishRequestDto);
        } else {
            return null;
        }
    }

    public static TicketEntry toTicket(GeneralSupportRequestDto generalSupportRequest) {
        var request = new GeneralSupportRequest();
        request.setIdentifier(generalSupportRequest.getIdentifier());
        request.setStatus(getTicketStatus(generalSupportRequest.getStatus()));
        request.setResourceIdentifier(generalSupportRequest.getPublicationIdentifier());
        request.setCreatedDate(generalSupportRequest.getCreatedDate());
        request.setModifiedDate(generalSupportRequest.getModifiedDate());
        request.setViewedBy(generalSupportRequest.getViewedBy());
        request.setAssignee(generalSupportRequest.getAssignee());
        request.setOwner(generalSupportRequest.getOwner());
        request.setOwnerAffiliation(generalSupportRequest.getOwnerAffiliation());
        return request;
    }

    public static TicketEntry toTicket(PublishingRequestDto publishingRequestDto) {
        var ticket = new PublishingRequestCase();
        ticket.setCreatedDate(publishingRequestDto.getCreatedDate());
        ticket.setStatus(getTicketStatus(publishingRequestDto.getStatus()));
        ticket.setModifiedDate(publishingRequestDto.getModifiedDate());
        ticket.setIdentifier(publishingRequestDto.getIdentifier());
        ticket.setResourceIdentifier(publishingRequestDto.getPublicationIdentifier());
        ticket.setViewedBy(publishingRequestDto.getViewedBy());
        ticket.setAssignee(publishingRequestDto.getAssignee());
        ticket.setOwner(publishingRequestDto.getOwner());
        ticket.setOwnerAffiliation(publishingRequestDto.getOwnerAffiliation());
        ticket.setFilesForApproval(publishingRequestDto.getFilesForApproval());
        return ticket;
    }

    public static TicketEntry toTicket(DoiRequestDto doiRequestDto) {
        var ticket = new DoiRequest();
        ticket.setCreatedDate(doiRequestDto.getCreatedDate());
        ticket.setStatus(getTicketStatus(doiRequestDto.getStatus()));
        ticket.setModifiedDate(doiRequestDto.getModifiedDate());
        ticket.setIdentifier(doiRequestDto.getIdentifier());
        ticket.setResourceIdentifier(doiRequestDto.getPublicationIdentifier());
        ticket.setViewedBy(doiRequestDto.getViewedBy());
        ticket.setAssignee(doiRequestDto.getAssignee());
        ticket.setOwner(doiRequestDto.getOwner());
        ticket.setOwnerAffiliation(doiRequestDto.getOwnerAffiliation());
        return ticket;
    }

    public static TicketEntry toTicket(UnpublishRequestDto unpublishRequest) {
        var request = new UnpublishRequest();
        request.setIdentifier(unpublishRequest.getIdentifier());
        request.setStatus(getTicketStatus(unpublishRequest.getStatus()));
        request.setResourceIdentifier(unpublishRequest.getPublicationIdentifier());
        request.setCreatedDate(unpublishRequest.getCreatedDate());
        request.setModifiedDate(unpublishRequest.getModifiedDate());
        request.setViewedBy(unpublishRequest.getViewedBy());
        request.setAssignee(unpublishRequest.getAssignee());
        request.setOwner(unpublishRequest.getOwner());
        request.setOwnerAffiliation(unpublishRequest.getOwnerAffiliation());
        return request;
    }

    private static TicketStatus getTicketStatus(TicketDtoStatus ticketDtoStatus) {
        if (TicketDtoStatus.NEW.equals(ticketDtoStatus)) {
            return TicketStatus.PENDING;
        }
        return TicketStatus.parse(ticketDtoStatus.toString());
    }
}
