package no.unit.nva.expansion;

import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import java.net.URI;
import java.util.Set;

public class FakeResourceExpansionService extends ResourceExpansionServiceImpl {

    public FakeResourceExpansionService() {
        super(null, null);
    }

    @Override
    public Set<URI> getOrganizationIds(String username) {
        return Set.of(randomUri());
    }
}
