package cucumber;

import static nva.commons.core.attempt.Try.attempt;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import no.unit.nva.cristin.CristinDataGenerator;
import no.unit.nva.cristin.mapper.CristinObject;
import no.unit.nva.cristin.mapper.CristinTitle;
import no.unit.nva.model.Publication;
import nva.commons.core.attempt.Try;

public class ScenarioContext {

    private CristinObject cristinEntry;
    private Publication nvaEntry;
    private Try<Publication> mappingAttempt;

    public ScenarioContext() {

    }

    public Try<Publication> getMappingAttempt() {
        return mappingAttempt;
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
        mappingAttempt = attempt(() -> cristinEntry.toPublication());
        this.nvaEntry = mappingAttempt.orElse(fail -> null);
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

    public boolean mappingIsSuccessful() {
        return this.mappingAttempt.isSuccess();
    }
}
