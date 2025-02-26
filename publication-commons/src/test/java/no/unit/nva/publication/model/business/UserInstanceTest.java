package no.unit.nva.publication.model.business;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.mock;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.net.http.HttpClient;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.ticket.test.TicketTestUtils;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiIoException;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class UserInstanceTest {
    
    @Test
    void shouldReturnUserInstanceFromPublication() {
        Publication publication = PublicationGenerator.randomPublication();
        var userInstance = UserInstance.fromPublication(publication);
        assertThat(userInstance.getUsername(), is(equalTo(publication.getResourceOwner().getOwner().getValue())));
        assertThat(userInstance.getCustomerId(), is(equalTo(publication.getPublisher().getId())));
    }

    @ParameterizedTest
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldReturnUserInstanceFromMessage(Class<? extends TicketEntry> ticketType, PublicationStatus status) {
        var publication = TicketTestUtils.createNonPersistedPublication(status);
        var ticket = TicketEntry.requestNewTicket(publication, ticketType)
                         .withOwner(UserInstance.fromPublication(publication).getUsername());
        var message = Message.create(ticket, UserInstance.fromTicket(ticket), randomString());
        var userInstance = UserInstance.fromMessage(message);
        assertThat(userInstance.getUsername(), is(equalTo(publication.getResourceOwner().getOwner().getValue())));
        assertThat(userInstance.getCustomerId(), is(equalTo(publication.getPublisher().getId())));
    }
    
    @Test
    void shouldReturnUserInstanceFromRequestInfo() throws JsonProcessingException, UnauthorizedException,
                                                          ApiIoException {
        var customerId = randomUri();
        var username = randomString();
        var httpRequest = new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper)
                              .withCurrentCustomer(customerId)
                              .withNvaUsername(username)
                              .build();
        var requestInfo = RequestInfo.fromRequest(httpRequest, mock(HttpClient.class));
        var userInstance = UserInstance.fromRequestInfo(requestInfo);
        assertThat(userInstance.getUsername(), is(equalTo(username)));
        assertThat(userInstance.getCustomerId(), is(equalTo(customerId)));
    }
}