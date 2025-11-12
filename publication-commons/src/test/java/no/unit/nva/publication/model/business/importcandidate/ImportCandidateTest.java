package no.unit.nva.publication.model.business.importcandidate;

import static no.unit.nva.model.testing.ImportCandidateGenerator.randomImportCandidate;
import static no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator.randomAssociatedArtifacts;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.stream.Stream;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.importcandidate.ImportCandidate;
import no.unit.nva.importcandidate.ImportStatus;
import no.unit.nva.importcandidate.ImportStatusFactory;
import no.unit.nva.model.Username;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class ImportCandidateTest {

    public static Stream<Arguments> importStatuses() {
        return Stream.of(Arguments.of(ImportStatusFactory.createImported(randomString(), SortableIdentifier.next())),
                Arguments.of(ImportStatusFactory.createNotApplicable(randomPerson(), randomString())));
    }

    private static Username randomPerson() {
        return new Username(randomString());
    }

    @Test
    void shouldDoRoundTripWithoutLosingData() throws JsonProcessingException {
        var randomImportCandidate = randomImportCandidate();
        var json = randomImportCandidate.toString();
        var regeneratedImportCandidate = JsonUtils.dtoObjectMapper.readValue(json, ImportCandidate.class);
        assertThat(regeneratedImportCandidate, is(equalTo(regeneratedImportCandidate)));
    }

    @ParameterizedTest
    @DisplayName("should be possible to swap imported status to other status")
    @MethodSource("importStatuses")
    void shouldBePossibleToTransitionLegalImportStatus(ImportStatus importStatus) {
        var randomImportCandidate = randomImportCandidate();
        randomImportCandidate.setImportStatus(importStatus);
        assertThat(randomImportCandidate.getImportStatus(), is(equalTo(importStatus)));
    }

    @Test
    void shouldCopy() {
        var randomImportCandidate = randomImportCandidate();
        var copy = randomImportCandidate.copy().withAssociatedArtifacts(randomAssociatedArtifacts()).build();
        assertThat(randomImportCandidate, is(not(equalTo(copy))));
    }
}
