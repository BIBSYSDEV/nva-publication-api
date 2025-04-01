package no.sikt.nva.brage.migration.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.Test;

class CustomerTest {

    @Test
    void shouldReturnOrganizationFromCustomerWhenHostDoesNotContainEnvironmentShortName() {
        var customer = Customer.fromBrageArchiveName("ffi");
        var host = "https://api.nva.unit.no";
        var organization = customer.toPublisher(host);

        var expected = UriWrapper.fromHost(host)
                           .addChild("customer")
                           .addChild("eb732391-19ec-4d9f-9cb3-a77c8876a18f")
                           .getUri();

        assertEquals(expected, organization.getId());
    }
}