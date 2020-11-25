package no.unit.nva.publication.doi.dto;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.javafaker.Faker;
import java.util.List;
import nva.commons.utils.JsonUtils;
import org.junit.jupiter.api.Test;

class PublicationDateTest {

    private static final ObjectMapper objectMapper = JsonUtils.objectMapper;
    private static final Faker faker = new Faker();

    @Test
    void testJsonNodeConstructor() {
        var actual = PublicationDate.fromJsonNode(getPublicationWithDate());
        assertThat(actual.getYear(), is(equalTo("1999")));
        assertThat(actual.getMonth(), is(equalTo("07")));
        assertThat(actual.getDay(), is(equalTo("09")));
    }

    private ObjectNode getPublicationWithDate() {
        var date = objectMapper.createObjectNode();
        var dateMap = date.putObject("date").putObject("m");
        dateMap.putObject("year").put("s", "1999");
        dateMap.putObject("month").put("s", "07");
        dateMap.putObject("day").put("s", "09");
        return date;
    }

    private ObjectNode getPublicationRandomMissingYearMonthOrDay() {
        var date = objectMapper.createObjectNode();
        var dateMap = date.putObject("date").putObject("m");
        dateMap.putObject("year").put("s", "1999");
        dateMap.putObject("month").put("s", "07");
        dateMap.putObject("day").put("s", "09");
        var fieldToRemove = faker.options().nextElement(List.of("year", "month", "day"));
        assert ((ObjectNode) date.get("date").get("m")).remove(fieldToRemove)
            != null : "Should find field " + fieldToRemove + " to remove.";
        return date;
    }

    private JsonNode getPublicationWithMissingYearMonthAndDay() {
        var objectNode = objectMapper.createObjectNode();
        return objectNode.putObject("date");
    }
}