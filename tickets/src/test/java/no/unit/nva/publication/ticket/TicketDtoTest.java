package no.unit.nva.publication.ticket;

import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import java.time.Clock;
import java.time.Instant;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import no.unit.nva.publication.ticket.test.TicketTestUtils;

class TicketDtoTest extends ResourcesLocalTest {

    private ResourceService resourceService;
    private TicketService ticketService;

    @BeforeEach
    public void setup() {
        super.init();
        this.resourceService = new ResourceService(client, Clock.systemDefaultZone());
        this.ticketService = new TicketService(client);
    }

    @ParameterizedTest
    @DisplayName("should include all publication summary fields")
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldIncludeAllPublicationSummaryFields(Class<? extends TicketEntry> ticketType, PublicationStatus status) throws ApiGatewayException {
        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        var ticket = TicketTestUtils.createNonPersistedTicket(publication, ticketType);
        var dto = TicketDto.fromTicket(ticket);
        var publicationSummary = dto.getPublicationSummary();
        assertThat(publicationSummary, doesNotHaveEmptyValues());
    }

    @ParameterizedTest(name = "should accept both date (legacy) and createdDate: {0}")
    @ValueSource(strings = {"date", "createdDate"})
    void shouldAcceptBothLegacyDateAndCreatedDate(String field) {
        var isoDateTime = "2022-12-01T11:07:32.039628Z";
        var input = JsonUtils.dtoObjectMapper.createObjectNode()
                        .put("type", MessageDto.TYPE)
                        .put(field, isoDateTime);

        var result = attempt(() -> JsonUtils.dtoObjectMapper.readValue(input.toString(), MessageDto.class))
                         .orElseThrow();

        assertThat(result.getCreatedDate(), is(equalTo(Instant.parse(isoDateTime))));
    }
}