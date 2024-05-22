package no.unit.nva.publication.ticket;

import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValuesIgnoringFields;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import java.time.Instant;
import java.util.Set;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.Username;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.GeneralSupportRequest;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UnpublishRequest;
import no.unit.nva.publication.ticket.test.TicketTestUtils;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TicketDtoTest extends TicketTestLocal {

    public static final Set<String> DTO_FIELDS_TO_IGNORE = Set.of("messages", "workflow", "approvedFiles");

    @BeforeEach
    public void setup() {
        super.init();
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

    @ParameterizedTest
    @ValueSource(classes = {
        DoiRequest.class,
        GeneralSupportRequest.class,
        PublishingRequestCase.class,
        UnpublishRequest.class
    })
    void shouldSerializeAllFieldsWhenConvertingToTicketDto(Class<? extends TicketEntry> ticketType) throws ApiGatewayException {
        var publication = TicketTestUtils.createPersistedPublication(PublicationStatus.PUBLISHED, resourceService);
        var ticket = TicketTestUtils.createCompletedTicket(publication, ticketType, ticketService);
        ticket.setOwnerAffiliation(randomUri());
        ticket.setAssignee(new Username(randomString()));

        var dto = TicketDto.fromTicket(ticket);
        assertThat(dto, doesNotHaveEmptyValuesIgnoringFields(DTO_FIELDS_TO_IGNORE));
    }
}