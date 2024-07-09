package no.unit.nva.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import no.unit.nva.commons.json.JsonUtils;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;

@Deprecated
class ApprovalTest {
    @Test
    void shouldMigrateOldStyleDatesToApprovalDate() throws JsonProcessingException {
        var body = "{\"type\": \"Approval\", \"date\": \"2004-08-05T18:57:20.198Z\"}";
        var actual = JsonUtils.dtoObjectMapper.readValue(body, Approval.class);
        assertThat(actual.getApprovalDate(), is(not(nullValue())));
    }
}