package cucumber;

import no.unit.nva.cristin.CristinDataGenerator;
import no.unit.nva.cristin.mapper.CristinObject.CristinObjectBuilder;
import no.unit.nva.model.Publication;

public class ScenarioContext {

    private CristinObjectBuilder cristinEntry;
    private Publication nvaEntry;

    public ScenarioContext() {
    }

    public void newCristinEntry() {
        this.cristinEntry = new CristinDataGenerator().randomBookMonograph().copy();
    }

    public CristinObjectBuilder getCristinEntry() {
        return this.cristinEntry;
    }

    public void newNvaEntry() {

        this.nvaEntry = cristinEntry.build().toPublication();
    }

    public Publication getNvaEntry() {
        return this.nvaEntry;
    }
}
