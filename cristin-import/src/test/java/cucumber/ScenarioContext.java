package cucumber;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import no.unit.nva.cristin.CristinDataGenerator;
import no.unit.nva.cristin.mapper.CristinObject;
import no.unit.nva.cristin.mapper.CristinTitle;
import no.unit.nva.model.Publication;

public class ScenarioContext {

    private CristinObject cristinEntry;
    private Publication nvaEntry;

    public ScenarioContext() {

    }

    public void newCristinEntry(Supplier<CristinObject> cristinObjectSupplier) {
        this.cristinEntry = cristinObjectSupplier.get();
    }

    public void newCristinEntry() {
        this.cristinEntry = new CristinDataGenerator().randomObject();
    }

    public CristinObject getCristinEntry() {
        return this.cristinEntry;
    }

    public void convertToNvaEntry() {
        this.nvaEntry = cristinEntry.toPublication();
    }

    public Publication getNvaEntry() {
        return this.nvaEntry;
    }

    public void addEmptyCristinTitle() {
        cristinEntry = cristinEntry.copy().withCristinTitles(new ArrayList<>()).build();
    }

    public void addCristinTitle() {
        cristinEntry.getCristinTitles().add(CristinTitle.builder().build());
    }

    public CristinTitle getLatestCristinTitle() {
        List<CristinTitle> titles = cristinEntry.getCristinTitles();
        return titles.get(titles.size() - 1);
    }
}
