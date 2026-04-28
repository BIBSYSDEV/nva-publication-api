package no.unit.nva.model.instancetypes.researchdata;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.net.URI;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.pages.NullPages;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public record SoftwareSourceCode(
    @JsonProperty(SoftwareSourceCode.SOFTWARE_VERSION_FIELD) String softwareVersion,
    @JsonProperty(SoftwareSourceCode.CODE_REPOSITORY_FIELD) URI codeRepository)
    implements PublicationInstance<NullPages> {

  public static final String SOFTWARE_VERSION_FIELD = "softwareVersion";
  public static final String CODE_REPOSITORY_FIELD = "codeRepository";

  @Override
  public NullPages getPages() {
    return NullPages.NULL_PAGES;
  }
}
