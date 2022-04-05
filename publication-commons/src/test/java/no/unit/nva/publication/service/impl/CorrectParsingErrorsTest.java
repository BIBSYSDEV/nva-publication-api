package no.unit.nva.publication.service.impl;

import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.publication.service.impl.CorrectParsingErrors.correctParsingErrors;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import org.junit.jupiter.api.Test;

public class CorrectParsingErrorsTest {

    public static final String PUBLICATION_BEFORE_JSON = "src/test/resources/migration/publication_before.json";
    public static final String PUBLICATION_AFTER_JSON = "src/test/resources/migration/publication_after.json";

    @Test
    void shouldCorrectParsingErrors() throws Exception {
        ObjectNode actual = (ObjectNode) dtoObjectMapper.readTree(new File(PUBLICATION_BEFORE_JSON));
        ObjectNode expected = (ObjectNode) dtoObjectMapper.readTree(new File(PUBLICATION_AFTER_JSON));

        correctParsingErrors(actual);

        assertThat(actual, is(equalTo(expected)));
    }
}
