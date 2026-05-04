package no.unit.nva.model.instancetypes.researchdata;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.pages.NullPages;
import nva.commons.apigateway.exceptions.ValidationError;
import nva.commons.core.StringUtils;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public record SoftwareSourceCode(
    @JsonProperty(SoftwareSourceCode.SOFTWARE_VERSION_FIELD) String softwareVersion,
    @JsonProperty(SoftwareSourceCode.CODE_REPOSITORY_FIELD) URI codeRepository)
    implements PublicationInstance<NullPages> {

  public static final String SOFTWARE_VERSION_FIELD = "softwareVersion";
  public static final String CODE_REPOSITORY_FIELD = "codeRepository";
  public static final String SOFTWARE_VERSION_REQUIRED_MESSAGE =
      "softwareVersion is required to publish SoftwareSourceCode";
  public static final String SOFTWARE_VERSION_POINTER =
      "#/entityDescription/reference/publicationInstance/softwareVersion";

  @Override
  public NullPages getPages() {
    return NullPages.NULL_PAGES;
  }

  @Override
  public List<ValidationError> validateForPublish() {
    if (StringUtils.isBlank(softwareVersion)) {
      return List.of(
          new ValidationError(SOFTWARE_VERSION_REQUIRED_MESSAGE, SOFTWARE_VERSION_POINTER));
    }
    return Collections.emptyList();
  }
}
