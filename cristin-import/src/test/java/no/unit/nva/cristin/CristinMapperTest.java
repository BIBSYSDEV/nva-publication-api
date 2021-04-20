package no.unit.nva.cristin;

import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.Publication;
import nva.commons.core.JsonUtils;
import nva.commons.core.attempt.Try;
import nva.commons.core.ioutils.IoUtils;
import org.junit.jupiter.api.Test;

public class CristinMapperTest {

    public static final String SAMPLE_INPUT_01 = "input01.gz";
    private static final Integer NUMBER_OF_LINES_IN_RESOURCES_FILE = 100;

    @Test
    public void mapReturnsResourceWithIdentifierEqualToCristinObjectId() throws IOException {
        Set<String> expectedIds = cristinObjects().map(CristinObject::getId).collect(Collectors.toSet());

        Set<String> actualIds = cristinObjects()
                                    .map(CristinMapper::new)
                                    .map(CristinMapper::generatePublication)
                                    .map(Publication::getAdditionalIdentifiers)
                                    .flatMap(Collection::stream)
                                    .map(AdditionalIdentifier::getValue)
                                    .collect(Collectors.toSet());

        assertThat(expectedIds.size(), is(equalTo(NUMBER_OF_LINES_IN_RESOURCES_FILE)));
        assertThat(actualIds, is(equalTo(expectedIds)));
    }

    private Stream<CristinObject> cristinObjects() throws IOException {
        GZIPInputStream inputStream = new GZIPInputStream(IoUtils.inputStreamFromResources(SAMPLE_INPUT_01));
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        return reader.lines()
                   .map(attempt(line -> JsonUtils.objectMapperWithEmpty.readValue(line, CristinObject.class)))
                   .map(Try::orElseThrow);
    }
}