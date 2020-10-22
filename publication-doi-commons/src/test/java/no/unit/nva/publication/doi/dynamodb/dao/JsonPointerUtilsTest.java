package no.unit.nva.publication.doi.dynamodb.dao;

import static no.unit.nva.publication.doi.JsonPointerUtils.textFromNode;
import static nva.commons.utils.JsonUtils.objectMapper;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

class JsonPointerUtilsTest {

    private static final String EXAMPLE_FIELD_WITH_VALUE = "fieldWithValue";
    private static final String EXAMPLE_FIELD_WITH_NO_VALUE = "fieldWithoutValue";
    private static final String EXAMPLE_VALUE = "Blub blub";
    private static final ObjectNode simpleNodeWithExampleField = objectMapper.createObjectNode().put(
        EXAMPLE_FIELD_WITH_VALUE, EXAMPLE_VALUE);
    private static final JsonPointer NON_EXISTING_POINTER = JsonPointer.compile("/nonExisting");
    private static final String ROOT = "/";
    private static final JsonPointer VALID_POINTER = JsonPointer.compile(ROOT + EXAMPLE_FIELD_WITH_VALUE);
    private static final JsonPointer VALID_POINTER_BUT_NO_VALUE = JsonPointer.compile(
        ROOT + EXAMPLE_FIELD_WITH_NO_VALUE);

    @Test
    void textFromNodeReturnsNullWhenPointerIsNotMatchingAnything() {
        assertThat(textFromNode(simpleNodeWithExampleField, NON_EXISTING_POINTER), is(nullValue()));
    }

    @Test
    void textFromNodeReturnsStringWhenValidPointerAndObjectHasValue() {
        assertThat(textFromNode(simpleNodeWithExampleField, VALID_POINTER), is(notNullValue()));
    }

    @Test
    void textFromNodeReturnsNullWhenValidPointerButNoValue() {
        assertThat(textFromNode(simpleNodeWithExampleField, VALID_POINTER_BUT_NO_VALUE), is(nullValue()));
    }
}