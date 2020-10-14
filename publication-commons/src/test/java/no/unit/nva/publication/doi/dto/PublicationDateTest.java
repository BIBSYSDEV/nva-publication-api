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
    private static Faker faker = new Faker();

    @Test
    void testJsonNodeConstructor() {
        var actual = new PublicationDate(getPublicationWithDate());
        assertThat(actual.getYear(), is(equalTo("1999")));
        assertThat(actual.getMonth(), is(equalTo("07")));
        assertThat(actual.getDay(), is(equalTo("09")));
    }

    private ObjectNode getPublicationWithDate() {
        var publicationWithDate = objectMapper.createObjectNode();
        publicationWithDate.putObject("publication_date")
            .put("year", "1999")
            .put("month", "07")
            .put("day", "09");
        return publicationWithDate;
    }

    private ObjectNode getPublicationRandomMissingYearMonthOrDay() {
        var publicationWithDate = objectMapper.createObjectNode();
        publicationWithDate.putObject("publication_date")
            .put("year", "1999")
            .put("month", "07")
            .put("day", "09");
        var fieldToRemove = faker.options().nextElement(List.of("year", "month", "day"));
        assert ((ObjectNode) publicationWithDate.get("publication_date")).remove(fieldToRemove)
            != null : "Should find field " + fieldToRemove + " to remove.";
        return publicationWithDate;
    }

    @Test
    void testIsPopulated() {
        var actual = new PublicationDate(getPublicationWithDate());
        assertThat(actual.isPopulated(), is(true));

        actual = new PublicationDate(getPublicationRandomMissingYearMonthOrDay());
        assertThat(actual.isPopulated(), is(true));

        actual = new PublicationDate(getPublicationWithMissingYearMonthAndDay());
        assertThat(actual.isPopulated(), is(false));
    }

    private JsonNode getPublicationWithMissingYearMonthAndDay() {
        var objectNode = objectMapper.createObjectNode();
        return objectNode.putObject("publication_date");
    }
}