package no.unit.nva.model.funding;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.net.URI;
import java.time.Instant;
import java.util.Map;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(name = "ConfirmedFunding", value = ConfirmedFunding.class),
    @JsonSubTypes.Type(name = "UnconfirmedFunding", value = UnconfirmedFunding.class)
})
public interface Funding {
    URI getSource();

    String getIdentifier();

    Map<String, String> getLabels();

    MonetaryAmount getFundingAmount();

    Instant getActiveFrom();

    Instant getActiveTo();
}
