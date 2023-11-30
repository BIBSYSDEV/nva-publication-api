package no.sikt.nva.scopus.conversion;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import com.fasterxml.jackson.core.JsonProcessingException;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.expansion.model.cristin.CristinOrganization;
import org.junit.jupiter.api.Test;

public class CristinOrganizationTest {

    @Test
    void shouldReturnTopLevelOrgWhenCristinOrgTopLevelOrg() throws JsonProcessingException {
        var org = JsonUtils.dtoObjectMapper.readValue("{\n"
                                                      + "    \"id\": \"https://api.dev.nva.aws.unit.no/cristin/organization/13907425.0.0.0\",\n"
                                                      + "    \"@context\": \"https://bibsysdev.github.io/src/organization-context.json\",\n"
                                                      + "    \"type\": \"Organization\",\n"
                                                      + "    \"partOf\": [],\n"
                                                      + "    \"country\": \"GB\",\n"
                                                      + "    \"labels\": {\n"
                                                      + "        \"nb\": \"Wellcome Trust Sanger Institute\"\n"
                                                      + "    }\n"
                                                      + "}", CristinOrganization.class);

        assertThat(org.id(), is(equalTo(org.getTopLevelOrg().id())));
    }
}
