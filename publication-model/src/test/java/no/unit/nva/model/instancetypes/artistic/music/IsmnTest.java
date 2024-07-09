package no.unit.nva.model.instancetypes.artistic.music;

import static no.unit.nva.model.instancetypes.artistic.music.Ismn.INVALID_ISMN_TEMPLATE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import no.unit.nva.commons.json.JsonUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class IsmnTest {

    public static final String DEFAULT_ISMN_SEPARATOR = "-";
    public static final String NO_SEPARATOR = "";
    public static final String SPACE = " ";
    private static final String VALID_ISMN_10 = "M-2306-7118-7";
    private static final String VALID_ISMN_13 = "979-0-9016791-7-7";
    private static final String INVALID_CHECKBIT_ISMN = "979-0-9016791-72";
    private static final String INVALID_SHORT_LENGTH_ISMN = "979-0-9016791";
    private static final String INVALID_LONG_LENGTH_ISMN = "979-0-9016791-1233";
    private static final String INVALID_PREFIX_ISMN = "929-0-9016791-7";

    @Test
    void shouldValidateValidIsmn13() {
        assertDoesNotThrow(() -> new Ismn(VALID_ISMN_13));
    }

    @Test
    void musicNotationCanBeCreatedWhenInputIsValidIsmn10() {
        assertDoesNotThrow(() -> new Ismn(VALID_ISMN_10));
    }

    @ParameterizedTest(name = "ISMNs like {0} throw InvalidIsmnException")
    @ValueSource(strings = {
        INVALID_PREFIX_ISMN,
        INVALID_SHORT_LENGTH_ISMN,
        INVALID_LONG_LENGTH_ISMN,
        INVALID_CHECKBIT_ISMN,
        "X-981-1-1234-1",
        "9876-1234-1",
        "111",
        "M-100",
        "M-1101010101000",
        "Madmadworl",
        "M is a letter",
        "979-1-32111-122-1",
        "",
        " ",
        "    "
    })
    void shouldThrowExceptionsWhenIsmsAreInvalid(String candidate) {
        var exception = assertThrows(InvalidIsmnException.class, () -> new Ismn(candidate));
        var expectedMessage = String.format(INVALID_ISMN_TEMPLATE, candidate);
        assertThat(exception.getMessage(), containsString(expectedMessage));
    }

    @ParameterizedTest(name = "Music notation getIsmn reformats {0} correctly")
    @ValueSource(strings = {
        "M-001-12050-0",
        "M-004-16663-5",
        "M-049-05851-3",
        "M-051-66073-5",
        "M-2306-2632-3",
        "M-706871-19-6",
        "M-708010-34-0",
        "M-9001001-2-2",
        "979-0-001-16094-0",
        "979-0-001-16093-3",
        "979-0-008-00281-6",
        "979-0-035-22568-4",
        "979-0-047-30105-5",
        "979-0-2152-1558-0",
        "979-0-2312-0112-3",
        "979-0-3007-5976-0",
        "979-0-50057-152-0",
        "979-0-50224-227-5",
        "979-0-56005-291-5",
        "979-0-69795-559-2",
        "979-0-708146-03-2",
        "979-0-708024-09-5",
        "979-0-800059-00-1",
        "979-0-9002305-4-6",
        "979-0-9002013-3-1",
    })
    void shouldReturnIsmnStrippedOfAllSeparators(String candidate) throws InvalidIsmnException {
        var expectedFormat = candidate.replaceAll(DEFAULT_ISMN_SEPARATOR, NO_SEPARATOR);
        var candidateWithSpaces = candidate.replaceAll(DEFAULT_ISMN_SEPARATOR, SPACE);
        assertThat(new Ismn(candidate).toString(), is(equalTo(expectedFormat)));
        assertThat(new Ismn(candidateWithSpaces).toString(), is(equalTo(expectedFormat)));
    }

    @Test
    void shouldSerializeAsTypedObject() throws InvalidIsmnException, JsonProcessingException {
        var ismn = new Ismn(VALID_ISMN_13);
        var expectedJson = createExpectedJson(ismn);
        var actualJsonString = JsonUtils.dtoObjectMapper.writeValueAsString(ismn);
        var actualJson = JsonUtils.dtoObjectMapper.readTree(actualJsonString);
        assertThat(actualJson, is(equalTo(expectedJson)));
    }

    @Test
    void musicNotationReturnsEmptyIsmnWhenIsmnIsNull() {
        assertDoesNotThrow(() -> new Ismn(null));
    }

    private ObjectNode createExpectedJson(Ismn ismn) {
        var expectedJson = JsonUtils.dtoObjectMapper.createObjectNode();
        expectedJson.put("type", Ismn.class.getSimpleName());
        expectedJson.put("value", ismn.value());
        expectedJson.put("formatted", ismn.formatted());
        return expectedJson;
    }
}