package no.sikt.nva.brage.migration.mapper;

import static java.util.Map.entry;
import java.util.Map;
import no.unit.nva.model.ResourceOwner;
import nva.commons.core.paths.UriWrapper;

public final class ResourceOwnerMapper {

    public static final String TEST = "https://api.test.nva.aws.unit.no/cristin/organization/test";
    public static final String NVE = "https://api.test.nva.aws.unit.no/cristin/organization/5948.0.0.0";
    private static final Map<String, ResourceOwner> RESOURCE_OWNER_MAP = Map.ofEntries(
        entry("TEST", new ResourceOwner("TestOwner", UriWrapper.fromUri(TEST).getUri())
        ));

    private ResourceOwnerMapper() {
    }

    public static ResourceOwner getResourceOwner(String customerShortName) {
        return RESOURCE_OWNER_MAP.getOrDefault(customerShortName, null);
    }
}
