package no.unit.nva.model.instancetypes.researchdata;

import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.net.URI;
import java.util.Objects;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.pages.NullPages;
import nva.commons.core.JacocoGenerated;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class SoftwareSourceCode implements PublicationInstance<NullPages> {

  public static final String SOFTWARE_VERSION_FIELD = "softwareVersion";
  public static final String CODE_REPOSITORY_FIELD = "codeRepository";

  @JsonProperty(SOFTWARE_VERSION_FIELD)
  private final String softwareVersion;

  @JsonProperty(CODE_REPOSITORY_FIELD)
  private final URI codeRepository;

  public SoftwareSourceCode(
      @JsonProperty(SOFTWARE_VERSION_FIELD) String softwareVersion,
      @JsonProperty(CODE_REPOSITORY_FIELD) URI codeRepository) {
    this.softwareVersion = requireNonNull(softwareVersion, "softwareVersion is required");
    this.codeRepository = codeRepository;
  }

  public String getSoftwareVersion() {
    return softwareVersion;
  }

  public URI getCodeRepository() {
    return codeRepository;
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
    if (!(other instanceof SoftwareSourceCode software)) {
      return false;
    }
    return Objects.equals(getSoftwareVersion(), software.getSoftwareVersion())
        && Objects.equals(getCodeRepository(), software.getCodeRepository());
  }

  @JacocoGenerated
  @Override
  public int hashCode() {
    return Objects.hash(getSoftwareVersion(), getCodeRepository());
  }
}
