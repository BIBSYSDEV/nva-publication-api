package cucumber;

import static java.util.Objects.isNull;
import static no.unit.nva.publication.testing.http.RandomPersonServiceResponse.randomUri;
import static nva.commons.core.attempt.Try.attempt;
import java.util.Set;
import no.sikt.nva.brage.migration.merger.CristinImportPublicationMerger;
import no.sikt.nva.brage.migration.model.PublicationRepresentation;
import no.sikt.nva.brage.migration.record.Record;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.exceptions.InvalidIsbnException;
import no.unit.nva.model.exceptions.InvalidIssnException;
import no.unit.nva.model.exceptions.InvalidUnconfirmedSeriesException;
import no.unit.nva.model.testing.PublicationGenerator;
import nva.commons.core.attempt.Try;

public class ScenarioContext {

    private PublicationRepresentation bragePublication;

    private Publication nvaPublication;

    private Publication mergedPublication;

    private Try<Publication> mergeAttempt;

    public ScenarioContext() {

    }

    public void newBragePublication(String cristinIdentifier) {
        var handle = randomUri();
        var publication = PublicationGenerator.randomPublication();
        publication.setAdditionalIdentifiers(Set.of(new AdditionalIdentifier("cristin", cristinIdentifier),
                                                    new AdditionalIdentifier("handle", handle.toString())));
        var record = new Record();
        record.setId(handle);

        this.bragePublication = new PublicationRepresentation(record, publication);
    }

    public void newNvaPublication(String cristinIdentifier) {
        var publication = PublicationGenerator.randomPublication();
        publication.setAdditionalIdentifiers(Set.of(new AdditionalIdentifier("cristin", cristinIdentifier)));
        this.nvaPublication = publication;
    }

    public PublicationRepresentation getBragePublication() {
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

    private Publication combinePublications()
        throws InvalidIsbnException, InvalidUnconfirmedSeriesException, InvalidIssnException {
        var merger = new CristinImportPublicationMerger(nvaPublication, bragePublication);
        return merger.mergePublications();
    }
}
