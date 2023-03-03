package no.unit.nva.doi.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;

public class DataCiteConfig {

    @JsonProperty("customerId")
    private final URI customerId;
    @JsonProperty("customerDoiPrefix")
    private final String customerDoiPrefix;
    @JsonProperty("dataCiteMdsClientUsername")
    private final String dataCiteMdsClientUsername;
    @JsonProperty("dataCiteMdsClientPassword")
    private final String dataCiteMdsClientPassword;

    @JsonCreator
    public DataCiteConfig(@JsonProperty("customerId") URI customerId,
                          @JsonProperty("customerDoiPrefix") String customerDoiPrefix,
                          @JsonProperty("dataCiteMdsClientUsername") String dataCiteMdsClientUsername,
                          @JsonProperty("dataCiteMdsClientPassword") String dataCiteMdsClientPassword) {
        this.customerId = customerId;
        this.customerDoiPrefix = customerDoiPrefix;
        this.dataCiteMdsClientUsername = dataCiteMdsClientUsername;
        this.dataCiteMdsClientPassword = dataCiteMdsClientPassword;
    }

    @JsonProperty("customerId")
    public URI getCustomerId() {
        return customerId;
    }

    @JsonProperty("customerDoiPrefix")
    public String getCustomerDoiPrefix() {
        return customerDoiPrefix;
    }

    @JsonProperty("dataCiteMdsClientUsername")
    public String getDataCiteMdsClientUsername() {
        return dataCiteMdsClientUsername;
    }

    @JsonProperty("dataCiteMdsClientPassword")
    public String getDataCiteMdsClientPassword() {
        return dataCiteMdsClientPassword;
    }
}
