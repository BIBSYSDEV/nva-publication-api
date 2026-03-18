package no.sikt.nva.scopus.conversion.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.List;
import no.unit.nva.commons.json.JsonSerializable;

public class PublicationChannelResponse implements JsonSerializable {

  public static final String HITS_JSON_NAME = "hits";
  public static final String TOTAL_HITS_JSON_NAME = "totalHits";

  @JsonProperty(TOTAL_HITS_JSON_NAME)
  private final int totalHits;

  @JsonProperty(HITS_JSON_NAME)
  private final List<PublicationChannelHit> hits;

  @JsonCreator
  public PublicationChannelResponse(
      @JsonProperty(TOTAL_HITS_JSON_NAME) int totalHits,
      @JsonProperty(HITS_JSON_NAME) List<PublicationChannelHit> hits) {
    this.totalHits = totalHits;
    this.hits = hits;
  }

  public int getTotalHits() {
    return totalHits;
  }

  public List<PublicationChannelHit> getHits() {
    return hits;
  }

  @Override
  public String toString() {
    return this.toJsonString();
  }

  public static class PublicationChannelHit implements JsonSerializable {

    public static final String ID_JSON_NAME = "id";

    @JsonProperty(ID_JSON_NAME)
    private final URI id;

    @JsonCreator
    public PublicationChannelHit(@JsonProperty(ID_JSON_NAME) URI id) {
      this.id = id;
    }

    public URI getId() {
      return id;
    }
  }
}
