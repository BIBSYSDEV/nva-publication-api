package no.unit.nva.publication.model.storage;

import static no.unit.nva.publication.model.storage.PublishingRequestDao.BY_RESOURCE_INDEX_ORDER_PREFIX;
import static no.unit.nva.publication.storage.model.DatabaseConstants.KEY_FIELDS_DELIMITER;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.publication.model.business.FilesApprovalThesis;
import no.unit.nva.publication.model.business.TicketEntry;
import nva.commons.core.JacocoGenerated;

@JsonTypeName(FilesApprovalThesisDao.TYPE)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class FilesApprovalThesisDao extends TicketDao implements JsonSerializable {

  public static final String TYPE = "FilesApprovalThesis";

  @JacocoGenerated
  public FilesApprovalThesisDao() {
    super();
  }

  public FilesApprovalThesisDao(TicketEntry data) {
    super(data);
  }

  @Override
  public String joinByResourceOrderedType() {
    return joinByResourceContainedOrderedType();
  }

  @JsonIgnore
  private static String joinByResourceContainedOrderedType() {
    return BY_RESOURCE_INDEX_ORDER_PREFIX + KEY_FIELDS_DELIMITER + FilesApprovalThesis.TYPE;
  }
}
