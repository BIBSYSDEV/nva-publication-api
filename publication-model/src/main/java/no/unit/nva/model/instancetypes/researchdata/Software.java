package no.unit.nva.model.instancetypes.researchdata;

import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.Objects;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.pages.NullPages;
import nva.commons.core.JacocoGenerated;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class Software implements PublicationInstance<NullPages> {

  public static final String SOFTWARE_VERSION_FIELD = "softwareVersion";

  @JsonProperty(SOFTWARE_VERSION_FIELD)
  private final String softwareVersion;

  public Software(@JsonProperty(SOFTWARE_VERSION_FIELD) String softwareVersion) {
    this.softwareVersion = requireNonNull(softwareVersion, "softwareVersion is required");
  }

  public String getSoftwareVersion() {
    return softwareVersion;
  }

  @Override
  public NullPages getPages() {
    return NullPages.NULL_PAGES;
  }

  @JacocoGenerated
  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof Software software)) {
      return false;
    }
    return Objects.equals(getSoftwareVersion(), software.getSoftwareVersion());
  }

  @JacocoGenerated
  @Override
  public int hashCode() {
    return Objects.hash(getSoftwareVersion());
  }
}
