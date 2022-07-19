package no.unit.nva.publication.indexing;

import static java.util.Map.entry;
import static no.unit.nva.expansion.ExpansionConfig.objectMapper;
import static nva.commons.core.ioutils.IoUtils.stringFromResources;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.nio.file.Path;
import java.util.Map;

public class PublicationChannelGenerator {
    
    public static final String SAMPLE_JSON_FILENAME = "framed-json/publication_channel_sample.json";
    public static final String FIELD_NAME = "name";
    public static final String FIELD_ID = "id";
    
    public static String getPublicationChannelSampleJournal(URI journalId, String journalName)
        throws JsonProcessingException {
        String publicationChannelSample = stringFromResources(Path.of(SAMPLE_JSON_FILENAME));
        JsonNode channelRoot = objectMapper.readTree(publicationChannelSample);
        
        ((ObjectNode) channelRoot).put(FIELD_ID, journalId.toString());
        ((ObjectNode) channelRoot).put(FIELD_NAME, journalName);
        return objectMapper.writeValueAsString(channelRoot);
    }
    
    public static String getPublicationChannelSamplePublisher(URI identifier, String publisherName)
        throws JsonProcessingException {
        Map<String, String> publisherMap = Map.ofEntries(
            entry("@context", "https://bibsysdev.github.io/src/publication-channel/channel-context.json"),
            entry("id", identifier.toString()),
            entry("name", publisherName));
        return objectMapper.writeValueAsString(publisherMap);
    }
}
