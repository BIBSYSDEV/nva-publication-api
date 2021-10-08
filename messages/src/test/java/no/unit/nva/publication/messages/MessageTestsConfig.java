package no.unit.nva.publication.messages;

import com.fasterxml.jackson.databind.ObjectMapper;
import nva.commons.core.JsonUtils;

public final class MessageTestsConfig {

    public static  final ObjectMapper messageTestsObjectMapper = JsonUtils.dtoObjectMapper;

}
