package no.unit.nva.publication.storage;

import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Is.is;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.publication.model.storage.GeneralSupportRequestDao;
import no.unit.nva.publication.model.storage.TicketDao;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class GeneralSupportRequestDaoTest {

    @ParameterizedTest(name = "Should return GeneralSupportRequestDao when type is {0}")
    @ValueSource(strings = {"GeneralSupportRequest", "GeneralSupportCase"})
    void shouldAcceptLegacyAndCurrentTypeForGeneralSupportCase(String type) {
        var input = JsonUtils.dynamoObjectMapper.createObjectNode()
                        .put("type", type);

        var result =
            attempt(() -> JsonUtils.dynamoObjectMapper.readValue(input.toString(), TicketDao.class)).orElseThrow();

        assertThat(result, is(instanceOf(GeneralSupportRequestDao.class)));
    }

}
