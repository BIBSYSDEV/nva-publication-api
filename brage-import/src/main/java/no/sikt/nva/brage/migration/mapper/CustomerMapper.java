package no.sikt.nva.brage.migration.mapper;

import static java.util.Map.entry;
import java.net.URI;
import java.util.Map;

public final class CustomerMapper {

    //TODO: Find customer identifier for NVA and put it in the map;
    private static final Map<String, URI> CUSTOMER_MAP = Map.ofEntries(
        entry("TEST", URI.create("https://api.nva.unit.no/customer/test"))
    );

    private CustomerMapper() {

    }

    public static URI getCustomerUri(String customerShortName) {
        return CUSTOMER_MAP.getOrDefault(customerShortName, null);
    }
}
