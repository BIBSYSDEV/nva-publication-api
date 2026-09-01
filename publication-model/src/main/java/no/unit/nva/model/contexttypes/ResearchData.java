package no.unit.nva.model.contexttypes;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.net.URI;
import java.util.Collections;
import java.util.Set;
import no.unit.nva.model.Agent;

@JsonTypeInfo(use = Id.NAME, property = "type")
public record ResearchData(@JsonProperty(PUBLISHER_FIELD) Agent publisher)
    implements PublicationContext {

  public static final String PUBLISHER_FIELD = "publisher";

  @Override
  public Agent publisher() {
    return isEffectivelyNullPublisher() ? new NullPublisher() : publisher;
  }

  private boolean isEffectivelyNullPublisher() {
    return isNull(publisher) || publisher instanceof PublishingHouse pub && !pub.isValid();
  }

  @JsonIgnore
  @Override
  public Set<URI> extractPublicationContextUris() {
    if (nonNull(publisher)
        && publisher instanceof Publisher publisherWithId
        && nonNull(publisherWithId.getId())) {
      return Set.of(publisherWithId.getId());
    }
    return Collections.emptySet();
  }
}
