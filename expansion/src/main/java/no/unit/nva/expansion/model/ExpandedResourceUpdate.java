package no.unit.nva.expansion.model;

import nva.commons.core.JsonSerializable;
import nva.commons.core.JsonUtils;

public interface ExpandedResourceUpdate extends JsonSerializable {

    default String toJsonString() {
        try {
            return JsonUtils.dtoObjectMapper.writeValueAsString(this);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
