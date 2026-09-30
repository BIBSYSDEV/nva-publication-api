package no.unit.nva.publication.commons.customer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CustomerApiRightsRetention {
  private static final String FIELD_TYPE = "type";
  private static final String FIELD_POLICY_URI = "policyUri";

  @JsonProperty(FIELD_TYPE)
  private final String type;

  @JsonProperty(FIELD_POLICY_URI)
  private final String policyUri;

  @JsonCreator
  public CustomerApiRightsRetention(
      @JsonProperty(FIELD_TYPE) String type, @JsonProperty(FIELD_POLICY_URI) String policyUri) {
    this.type = type;
    this.policyUri = policyUri;
  }

  public String getType() {
    return type;
  }

  public String getPolicyUri() {
    return policyUri;
  }
}
