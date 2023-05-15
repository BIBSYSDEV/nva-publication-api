package no.unit.nva.publication.ticket;

import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import java.time.Instant;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.publication.service.ResourcesLocalTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TicketDtoTest extends ResourcesLocalTest {

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
}