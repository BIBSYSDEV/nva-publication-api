package no.unit.nva.publication.ticket;

import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.GeneralSupportRequest;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.TicketStatus;

public final class TicketDtoParser {

    private TicketDtoParser() {

    }

    public static TicketEntry toTicket(TicketDto ticketDto) {
        if (ticketDto instanceof DoiRequestDto) {
            return toTicket((DoiRequestDto) ticketDto);
        }
        if (ticketDto instanceof PublishingRequestDto) {
            return toTicket((PublishingRequestDto) ticketDto);
        }
        if (ticketDto instanceof GeneralSupportRequestDto) {
            return toTicket((GeneralSupportRequestDto) ticketDto);
        }
        return null;
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
        return ticket;
    }

    private static TicketStatus getTicketStatus(TicketDtoStatus ticketDtoStatus) {
        if (TicketDtoStatus.NEW.equals(ticketDtoStatus)) {
            return TicketStatus.PENDING;
        }
        return TicketStatus.parse(ticketDtoStatus.toString());
    }
}