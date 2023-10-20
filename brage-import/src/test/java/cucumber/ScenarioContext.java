package cucumber;

import static java.util.Objects.isNull;
import static nva.commons.core.attempt.Try.attempt;
import io.cucumber.java.bs.A;
import java.util.Set;
import no.sikt.nva.brage.migration.merger.CristinImportPublicationMerger;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.testing.PublicationGenerator;
import nva.commons.core.attempt.Try;

public class ScenarioContext {

    private Publication bragePublication;

    private Publication nvaPublication;

    private Publication mergedPublication;

    private Try<Publication> mergeAttempt;

    public ScenarioContext() {

    }

    public void newBragePublication(String cristinIdentifier) {
        var publication = PublicationGenerator.randomPublication();
        publication.setAdditionalIdentifiers(Set.of(new AdditionalIdentifier("cristin", cristinIdentifier)));
        this.bragePublication = publication;
    }

    public void newNvaPublication(String cristinIdentifier) {
        var publication = PublicationGenerator.randomPublication();
        publication.setAdditionalIdentifiers(Set.of(new AdditionalIdentifier("cristin", cristinIdentifier)));
        this.nvaPublication = publication;
    }

    public Publication getBragePublication() {
        return bragePublication;
    }

    public Publication getNvaPublication() {
        return nvaPublication;
    }

    public Publication getMergedPublication() {
        if (isNull(mergedPublication)) {
            this.mergedPublication = this.mergeAttempt.orElseThrow();
        }
        return mergedPublication;
    }

    public void mergePublications() {
        mergeAttempt = attempt(this::combinePublications);
    }

    private Publication combinePublications() {
        var merger = new CristinImportPublicationMerger(nvaPublication, bragePublication);
        return merger.mergePublications();
    }
}
