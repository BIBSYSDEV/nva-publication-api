package no.unit.nva.expansion;

import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import java.net.URI;
import java.util.Set;
import no.unit.nva.publication.model.business.DataEntry;

public class FakeResourceExpansionService extends ResourceExpansionServiceImpl {
    
    public FakeResourceExpansionService() {
        super(null, null, null, null);
    }
    
    @Override
    public Set<URI> getOrganizationIds(DataEntry dataEntry) {
        return Set.of(randomUri());
    }
}
