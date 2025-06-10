package no.unit.nva.publication.model.utils;

import java.util.List;

public record CustomerList(List<CustomerSummary> customers) {

    public static CustomerList fromCustomerResponse(List<CustomerResponse> customers) {
        var list = customers.stream()
                       .map(customer -> new CustomerSummary(customer.id(), customer.cristinId()))
                       .toList();
        return new CustomerList(list);
    }
}
