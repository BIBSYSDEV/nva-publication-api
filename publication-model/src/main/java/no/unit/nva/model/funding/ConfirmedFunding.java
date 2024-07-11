package no.unit.nva.model.funding;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

import nva.commons.core.JacocoGenerated;

import static java.util.Objects.isNull;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class ConfirmedFunding extends UnconfirmedFunding {

    private final URI id;

    @JsonCreator
    protected ConfirmedFunding(@JsonProperty("source") URI source,
                               @JsonProperty("id") URI id,
                               @JsonProperty("identifier") String identifier,
                               @JsonProperty("labels") Map<String, String> labels,
                               @JsonProperty("fundingAmount") MonetaryAmount fundingAmount,
                               @JsonProperty("activeFrom") Instant activeFrom,
                               @JsonProperty("activeTo") Instant activeTo) {
        super(source, identifier, labels, fundingAmount, activeFrom, activeTo);
        this.id = validate(id);
    }

    private URI validate(URI id) {
        if (isNull(id)) {
            throw new IllegalArgumentException("The funding object lacks an id");
        }
        return id;
    }


    public URI getId() {
        return id;
    }

    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ConfirmedFunding)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        ConfirmedFunding that = (ConfirmedFunding) o;
        return Objects.equals(getId(), that.getId());
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(super.hashCode(), getId());
    }
}
