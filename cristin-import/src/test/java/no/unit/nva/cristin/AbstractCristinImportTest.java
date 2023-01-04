package no.unit.nva.cristin;

import java.util.stream.IntStream;
import java.util.stream.Stream;
import no.unit.nva.cristin.mapper.CristinObject;
import no.unit.nva.publication.service.ResourcesLocalTest;

public class AbstractCristinImportTest extends ResourcesLocalTest {

    public Stream<CristinObject> cristinObjects(int numberOfObjects) {
        return IntStream.range(0, numberOfObjects)
                   .boxed()
                   .map(ignored -> CristinDataGenerator.randomObject());
    }
}
