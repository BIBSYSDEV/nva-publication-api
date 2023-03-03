package no.unit.nva.doi.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;

public class DataCiteConfig {

    @JsonProperty("customerId")
    private URI customerId;
    @JsonProperty("customerDoiPrefix")
    private String customerDoiPrefix;
    @JsonProperty("dataCiteMdsClientUrl")
    private String dataCiteMdsClientUrl;
    @JsonProperty("dataCiteMdsClientUsername")
    private String dataCiteMdsClientUsername;
    @JsonProperty("dataCiteMdsClientPassword")
    private String dataCiteMdsClientPassword;

    @JsonCreator
    public DataCiteConfig(@JsonProperty("customerId") URI customerId,
                          @JsonProperty("customerDoiPrefix") String customerDoiPrefix,
                          @JsonProperty("dataCiteMdsClientUrl") String dataCiteMdsClientUrl,
                          @JsonProperty("dataCiteMdsClientUsername") String dataCiteMdsClientUsername,
                          @JsonProperty("dataCiteMdsClientPassword") String dataCiteMdsClientPassword) {
        this.customerId = customerId;
        this.customerDoiPrefix = customerDoiPrefix;
        this.dataCiteMdsClientUrl = dataCiteMdsClientUrl;
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

    @JsonProperty("dataCiteMdsClientUrl")
    public String getDataCiteMdsClientUrl() {
        return dataCiteMdsClientUrl;
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
