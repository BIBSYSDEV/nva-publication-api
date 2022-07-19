package no.unit.nva.expansion;

import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import java.net.URI;
import java.util.Set;
import no.unit.nva.publication.model.business.Entity;

public class FakeResourceExpansionService extends ResourceExpansionServiceImpl {
    
    public FakeResourceExpansionService() {
        super(null, null, null, null);
    }
    
    @Override
    public Set<URI> getOrganizationIds(Entity dataEntry) {
        return Set.of(randomUri());
    }
}
