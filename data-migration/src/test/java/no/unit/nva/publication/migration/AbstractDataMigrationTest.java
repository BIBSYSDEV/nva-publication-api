package no.unit.nva.publication.migration;

import static no.unit.nva.publication.migration.FakeS3Driver.allSamplePublications;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.service.ResourcesDynamoDbLocalTest;

public class AbstractDataMigrationTest extends ResourcesDynamoDbLocalTest {

    public static final Set<Publication> EXPECTED_IMPORTED_PUBLICATIONS = constructExpectedPublications();

    @Override
    public void init() {
        super.init();
    }

    protected static Set<Publication> constructExpectedPublications() {

        return withoutDuplicates(allSamplePublications().stream());
    }

    protected static Set<Publication> withoutDuplicates(Stream<Publication> publications) {
        return publications
                   .collect(Collectors.groupingBy(Publication::getIdentifier))
                   .values()
                   .stream()
                   .map(AbstractDataMigrationTest::chooseLatestPublication)
                   .collect(Collectors.toSet());
    }

    private static Publication chooseLatestPublication(List<Publication> list) {
        return list.stream().reduce(AbstractDataMigrationTest::latest).orElseThrow();
    }

    private static Publication latest(Publication left, Publication right) {
        if (left.getModifiedDate().isAfter(right.getModifiedDate())) {
            return left;
        }
        return right;
    }
}
