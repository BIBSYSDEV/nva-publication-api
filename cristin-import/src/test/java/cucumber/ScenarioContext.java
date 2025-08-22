package cucumber;

import static java.util.Objects.isNull;
import static nva.commons.core.attempt.Try.attempt;
import static org.mockito.Mockito.mock;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import no.unit.nva.auth.uriretriever.UriRetriever;
import no.unit.nva.cristin.CristinDataGenerator;
import no.unit.nva.cristin.mapper.CristinMapper;
import no.unit.nva.cristin.mapper.CristinObject;
import no.unit.nva.cristin.mapper.CristinTitle;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.model.utils.CustomerService;
import no.unit.nva.publication.service.FakeCristinUnitsUtil;
import no.unit.nva.publication.utils.CristinUnitsUtil;
import nva.commons.core.attempt.Try;
import software.amazon.awssdk.services.s3.S3Client;

public class ScenarioContext {

    private final CristinUnitsUtil cristinUnitsUtil;
    private CristinObject cristinEntry;
    private Try<Publication> mappingAttempt;
    private Publication nvaEntry;

    public ScenarioContext() {
        this.cristinUnitsUtil = new FakeCristinUnitsUtil();
    }

    public Try<Publication> getMappingAttempt() {
        return mappingAttempt;
    }

    public void newCristinEntry(Supplier<CristinObject> cristinObjectSupplier) {
        this.cristinEntry = cristinObjectSupplier.get();
    }

    public void newCristinEntry() {
        this.cristinEntry = CristinDataGenerator.randomObject();
    }

    public CristinObject getCristinEntry() {
        return this.cristinEntry;
    }

    public void convertToNvaEntry() {
        mappingAttempt =
            attempt(() -> new CristinMapper(cristinEntry, cristinUnitsUtil, mock(S3Client.class),
                                            mock(UriRetriever.class), mock(CustomerService.class))
                              .generatePublication());
    }

    public Publication getNvaEntry() {
        if (isNull(this.nvaEntry)) {
            this.nvaEntry = this.mappingAttempt.orElseThrow();
        }
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
