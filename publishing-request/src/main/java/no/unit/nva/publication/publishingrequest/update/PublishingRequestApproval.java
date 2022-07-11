package no.unit.nva.publication.publishingrequest.update;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

@JsonTypeInfo(use= Id.NAME, property = "type")
public class PublishingRequestApproval {

}
