package no.unit.nva.publication.model.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.net.URI;
import java.util.Optional;
import no.unit.nva.auth.uriretriever.UriRetriever;
import nva.commons.core.Environment;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.Test;

class CustomerServiceTest {

    @Test
    void shouldReturnCustomerListWhenFetchingCustomer(){
        var uriRetriever = mock(UriRetriever.class);
        when(uriRetriever.getRawContent(listCustomersUri(), "application/json")).thenReturn(Optional.of(customerResponse()));
        var customerService = new CustomerService(uriRetriever);

        var customers = customerService.fetchCustomers();

        assertEquals(2, customers.customers().size());
    }

    @Test
    void shouldThrowCustomerServiceExceptionWhenFetchingCustomersFails(){
        var uriRetriever = mock(UriRetriever.class);
        var customerService = new CustomerService(uriRetriever);

        assertThrows(CustomerServiceException.class, customerService::fetchCustomers);
    }

    private String customerResponse() {
        return """
            {
              "type" : "CustomerList",
              "customers" : [ {
                "id" : "https://host.no/customer/4a0ccb49-97e2-4d78-948e-4ecc0c924689",
                "cristinId" : "https://host.no/cristin/organization/123456",
                "displayName" : "University",
                "createdDate" : "2025-04-25T07:51:43.231220404Z",
                "active" : true,
                "nviInstitution" : false
              }, {
                "id" : "https://host.no/customer/14d49ab7-4d1d-464d-b732-54b5c46ce6cc",
                "cristinId" : "https://host.no/cristin/organization/20754.0.0.0",
                "displayName" : "University",
                "createdDate" : "2025-03-28T13:03:29.536427513Z",
                "active" : true,
                "doiPrefix" : "00.000",
                "nviInstitution" : true
              } ],
              "@context" : "https://bibsysdev.github.io/src/customer-context.json",
              "id" : "https://host.no/customer"
            }
            """;
    }

    private static URI listCustomersUri() {
        return UriWrapper.fromHost(new Environment().readEnv("API_HOST")).addChild(
            "customer").getUri();
    }
}